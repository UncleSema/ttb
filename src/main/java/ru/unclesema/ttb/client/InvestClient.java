package ru.unclesema.ttb.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.unclesema.ttb.MarketSubscriber;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.UserMode;
import ru.unclesema.ttb.model.CachedPortfolio;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class InvestClient {
    private static final String APP_NAME = "ru.unclesema.ttb";
    private final Map<User, String> streamIdByUser = new HashMap<>();
    private final Map<String, InvestApi> apiByToken = new HashMap<>();
    private final Map<User, BigDecimal> spent = new HashMap<>();

    public String addSandboxUser(String token) {
        var api = InvestApi.createSandbox(token, APP_NAME);
        apiByToken.put(token, api);
        return api.getSandboxService().openAccountSync();
    }

    public void addMarketUser(String token) {
        apiByToken.put(token, InvestApi.create(token, APP_NAME));
    }

    private InvestApi api(User user) {
        if (!apiByToken.containsKey(user.token())) {
            throw new IllegalStateException("Запрошено api для неизвестного пользователя " + user);
        }
        return apiByToken.get(user.token());
    }

    public boolean buy(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на покупку {} за {}.", figi, price);
            var response = api(user).getSandboxService().postOrderSync(
                    figi,
                    1,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_BUY,
                    user.accountId(),
                    OrderType.ORDER_TYPE_LIMIT,
                    UUID.randomUUID().toString()
            );
            log.info(String.valueOf(response));
        } else {

        }
        return true;
    }

    public CompletableFuture<PostOrderResponse> buyMarket(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на покупку {} по рыночной цене.", figi);
            var response = api(user).getSandboxService().postOrder(
                    figi,
                    1,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_BUY,
                    user.accountId(),
                    OrderType.ORDER_TYPE_MARKET,
                    UUID.randomUUID().toString()
            );
            return response.exceptionally(e -> {
                log.error("Ошибка при запросе на покупку", e);
                return null;
            }).thenApply(r -> {
                        if (r != null) {
                            spent.merge(user, price, BigDecimal::add);
                        }
                        return r;
                    }
            );
        } else {
            log.info("Запрос на покупку {} по рыночной цене.", figi);
            var response = api(user).getOrdersService().postOrder(
                    figi,
                    1,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_BUY,
                    user.accountId(),
                    OrderType.ORDER_TYPE_MARKET,
                    UUID.randomUUID().toString()
            );
            return response.exceptionally(e -> {
                log.error("Ошибка при запросе на покупку", e);
                return null;
            });
        }
    }

    public boolean sell(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на продажу {} за {}. {}", figi, price, Utility.toQuotation(price));
            var response = api(user).getSandboxService().postOrderSync(
                    figi,
                    1,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_SELL,
                    user.accountId(),
                    OrderType.ORDER_TYPE_LIMIT,
                    UUID.randomUUID().toString()
            );
        } else {

        }
        return true;
    }

    public CompletableFuture<PostOrderResponse> sellMarket(User user, String figi, long quantity, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на продажу {} по рыночной цене.", figi);
            var response = api(user).getSandboxService().postOrder(
                    figi,
                    quantity,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_SELL,
                    user.accountId(),
                    OrderType.ORDER_TYPE_MARKET,
                    UUID.randomUUID().toString()
            );
            return response.exceptionally(e -> {
                log.error("Ошибка при запросе на продажу", e);
                return null;
            });
        } else {
            log.info("Запрос на продажу {} по рыночной цене.", figi);
            var response = api(user).getOrdersService().postOrder(
                    figi,
                    quantity,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_SELL,
                    user.accountId(),
                    OrderType.ORDER_TYPE_MARKET,
                    UUID.randomUUID().toString()
            );
            return response.exceptionally(e -> {
                log.error("Ошибка при запросе на продажу", e);
                return null;
            });
        }
    }

    public CompletableFuture<PostOrderResponse> sellMarket(User user, String figi, BigDecimal price) {
        return sellMarket(user, figi, 1, price);
    }

    public void subscribeMarket(MarketSubscriber marketSubscriber, InstrumentType type) {
        log.info("Запрос к api о подписке на {} для {}", type, marketSubscriber.user());
        var user = marketSubscriber.user();
        var streamId = "";
        if (streamIdByUser.containsKey(user)) {
            streamId = streamIdByUser.get(user);
        } else {
            streamId = UUID.randomUUID().toString();
            streamIdByUser.put(user, streamId);
        }
        var subscriptionService = api(user)
                .getMarketDataStreamService()
                .newStream(streamId, marketSubscriber, e -> log.error("Произошла ошибка у " + user, e));
        if (type == InstrumentType.ORDER_BOOK) {
            subscriptionService.subscribeOrderbook(user.figis(), 20);
        } else if (type == InstrumentType.CANDLE) {
            subscriptionService.subscribeCandles(user.figis());
        } else {
            subscriptionService.subscribeLastPrices(user.figis());
        }
    }

    public void subscribeTrades(User user, StreamProcessor<TradesStreamResponse> processor) {
        if (user.mode() == UserMode.MARKET) {
            api(user)
                    .getOrdersStreamService()
                    .subscribeTrades(processor, e -> log.error("Произошла ошибка у " + user, e), List.of(user.accountId()));
        }
    }

    public void unsubscribe(MarketSubscriber subscriber, InstrumentType type) {
        log.info("Запрос к api об отписке на {} для {}", type, subscriber.user());
        User user = subscriber.user();
        if (!streamIdByUser.containsKey(user)) {
            log.error("Попытка отписаться от потока для пользователя, который не был подписан " + user);
            return;
        }
        var subscriptionService = api(user)
                .getMarketDataStreamService()
                .getStreamById(streamIdByUser.get(user));
        if (type == InstrumentType.ORDER_BOOK) {
            subscriptionService.unsubscribeOrderbook(user.figis(), 20);
        } else if (type == InstrumentType.CANDLE) {
            subscriptionService.unsubscribeCandles(user.figis());
        } else if (type == InstrumentType.LAST_PRICE) {
            subscriptionService.unsubscribeLastPrices(user.figis());
        }
    }

    @Cacheable(value = "portfolio", cacheManager = "cache5s")
    public CompletableFuture<CachedPortfolio> getPortfolio(User user) {
        log.info("Запрос к api о портфолио {}", user);
        if (user.mode() == UserMode.SANDBOX) {
            CompletableFuture<PortfolioResponse> response = api(user).getSandboxService().getPortfolio(user.accountId());
            return response.thenApply(CachedPortfolio::of);
        }
        if (user.mode() == UserMode.MARKET) {
            CompletableFuture<Portfolio> response = api(user).getOperationsService().getPortfolio(user.accountId());
            return response.thenApply(CachedPortfolio::of);
        }
        return null;
    }

    /**
     * <p>Асинхронно возвращает список операций, отсортированных по дате исполнения, для профиля <code>user</code>,
     * произведенных по времени в заданном интервале.</p>
     * <p> Метод кэшируется по полю <code>user</code>, кэш обновляется не чаще, чем раз в 10 секунд. </p>
     *
     * @param user профиль
     * @param from начало периода
     * @param to   конец периода
     * @return отсортированный список операций, произведенных в заданном интервале.
     */
    @Cacheable(value = "operations", cacheManager = "cache5s", key = "#user")
    public CompletableFuture<List<Operation>> getOperations(User user, Instant from, Instant to) {
        log.info("Запрос к api о последних операциях {}", user);
        CompletableFuture<List<Operation>> operations = CompletableFuture.completedFuture(new ArrayList<>());
        if (user.mode() == UserMode.SANDBOX) {
            // Так как в Sandbox режиме нельзя получить все операции разом, то их нужно `склеить`
            for (String figi : user.figis()) {
                var cur = api(user)
                        .getSandboxService()
                        .getOperations(user.accountId(), from, to, OperationState.OPERATION_STATE_EXECUTED, figi);
                operations = operations.thenCombine(cur, (ops, other) -> {
                    ops.addAll(other);
                    return ops;
                });
            }
        } else if (user.mode() == UserMode.MARKET) {
            operations = api(user).getOperationsService().getAllOperations(user.accountId(), from, to);
        }
        return operations.handleAsync((ops, e) -> {
            if (e != null) {
                log.error("Ошибка во время получения списка операций", e);
                return List.of();
            }
            return ops.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingLong(operation -> operation.getDate().getSeconds()))
                    .toList();
        });
    }

    @Cacheable(value = "instrument", cacheManager = "cache1d")
    public Instrument getInstrument(User user, String figi) {
        log.info("Запрос к api об инструменте {} для {}", figi, user);
        return api(user).getInstrumentsService().getInstrumentByFigiSync(figi);
    }

    @Cacheable(value = "lastPrice", cacheManager = "cache5s")
    public CompletableFuture<BigDecimal> loadLastPrice(User user, String figi) {
        log.info("Запрос к api о последней цене для {} для {}", figi, user);
        var response = api(user).getMarketDataService().getLastPrices(List.of(figi));
        return response.handleAsync((lastPrices, e) -> {
            if (e != null) {
                log.error("Ошибка при получении последней цены", e);
                return null;
            }
            return Utility.toBigDecimal(lastPrices.get(0).getPrice());
        });
    }

    @Cacheable(value = "currencies", cacheManager = "cache1d")
    public List<Currency> loadAllCurrencies(User user) {
        log.warn("Запрос к api о всех валютах для {}", user);
        return api(user).getInstrumentsService().getAllCurrenciesSync();
    }

    public CompletableFuture<List<Candle>> getCandles(User user, Instant from, Instant to) {
        return CompletableFuture.completedFuture(null);
    }

    @Cacheable(value = "balance", cacheManager = "cache5s")
    public BigDecimal getBalance(User user) {
        return spent.getOrDefault(user, BigDecimal.ZERO);
//        return getPortfolio(user).join().totalAmountCurrencies().getValue().negate();
    }

    public void addToBalance(User user, BigDecimal price) {
//        spent.merge(user, price, BigDecimal::add);
    }

    public void subtractFromBalance(User user, BigDecimal price) {
//        spent.merge(user, price, BigDecimal::subtract);
    }

    public enum InstrumentType {
        ORDER_BOOK,
        LAST_PRICE,
        CANDLE
    }
}


