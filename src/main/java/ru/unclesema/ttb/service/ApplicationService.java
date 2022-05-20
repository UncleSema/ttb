package ru.unclesema.ttb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.Currency;
import ru.unclesema.ttb.NewUserRequest;
import ru.unclesema.ttb.Subscriber;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.UserMode;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.OrderBookStrategy;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.StrategyDecision;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class ApplicationService {
    private final InvestClient investClient;
    private final UserService userService;
    private final PriceService priceService;
    private final List<Strategy> availableStrategies;

    private final Map<User, Subscriber> orderBookSubscriberByUser = new HashMap<>();
    private final Map<User, Subscriber> candlesSubscriberByUser = new HashMap<>();
    private final Map<User, Subscriber> lastPricesSubscriberByUser = new HashMap<>();
    private final Set<User> activeOrderBookUsers = new HashSet<>();
    private final Set<User> activeCandleUsers = new HashSet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private LocalDateTime lastBought = null;
    private static final long OPERATIONS_PERIOD_SECONDS = 30;

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
        String accountId = request.getAccountId();
        if (request.getMode() == UserMode.SANDBOX) {
            accountId = investClient.addSandboxUser(request.getToken());
        } else if (request.getMode() == UserMode.MARKET) {
            investClient.addMarketUser(request.getToken());
        }
        User user = new User(request.getToken(), request.getMode(), request.getMaxBalance(), accountId, request.getFigis(), strategy);
        userService.addUser(user);
        return user;
    }

    public void addOrderBook(OrderBook orderBook) {
        for (User user : activeOrderBookUsers) {
            OrderBookStrategy strategy = (OrderBookStrategy) user.strategy();
            String figi = orderBook.getFigi();
            StrategyDecision decision = strategy.addOrderBook(orderBook);
            processStrategyDecision(user, figi, priceService.getLastPrice(user, figi), decision);
        }
    }

    public void addCandle(Candle candle) {
        for (User user : activeCandleUsers) {
            CandleStrategy strategy = (CandleStrategy) user.strategy();
            String figi = candle.getFigi();
            StrategyDecision decision = strategy.addCandle(candle);
            processStrategyDecision(user, figi, priceService.getLastPrice(user, figi), decision);
        }
    }

    public void simulate(User user, Instant from, Instant to) {
        List<Candle> candles = investClient.getCandles(user, from, to).join();
        if (!(user.strategy() instanceof CandleStrategy strategy)) {
            throw new UnsupportedOperationException("Чтобы просимулировать на исторических данных работу стратегии, она должна реализовывать CandleStrategy интерфейс");
        }
        for (Candle candle : candles) {
            StrategyDecision decision = strategy.addCandle(candle);
            String figi = candle.getFigi();
            processStrategyDecision(user, figi, Utility.toBigDecimal(candle.getClose()), decision);
        }
    }

    private void processStrategyDecision(User user, String figi, BigDecimal currentPrice, StrategyDecision decision) {
        if (decision == StrategyDecision.NOTHING) return;
        boolean isBuy = decision == StrategyDecision.BUY;
        BigDecimal profit = BigDecimal.valueOf(1 + (isBuy ? 1 : -1) * user.strategy().getTakeProfit() / 100);
        BigDecimal loss = BigDecimal.valueOf(1 - (isBuy ? 1 : -1) * user.strategy().getStopLoss() / 100);
        BigDecimal takeProfit = currentPrice.multiply(profit);
        BigDecimal stopLoss = currentPrice.multiply(loss);
        LocalDateTime now = LocalDateTime.now();
        Instrument instrument = investClient.getInstrument(user, figi);
        if (isBuy && canMakeOperationNow(user, currentPrice, instrument.getCurrency())) {
            log.info("Стратегия собирается пойти в лонг по бумаге с figi {}.\nЦена покупки: {}.\nTakeProfit: {}.\nStopLoss: {}",
                    figi, currentPrice, takeProfit, stopLoss);
            investClient.buyMarket(user, figi, currentPrice).thenAcceptAsync(response -> {
                priceService.addStopRequest(new StopRequest(user, figi, takeProfit, stopLoss, OrderDirection.ORDER_DIRECTION_SELL));
                lastBought = now;
                investClient.addToBalance(user, getPriceInRubles(user, currentPrice, instrument.getCurrency()));
            });
        } else if (!isBuy && instrument.getShortEnabledFlag() && canMakeOperationNow(user, currentPrice, instrument.getCurrency())) {
            log.info("Стратегия собирается пойти в шорт по бумаге с figi {}.\nЦена покупки: {}.\nTakeProfit: {}.\nStopLoss: {}",
                    figi, currentPrice, takeProfit, stopLoss);
            investClient.sellMarket(user, figi, currentPrice).thenAcceptAsync(response -> {
                priceService.addStopRequest(new StopRequest(user, figi, takeProfit, stopLoss, OrderDirection.ORDER_DIRECTION_BUY));
                lastBought = now;
            });
        }
    }

    public void enableStrategyForUser(String accountId) {
        log.info("Запрос на включение стратегии от пользователя с accountId = {}", accountId);
        Optional<User> optionalUser = userService.getAllUsers().stream().filter(u -> u.accountId().equals(accountId)).findAny();
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Не получилось найти пользователя с accountId = " + accountId);
        }
        User user = optionalUser.get();
        Subscriber subscriber = new Subscriber(this, user);
        if (user.strategy() instanceof OrderBookStrategy) {
            orderBookSubscriberByUser.computeIfAbsent(user, u -> {
                activeOrderBookUsers.add(u);
                investClient.subscribe(subscriber, InvestClient.InstrumentType.ORDER_BOOK);
                return subscriber;
            });
        }
        if (user.strategy() instanceof CandleStrategy) {
            candlesSubscriberByUser.computeIfAbsent(user, u -> {
                activeCandleUsers.add(u);
                investClient.subscribe(subscriber, InvestClient.InstrumentType.CANDLE);
                return subscriber;
            });
        }
        lastPricesSubscriberByUser.computeIfAbsent(user, u -> {
            investClient.subscribe(subscriber, InvestClient.InstrumentType.LAST_PRICE);
            return subscriber;
        });
    }

    public void disableStrategyForUser(Integer userHash) {
        log.info("Запрос на выключение стратегии от пользователя с hash = {}", userHash);
        Optional<User> optionalUser = userService.findUserByHash(userHash);
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Не получается найти пользователя с hash=" + userHash);
        }
        User user = optionalUser.get();
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
        activeCandleUsers.remove(user);
        activeOrderBookUsers.remove(user);
    }

    public boolean isActive(User user) {
        return activeCandleUsers.contains(user) || activeOrderBookUsers.contains(user);
    }

    public void addLastPrice(LastPrice lastPrice) {
        priceService.addLastPrice(lastPrice);
    }

    private boolean canMakeOperationNow(User user, BigDecimal price, String currency) {
        var alreadySpent = investClient.getBalance(user);
        var priceInRubles = getPriceInRubles(user, price, currency);
        var newBalance = alreadySpent.add(priceInRubles);
        if (newBalance.compareTo(user.maxBalance()) > 0) {
            log.warn("Недостаточно средств для операции, требуется {} RUB, а осталось {} RUB", priceInRubles, user.maxBalance().subtract(alreadySpent));
            return false;
        }
        return (lastBought == null || lastBought.plusSeconds(OPERATIONS_PERIOD_SECONDS).isBefore(LocalDateTime.now()));
    }

    @CacheEvict("operations")
    public void sellAll(String accountId) {
        log.info("Запрос на продажу всех активов {}", accountId);
        var optionalUser = userService.getAllUsers().stream().filter(u -> u.accountId().equals(accountId)).findAny();
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Не получилось найти пользователя с accountId = " + accountId);
        }
        var user = optionalUser.get();
        priceService.deleteRequestsForUser(user);
        investClient.getPortfolio(user).join().positions().forEach(position -> {
                    if (!position.figi().equalsIgnoreCase("FG0000000000") && !investClient.getInstrument(user, position.figi()).getInstrumentType().equalsIgnoreCase("currency")) {
                        investClient.sellMarket(user, position.figi(), priceService.getLastPrice(user, position.figi())).join();
                    }
                }
        );
    }

    public BigDecimal getPriceInRubles(User user, BigDecimal price, String currency) {
        if (currency.equalsIgnoreCase("rub")) {
            return price;
        }
        Optional<Currency> optionalCurrency = investClient.loadAllCurrencies(user).stream().filter(c -> c.getIsoCurrencyName().equalsIgnoreCase(currency)).findAny();
        if (optionalCurrency.isEmpty()) {
            throw new IllegalArgumentException("Не получается найти валюту " + currency);
        }
        BigDecimal lastCurrencyPrice = priceService.getLastPrice(user, optionalCurrency.get().getFigi());
        return lastCurrencyPrice.multiply(price);
    }
}
