package ru.unclesema.ttb.client;

import com.jcabi.aspects.Cacheable;
import lombok.extern.slf4j.Slf4j;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.unclesema.ttb.OrderBookSubscriber;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.UserMode;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InvestClient {
    private static final String APP_NAME = "ru.unclesema.ttb";

    private final Map<User, InvestApi> apiByUser = new HashMap<>();
    private final Map<User, String> streamIdByUser = new HashMap<>();

    public boolean buy(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            PostOrderResponse response = apiByUser.get(user).getSandboxService().postOrderSync(
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

    public boolean sell(User user, String figi, BigDecimal price) {
        if (user.mode() == UserMode.SANDBOX) {
            PostOrderResponse response = apiByUser.get(user).getSandboxService().postOrderSync(
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

    public void subscribeOrderBook(User user, OrderBookSubscriber subscriber) {
        String streamId;
        if (streamIdByUser.containsKey(user)) {
            streamId = streamIdByUser.get(user);
        } else {
            streamId = UUID.randomUUID().toString();
            streamIdByUser.put(user, streamId);
        }
        apiByUser.get(user).getMarketDataStreamService().newStream(streamId, subscriber, e ->
                log.error("Произошла ошибка у " + user, e)
        ).subscribeOrderbook(user.figis());
    }

    public void subscribeLastPrices(User user, OrderBookSubscriber subscriber) {
        String streamId;
        if (streamIdByUser.containsKey(user)) {
            streamId = streamIdByUser.get(user);
        } else {
            streamId = UUID.randomUUID().toString();
            streamIdByUser.put(user, streamId);
        }
        apiByUser.get(user).getMarketDataStreamService().newStream(streamId, subscriber, e ->
                log.error("Произошла ошибка у " + user, e)
        ).subscribeLastPrices(user.figis());
    }

    public void unSubscribeOrderBook(User user) {
        if (!streamIdByUser.containsKey(user)) {
            log.error("Попытка отписаться от потока для пользователя, который не был подписан " + user);
            return;
        }
        apiByUser.get(user)
                .getMarketDataStreamService()
                .getStreamById(streamIdByUser.get(user)).unsubscribeOrderbook(user.figis());
    }

    public void addUser(User user) {
        if (user.mode() == UserMode.SANDBOX) {
            apiByUser.put(user, InvestApi.createSandbox(user.token(), APP_NAME));
        } else if (user.mode() == UserMode.MARKET) {
            apiByUser.put(user, InvestApi.create(user.token(), APP_NAME));
        }
    }

    public List<Account> getUserAccounts(User user) {
        if (!apiByUser.containsKey(user)) {
            log.error("Неизвестный пользователь {}", user);
            return List.of();
        }
        if (user.mode() == UserMode.SANDBOX) {
            return apiByUser.get(user).getSandboxService().getAccountsSync();
        }
        if (user.mode() == UserMode.MARKET) {
            return apiByUser.get(user).getUserService().getAccountsSync();
        }
        return null;
    }

    public List<Share> figis(User user) {
        return apiByUser.get(user).getInstrumentsService().getAllSharesSync();
    }

    public Portfolio getPortfolio(User user) {
        if (user.mode() == UserMode.SANDBOX) {
            PortfolioResponse response = apiByUser.get(user).getSandboxService().getPortfolioSync(user.accountId());
            return Portfolio.fromResponse(response);
        }
        if (user.mode() == UserMode.MARKET) {
            return apiByUser.get(user).getOperationsService().getPortfolioSync(user.accountId());
        }
        return null;
    }

    @Cacheable(lifetime = 2, unit = TimeUnit.DAYS)
    public String getNameByFigi(String figi) {
        return apiByUser.values().stream().toList().get(0).getInstrumentsService().getInstrumentByFigiSync(figi).getName();
    }
}
