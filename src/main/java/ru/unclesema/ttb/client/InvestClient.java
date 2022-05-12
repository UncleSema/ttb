package ru.unclesema.ttb.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import ru.tinkoff.piapi.contract.v1.*;
import ru.unclesema.ttb.Subscriber;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.UserMode;
import ru.unclesema.ttb.model.CachedPortfolio;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class InvestClient {
    private final Map<User, String> streamIdByUser = new HashMap<>();
    private static final List<String> currencies = List.of("BBG0013HGFT4");

    public boolean buy(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на покупку {} за {}.", figi, price);
            PostOrderResponse response = user.api().getSandboxService().postOrderSync(
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

    public boolean buyMarket(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на покупку {} по рыночной цене.", figi);
            PostOrderResponse response = user.api().getSandboxService().postOrderSync(
                    figi,
                    1,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_BUY,
                    user.accountId(),
                    OrderType.ORDER_TYPE_MARKET,
                    UUID.randomUUID().toString()
            );
            log.info(String.valueOf(response));
        } else {

        }
        return true;
    }

    public boolean sell(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на продажу {} за {}. {}", figi, price, Utility.toQuotation(price));
            PostOrderResponse response = user.api().getSandboxService().postOrderSync(
                    figi,
                    1,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_SELL,
                    user.accountId(),
                    OrderType.ORDER_TYPE_LIMIT,
                    UUID.randomUUID().toString()
            );
            log.info(String.valueOf(response));
        } else {

        }
        return true;
    }

    public boolean sellMarket(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            log.info("Запрос на продажу {} по рыночной цене.", figi);
            PostOrderResponse response = user.api().getSandboxService().postOrderSync(
                    figi,
                    1,
                    Utility.toQuotation(price),
                    OrderDirection.ORDER_DIRECTION_SELL,
                    user.accountId(),
                    OrderType.ORDER_TYPE_MARKET,
                    UUID.randomUUID().toString()
            );
            log.info(String.valueOf(response));
        } else {

        }
        return true;
    }

    public void subscribeOrderBook(User user, Subscriber subscriber) {
        String streamId;
        if (streamIdByUser.containsKey(user)) {
            streamId = streamIdByUser.get(user);
        } else {
            streamId = UUID.randomUUID().toString();
            streamIdByUser.put(user, streamId);
        }
        user.api().getMarketDataStreamService().newStream(streamId, subscriber, e ->
                log.error("Произошла ошибка у " + user, e)
        ).subscribeOrderbook(user.figis(), 20);
    }

    public void subscribeLastPrices(User user, Subscriber subscriber) {
        String streamId;
        if (streamIdByUser.containsKey(user)) {
            streamId = streamIdByUser.get(user);
        } else {
            streamId = UUID.randomUUID().toString();
            streamIdByUser.put(user, streamId);
        }
        List<String> figis = new ArrayList<>(user.figis());
        figis.addAll(currencies);
        user.api().getMarketDataStreamService().newStream(streamId, subscriber, e ->
                log.error("Произошла ошибка у " + user, e)
        ).subscribeLastPrices(figis);
    }

    public void subscribeCandles(User user, Subscriber subscriber) {
        String streamId;
        if (streamIdByUser.containsKey(user)) {
            streamId = streamIdByUser.get(user);
        } else {
            streamId = UUID.randomUUID().toString();
            streamIdByUser.put(user, streamId);
        }
        user.api().getMarketDataStreamService().newStream(streamId, subscriber, e ->
                log.error("Произошла ошибка у " + user, e)
        ).subscribeCandles(user.figis());
    }

    public void unSubscribeOrderBook(Subscriber subscriber) {
        User user = subscriber.user();
        if (!streamIdByUser.containsKey(user)) {
            log.error("Попытка отписаться от потока для пользователя, который не был подписан " + user);
            return;
        }
        user.api()
                .getMarketDataStreamService()
                .getStreamById(streamIdByUser.get(user)).unsubscribeOrderbook(user.figis(), 20);
    }

    public void unSubscribeCandles(Subscriber subscriber) {
        User user = subscriber.user();
        if (!streamIdByUser.containsKey(user)) {
            log.error("Попытка отписаться от потока для пользователя, который не был подписан " + user);
            return;
        }
        user.api()
                .getMarketDataStreamService()
                .getStreamById(streamIdByUser.get(user)).unsubscribeCandles(user.figis());
    }

    public void unSubscribeLastPrices(Subscriber subscriber) {
        User user = subscriber.user();
        if (!streamIdByUser.containsKey(user)) {
            log.error("Попытка отписаться от потока для пользователя, который не был подписан " + user);
            return;
        }
        user.api()
                .getMarketDataStreamService()
                .getStreamById(streamIdByUser.get(user)).unsubscribeLastPrices(user.figis());
    }

    @Cacheable(value = "portfolio", cacheManager = "cache5s")
    public CachedPortfolio getPortfolio(User user) {
        if (user.mode() == UserMode.SANDBOX) {
            PortfolioResponse response = user.api().getSandboxService().getPortfolioSync(user.accountId());
            return CachedPortfolio.of(response);
        }
        if (user.mode() == UserMode.MARKET) {
            return CachedPortfolio.of(user.api().getOperationsService().getPortfolioSync(user.accountId()));
        }
        return null;
    }

    @Cacheable(value = "operations", cacheManager = "cache15s", key = "#user")
    public List<Operation> getOperations(User user, Instant from, Instant to) {
        List<Operation> operations = new ArrayList<>();
        if (user.mode() == UserMode.SANDBOX) {
            for (String figi : user.figis()) {
                List<Operation> cur = user.api().getSandboxService().getOperationsSync(user.accountId(), from, to, OperationState.OPERATION_STATE_EXECUTED, figi);
                operations.addAll(cur);
            }
        } else if (user.mode() == UserMode.MARKET) {
            operations = user.api().getOperationsService().getAllOperationsSync(user.accountId(), from, to);
        }
        operations.sort(Comparator.comparingLong(o -> o.getDate().getSeconds()));
        return operations;
    }

    @Cacheable(value = "instrument", cacheManager = "cacheForever")
    public Instrument getInstrument(User user, String figi) {
        return user.api().getInstrumentsService().getInstrumentByFigiSync(figi);
    }
}
