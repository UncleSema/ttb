package ru.unclesema.ttb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.unclesema.ttb.MarketSubscriber;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.model.NewUserRequest;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.model.UserMode;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.OrderBookStrategy;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.StrategyDecision;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class ApplicationService {
    private final PortfolioService portfolioService;
    private final InvestClient investClient;
    private final UserService userService;
    private final PriceService priceService;
    private final AnalyzeService analyzeService;
    private final List<Strategy> availableStrategies;
    private final ObjectMapper objectMapper;

    private static final long OPERATIONS_PERIOD_SECONDS = 30;
    private final Map<User, MarketSubscriber> orderBookSubscriberByUser = new HashMap<>();
    private final Map<User, MarketSubscriber> candlesSubscriberByUser = new HashMap<>();
    private final Map<User, MarketSubscriber> lastPricesSubscriberByUser = new HashMap<>();
    private final Map<User, Instant> lastBoughtByUser = new HashMap<>();

    public User addNewUser(NewUserRequest request) {
        Map<String, String> strategyParameters = request.getStrategyParameters();
        if (!strategyParameters.containsKey("name")) {
            throw new IllegalArgumentException("Пропущено имя стратегии");
        }
        String name = strategyParameters.get("name");
        strategyParameters.remove("name");
        Optional<Strategy> strategyOptional = availableStrategies.stream().filter(s -> s.getName().equals(name)).findAny();
        if (strategyOptional.isEmpty()) {
            throw new IllegalArgumentException("Стратегия `" + name + "` не найдена");
        }
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException("Токен пользователя не может быть пустым");
        }
        Class<? extends Strategy> strategyClazz = strategyOptional.get().getClass();
        Strategy strategy = objectMapper.convertValue(strategyParameters, strategyClazz);
        request.getFigis().removeIf(String::isBlank);
        if (request.getMode() == UserMode.MARKET) {
            if (request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("Для режима реальной торговли необходимо указать accountId");
            }
            if (userService.contains(request.getAccountId())) {
                throw new IllegalArgumentException("Пользователь с accountId = " + request.getAccountId() + " уже существует");
            }
        }
        String accountId = investClient.addUser(request.getToken(), request.getAccountId(), request.getMode());
        User user = new User(request.getToken(), request.getMode(), request.getMaxBalance(), accountId, request.getFigis(), strategy);
        userService.addUser(user);
        return user;
    }

    public void addOrderBook(OrderBook orderBook) {
        for (User user : userService.getActiveOrderBookUsers()) {
            OrderBookStrategy strategy = (OrderBookStrategy) user.strategy();
            String figi = orderBook.getFigi();
            StrategyDecision decision = strategy.addOrderBook(orderBook);
            BigDecimal lastPrice = priceService.getLastPrice(user, figi);
            if (canMakeOperationNow(user, lastPrice, figi)) {
                processStrategyDecision(user, figi, Instant.now(), lastPrice, decision);
            }
        }
    }

    public void addCandle(Candle candle) {
        for (User user : userService.getActiveCandleUsers()) {
            CandleStrategy strategy = (CandleStrategy) user.strategy();
            String figi = candle.getFigi();
            StrategyDecision decision = strategy.addCandle(candle);
            BigDecimal lastPrice = priceService.getLastPrice(user, figi);
            if (canMakeOperationNow(user, lastPrice, figi)) {
                processStrategyDecision(user, figi, Instant.now(), lastPrice, decision);
            }
        }
    }

    public void simulate(User user, Instant from, Instant to, CandleInterval interval) {
        log.info("Запрос просимулировать стратегию для {} с {} по {} с интервалом {}", user, from, to, interval);
        if (user.mode() != UserMode.ANALYZE) {
            throw new IllegalArgumentException("Используйте `ANALYZE` режим, чтобы симулировать работу стратегии");
        }
        if (!(user.strategy() instanceof CandleStrategy strategy)) {
            throw new UnsupportedOperationException("Чтобы просимулировать на исторических данных работу стратегии, она должна реализовывать CandleStrategy интерфейс");
        }
        var candles = investClient.getCandles(user, from, to, interval).join();
        for (var candle : candles) {
            var decision = strategy.addCandle(candle);
            var figi = candle.getFigi();
            var price = Utility.toBigDecimal(candle.getClose());
            var time = Utility.toInstant(candle.getTime());
            analyzeService.processNewCandle(user, candle);
            priceService.checkStopRequests(Utility.toLastPrice(candle));
            if (canMakeOperationNow(user, time, price, figi)) {
                processStrategyDecision(user, figi, time, Utility.toBigDecimal(candle.getClose()), decision);
            }
        }
        log.info("Симуляция для {} завершена", user);
        priceService.deleteRequestsForUser(user);
    }

    private void processStrategyDecision(User user, String figi, Instant now, BigDecimal currentPrice, StrategyDecision decision) {
        if (decision == StrategyDecision.NOTHING) return;
        boolean isBuy = decision == StrategyDecision.BUY;
        BigDecimal profit = BigDecimal.valueOf(1 + (isBuy ? 1 : -1) * user.strategy().getTakeProfit() / 100);
        BigDecimal loss = BigDecimal.valueOf(1 - (isBuy ? 1 : -1) * user.strategy().getStopLoss() / 100);
        BigDecimal takeProfit = currentPrice.multiply(profit);
        BigDecimal stopLoss = currentPrice.multiply(loss);
        Instrument instrument = investClient.getInstrument(user, figi);
        if (isBuy) {
            log.info("Стратегия собирается пойти в лонг по бумаге с figi {}.\nЦена покупки: {}.\nTakeProfit: {}.\nStopLoss: {}",
                    figi, currentPrice, takeProfit, stopLoss);
            var response = investClient.buyMarket(user, figi, currentPrice).join();
            if (response != null) {
                priceService.processNewOperation(user, figi, takeProfit, currentPrice, stopLoss, OrderDirection.ORDER_DIRECTION_BUY);
                lastBoughtByUser.put(user, now);
            }
        } else if (instrument.getShortEnabledFlag()) {
            log.info("Стратегия собирается пойти в шорт по бумаге с figi {}.\nЦена покупки: {}.\nTakeProfit: {}.\nStopLoss: {}",
                    figi, currentPrice, takeProfit, stopLoss);
            var response = investClient.sellMarket(user, figi, currentPrice).join();
            if (response != null) {
                priceService.processNewOperation(user, figi, takeProfit, currentPrice, stopLoss, OrderDirection.ORDER_DIRECTION_SELL);
                lastBoughtByUser.put(user, now);
            }
        }
    }

    public void enableStrategyForUser(String accountId) {
        log.info("Запрос на включение стратегии от пользователя с accountId = {}", accountId);
        Optional<User> optionalUser = userService.getAllUsers().stream().filter(u -> u.accountId().equals(accountId)).findAny();
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Не получилось найти пользователя с accountId = " + accountId);
        }
        User user = optionalUser.get();
        if (user.mode() == UserMode.ANALYZE) {
            throw new IllegalArgumentException("В режиме анализа нельзя подписаться на стакан, свечи и последние цены");
        }
        MarketSubscriber subscriber = new MarketSubscriber(this, user);
        if (user.strategy() instanceof OrderBookStrategy) {
            orderBookSubscriberByUser.computeIfAbsent(user, u -> {
                investClient.subscribeMarket(subscriber, InvestClient.InstrumentType.ORDER_BOOK);
                return subscriber;
            });
        }
        if (user.strategy() instanceof CandleStrategy) {
            candlesSubscriberByUser.computeIfAbsent(user, u -> {
                investClient.subscribeMarket(subscriber, InvestClient.InstrumentType.CANDLE);
                return subscriber;
            });
        }
        userService.makeUserActive(user);
        lastPricesSubscriberByUser.computeIfAbsent(user, u -> {
            investClient.subscribeMarket(subscriber, InvestClient.InstrumentType.LAST_PRICE);
            return subscriber;
        });
        investClient.subscribeTrades(user, response -> {
            if (response.hasOrderTrades()) {
                addOrderTrades(response.getOrderTrades());
            }
        });
    }

    public void disableStrategyForUser(String accountId) {
        log.info("Запрос на выключение стратегии от пользователя с accountId = {}", accountId);
        var user = userService.findUserByAccountId(accountId);
        if (orderBookSubscriberByUser.containsKey(user)) {
            investClient.unsubscribe(orderBookSubscriberByUser.get(user), InvestClient.InstrumentType.ORDER_BOOK);
            orderBookSubscriberByUser.remove(user);
        }
        if (candlesSubscriberByUser.containsKey(user)) {
            investClient.unsubscribe(candlesSubscriberByUser.get(user), InvestClient.InstrumentType.CANDLE);
            candlesSubscriberByUser.remove(user);
        }
        if (lastPricesSubscriberByUser.containsKey(user)) {
            investClient.unsubscribe(lastPricesSubscriberByUser.get(user), InvestClient.InstrumentType.LAST_PRICE);
            lastPricesSubscriberByUser.remove(user);
        }
        userService.makeUserInactive(user);
    }

    public void addLastPrice(LastPrice lastPrice) {
        priceService.addLastPrice(lastPrice);
    }

    private boolean canMakeOperationNow(User user, BigDecimal price, String figi) {
        return canMakeOperationNow(user, Instant.now(), price, figi);
    }

    private boolean canMakeOperationNow(User user, Instant now, BigDecimal price, String figi) {
        var alreadySpent = priceService.getBalance(user);
        var priceInRubles = priceService.getPriceInRubles(user, price, figi);
        var newBalance = alreadySpent.add(priceInRubles);
        if (newBalance.compareTo(user.maxBalance()) > 0) {
            log.warn("Недостаточно средств для операции, требуется {} RUB, а осталось {} RUB", priceInRubles, user.maxBalance().subtract(alreadySpent));
            return false;
        }
        return !lastBoughtByUser.containsKey(user) || lastBoughtByUser.get(user).plusSeconds(OPERATIONS_PERIOD_SECONDS).isBefore(now);
    }

    public void sellAll(String accountId) {
        log.info("Запрос на продажу всех активов {}", accountId);
        var user = userService.findUserByAccountId(accountId);
        priceService.deleteRequestsForUser(user);
        portfolioService.getRemaining(user).forEach((figi, quantity) -> {
            if (figi.equalsIgnoreCase("FG0000000000") && !investClient.getInstrument(user, figi).getInstrumentType().equalsIgnoreCase("currency")) {
                investClient.sellMarket(user, figi, quantity, priceService.getLastPrice(user, figi)).join();
            }
        });
    }

    public void addOrderTrades(OrderTrades orderTrades) {
        var user = userService.findUserByAccountId(orderTrades.getAccountId());
        portfolioService.addOrderTrades(user, orderTrades);
    }
}
