package ru.unclesema.ttb.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;
import ru.unclesema.ttb.Subscriber;
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
    private static final Instant appStartTime = Instant.now();
    private final Map<User, String> streamIdByUser = new HashMap<>();
    private final Map<String, InvestApi> apiByToken = new HashMap<>();
    private final Map<User, BigDecimal> spent = new HashMap<>();


    public String addSandboxUser(String token) {
        InvestApi api = InvestApi.createSandbox(token, APP_NAME);
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
            PostOrderResponse response = api(user).getSandboxService().postOrderSync(
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
            CompletableFuture<PostOrderResponse> response = api(user).getSandboxService().postOrder(
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
        } else {

        }
        return null;
    }

    public boolean sell(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на продажу {} за {}. {}", figi, price, Utility.toQuotation(price));
            PostOrderResponse response = api(user).getSandboxService().postOrderSync(
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

    public CompletableFuture<PostOrderResponse> sellMarket(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на продажу {} по рыночной цене.", figi);
            CompletableFuture<PostOrderResponse> response = api(user).getSandboxService().postOrder(
                    figi,
                    1,
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

        }
        return null;
    }

    public void subscribe(Subscriber subscriber, InstrumentType type) {
        log.info("Запрос к api о подписке на {} для {}", type, subscriber.user());
        User user = subscriber.user();
        String streamId;
        if (streamIdByUser.containsKey(user)) {
            streamId = streamIdByUser.get(user);
        } else {
            streamId = UUID.randomUUID().toString();
            streamIdByUser.put(user, streamId);
        }
        MarketDataSubscriptionService subscriptionService = api(user)
                .getMarketDataStreamService()
                .newStream(streamId, subscriber, e -> log.error("Произошла ошибка у " + user, e));
        if (type == InstrumentType.ORDER_BOOK) {
            subscriptionService.subscribeOrderbook(user.figis(), 20);
        } else if (type == InstrumentType.CANDLE) {
            subscriptionService.subscribeCandles(user.figis());
        } else {
            subscriptionService.subscribeLastPrices(user.figis());
        }
    }

    public void unsubscribe(Subscriber subscriber, InstrumentType type) {
        log.info("Запрос к api об отписке на {} для {}", type, subscriber.user());
        User user = subscriber.user();
        if (!streamIdByUser.containsKey(user)) {
            log.error("Попытка отписаться от потока для пользователя, который не был подписан " + user);
            return;
        }
        MarketDataSubscriptionService subscriptionService = api(user)
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
    @Cacheable(value = "operations", cacheManager = "cache10s", key = "#user")
    public CompletableFuture<List<Operation>> getOperations(User user, Instant from, Instant to) {
        log.info("Запрос к api о последних операциях {}", user);
        CompletableFuture<List<Operation>> operations = CompletableFuture.completedFuture(new ArrayList<>());
        if (user.mode() == UserMode.SANDBOX) {
            // Так как в Sandbox режиме нельзя получить все операции разом, то их нужно `склеить`
            for (String figi : user.figis()) {
                CompletableFuture<List<Operation>> cur = api(user)
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
            ops.sort(Comparator.comparingLong(operation -> operation.getDate().getSeconds()));
            return ops;
        });
    }

    @Cacheable(value = "instrument", cacheManager = "cache1d")
    public Instrument getInstrument(User user, String figi) {
        log.info("Запрос к api об инструменте {} для {}", figi, user);
        return api(user).getInstrumentsService().getInstrumentByFigiSync(figi);
    }

    @Cacheable(value = "5s")
    public CompletableFuture<BigDecimal> loadLastPrice(User user, String figi) {
        log.info("Запрос к api о последней цене для {} для {}", figi, user);
        CompletableFuture<List<LastPrice>> response = api(user).getMarketDataService().getLastPrices(List.of(figi));
        return response.handleAsync((lastPrices, e) -> {
            if (e != null) {
                log.error("Ошибка при получении последней цены", e);
                return null;
            }
            return Utility.toBigDecimal(lastPrices.get(0).getPrice());
        });
    }

    @Cacheable(value = "forever")
    public List<Currency> loadAllCurrencies(User user) {
        return api(user).getInstrumentsService().getAllCurrenciesSync();
    }

    public CompletableFuture<List<Candle>> getCandles(User user, Instant from, Instant to) {
        return CompletableFuture.completedFuture(null);
    }

    public BigDecimal getBalance(User user) {
        return getPortfolio(user).join().totalAmountCurrencies().getValue().negate();
    }

    public void addToBalance(User user, BigDecimal price, String currency) {
        spent.merge(user, getPriceInRubles(user, price, currency), BigDecimal::add);
    }

    public void subtractToBalance(User user, BigDecimal price, String currency) {
        spent.merge(user, getPriceInRubles(user, price, currency), BigDecimal::subtract);
    }

    public BigDecimal getPriceInRubles(User user, BigDecimal price, String currency) {
        if (currency.equalsIgnoreCase("rub")) {
            return price;
        }
        Optional<Currency> optionalCurrency = loadAllCurrencies(user).stream().filter(c -> c.getIsoCurrencyName().equalsIgnoreCase(currency)).findAny();
        if (optionalCurrency.isEmpty()) {
            throw new IllegalArgumentException("Не получается найти валюту " + currency);
        }
        BigDecimal lastCurrencyPrice = loadLastPrice(user, optionalCurrency.get().getFigi()).join();
        return lastCurrencyPrice.multiply(price);
    }

    public Map<Instrument, Long> getRemainingInstruments(User user) {
        List<Operation> operations = getOperations(user, appStartTime, Instant.now()).join();
        Map<Instrument, Long> remainingInstruments = new HashMap<>();
        for (Operation op : operations) {
            if (op.getInstrumentType().equalsIgnoreCase("currency")) continue;
            Instrument instrument = getInstrument(user, op.getFigi());
            if (op.getOperationType() == OperationType.OPERATION_TYPE_BUY) {
                remainingInstruments.merge(instrument, op.getQuantity(), Long::sum);
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_SELL) {
                Long cur = remainingInstruments.getOrDefault(instrument, 0L);
                if (cur == op.getQuantity()) {
                    remainingInstruments.remove(instrument);
                } else {
                    remainingInstruments.put(instrument, cur - op.getQuantity());
                }
            } else {
                log.error("Неизвестный тип операции: {}", op);
            }
        }
        return remainingInstruments;
    }

    public enum InstrumentType {
        ORDER_BOOK,
        LAST_PRICE,
        CANDLE
    }
}


