package ru.unclesema.ttb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.unclesema.ttb.OrderBookSubscriber;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.StrategyDecision;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class ApplicationService {
    private final InvestClient investClient;
    private final UserService userService;
    private final PriceService priceService;
    private final Map<User, OrderBookSubscriber> subscriberByUser = new HashMap<>();

    public void addLastPrice(LastPrice lastPrice) {
        priceService.addLastPrice(lastPrice);
    }

    public void addNewUser(User user) {
        if (user == null || user.token() == null || user.token().isBlank()) {
            log.error("Токен пользователя не может быть пустым");
        } else if (subscriberByUser.containsKey(user)) {
            log.warn("Пользователь уже существует {}", user);
        } else {
            userService.addUser(user);
            investClient.addUser(user);
//            OrderBookSubscriber subscriber = new OrderBookSubscriber(this, user);
//            investClient.subscribeOrderBook(user, subscriber);
//            investClient.subscribeLastPrices(user, subscriber);
            log.info("Новый пользователь {} добавлен", user);
        }
    }

    public void removeUser(User user) {
        if (subscriberByUser.containsKey(user)) {
            userService.removeUser(user);
            investClient.unSubscribeOrderBook(user);
        } else {
            log.warn("Пользователя не существует {}", user);
        }
    }

    public void addNewOrderBook(OrderBookSubscriber subscriber, OrderBook orderBook) {
        log.info(String.valueOf(orderBook.getAsksList()));
        User user = subscriber.user();
        if (user.strategy() != null) {
            Strategy strategy = user.strategy();
            String figi = orderBook.getFigi();
            StrategyDecision decision = strategy.addOrderBook(orderBook);
            if (decision == StrategyDecision.BUY) {
                BigDecimal profit = BigDecimal.valueOf(strategy.getConfig().takeProfit() / 100 + 1);
                BigDecimal loss = BigDecimal.valueOf(1 - strategy.getConfig().stopLoss() / 100);
                BigDecimal currentPrice = priceService.getLastPrice(figi);
                BigDecimal takeProfit = currentPrice.multiply(profit);
                BigDecimal stopLoss = currentPrice.multiply(loss);
                log.info("Стратегия собирается пойти в лонг по бумаге с figi {}.\nЦена покупки: {}.\nTakeProfit: {}.\nStopLoss: {}",
                        figi, currentPrice, takeProfit, stopLoss);
                if (investClient.buy(user, figi, currentPrice)) {
                    priceService.addTakeProfit(new TakeProfitRequest(user, figi, takeProfit));
                    priceService.addStopLoss(new StopLossRequest(user, figi, stopLoss));
                }
            } else if (decision == StrategyDecision.SELL) {
                log.info("Стратегия собирается пойти в шорт по бумаге с figi {}.", figi);
//                BigDecimal loss = BigDecimal.valueOf(strategy.getConfig().getTakeProfit() / 100 + 1);
//                BigDecimal price = priceService.getLastPrice(figi).multiply(profit);
//                if (investClient.sell(userService.getUserPreferences(user), figi, priceService.getLastPrice(figi))) {
//
//                }
            }
        }
    }

    public List<Account> getAccounts(User user) {
        return investClient.getUserAccounts(user);
    }

    private void subscribe(User user) {
        if (subscriberByUser.containsKey(user)) {
            log.warn("{} уже подписан на стакан", user);
            return;
        }
        OrderBookSubscriber subscriber = new OrderBookSubscriber(this, user);
        subscriberByUser.put(user, subscriber);
        investClient.subscribeOrderBook(user, subscriber);
    }

    public List<Share> figis(User user) {
        return investClient.figis(user);
    }

}
