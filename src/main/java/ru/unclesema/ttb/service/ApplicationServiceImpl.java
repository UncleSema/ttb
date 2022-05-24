package ru.unclesema.ttb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;
import ru.unclesema.ttb.MarketSubscriber;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.model.NewUserRequest;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.model.UserMode;
import ru.unclesema.ttb.service.analyze.AnalyzeService;
import ru.unclesema.ttb.service.price.PriceService;
import ru.unclesema.ttb.service.user.UserService;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Основной сервис приложения, который отвечает за добавление новых пользователей, создание новых заявок, удаление существующих
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
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
    private final Map<User, Instant> lastBoughtByUser = new ConcurrentHashMap<>();

    /**
     * Метод обрабатывает запрос о новом пользователе, обрабатывая ошибки:
     * <ul>
     *     <li>Параметры стратегии, пришедшие с UI, должны содержать имя стратегии</li>
     *     <li>Стратегия с заданным именем должна существовать ровно одна</li>
     *     <li>Токен пользователя не должен быть пустым</li>
     *     <li>Если <code>mode == MARKET</code>, то accountId не должен быть пустым </li>
     *     <li>Пользователя с заданным accountId не должно существовать</li>
     *     <li>Ошибки обращений к API</li>
     * </ul>
     */
    @Override
    public User addNewUser(NewUserRequest request) {
        var strategyParameters = request.getStrategyParameters();
        if (!strategyParameters.containsKey("name")) {
            throw new IllegalArgumentException("Пропущено имя стратегии");
        }
        var name = strategyParameters.get("name");
        strategyParameters.remove("name");
        var strategyStream = availableStrategies.stream().filter(s -> s.getName().equals(name));
        if (strategyStream.count() > 1) {
            throw new IllegalStateException("Найдено несколько стратегий с именем `" + name + "`");
        }
        var strategyOptional = availableStrategies.stream().filter(s -> s.getName().equals(name)).findAny();
        if (strategyOptional.isEmpty()) {
            throw new IllegalArgumentException("Стратегия `" + name + "` не найдена");
        }
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException("Токен пользователя не может быть пустым");
        }
        var strategyClazz = strategyOptional.get().getClass();
        var strategy = objectMapper.convertValue(strategyParameters, strategyClazz);
        var figis = request.getFigis().stream()
                .distinct()
                .filter(figi -> !figi.isBlank())
                .toList();
        if (request.getMode() == UserMode.MARKET) {
            if (request.getAccountId() == null || request.getAccountId().isBlank()) {
                throw new IllegalArgumentException("Для режима реальной торговли необходимо указать accountId");
            }
            if (userService.contains(request.getAccountId())) {
                throw new IllegalArgumentException("Пользователь с accountId = " + request.getAccountId() + " уже существует");
            }
        }
        try {
            var accountId = investClient.addUser(request.getToken(), request.getAccountId(), request.getMode());
            var user = new User(request.getToken(), request.getMode(), request.getMaxBalance(), accountId, figis, strategy);
            userService.addUser(user);
            return user;
        } catch (ApiRuntimeException e) {
            if (Utility.checkExceptionCode(e, "70001")) {
                log.error("Внутренняя ошибка Invest Api. Вы правильно указали токен?");
            } else {
                log.error("Неизвестная ошибка при создании пользователя", e);
            }
            throw e;
        }
    }

    /**
     * Метод добавляет информацию о новом стакане, о чём оповещает все стратегии, использующие стаканы.
     */
    @Override
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

    /**
     * Метод добавляет информацию о новой свече, о чём оповещает все стратегии, использующие свечи.
     */
    @Override
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

    /**
     * Метод симулирует работу стратегию на заданном временном промежутке.
     */
    @Override
    public void simulate(User user, Instant from, Instant to, CandleInterval interval) {
        log.info("Запрос просимулировать стратегию для {} с {} по {} с интервалом {}", user, from, to, interval);
        if (user.mode() != UserMode.ANALYZE) {
            throw new IllegalArgumentException("Используйте режим ANALYZE, чтобы симулировать работу стратегии");
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

    /**
     * Метод обрабатывает решение стратегии, в зависимости от которого покупает / продает выбранную бумагу; выставляет takeProfit, stopLoss.
     */
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

    /**
     * Подписывает пользователя на:
     * <ul>
     *     <li>Последние цены на выбранные инструменты</li>
     *     <li>Последний стакан, если стратегия использует стакан</li>
     *     <li>Последние свечи, если стратегия использует свечи</li>
     * </ul>
     */
    @Override
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
        MarketSubscriber subscriber = new MarketSubscriber(this);
        if (user.strategy() instanceof OrderBookStrategy) {
            orderBookSubscriberByUser.computeIfAbsent(user, u -> {
                investClient.subscribe(user, subscriber, InvestClient.InstrumentType.ORDER_BOOK);
                return subscriber;
            });
        }
        if (user.strategy() instanceof CandleStrategy) {
            candlesSubscriberByUser.computeIfAbsent(user, u -> {
                investClient.subscribe(user, subscriber, InvestClient.InstrumentType.CANDLE);
                return subscriber;
            });
        }
        userService.makeUserActive(user);
        lastPricesSubscriberByUser.computeIfAbsent(user, u -> {
            investClient.subscribe(user, subscriber, InvestClient.InstrumentType.LAST_PRICE);
            return subscriber;
        });
    }

    /**
     * Метод отменяет все подписки пользователя, делает его `неактивным`.
     */
    @Override
    public void disableStrategyForUser(String accountId) {
        log.info("Запрос на выключение стратегии от пользователя с accountId = {}", accountId);
        var user = userService.findUserByAccountId(accountId);
        if (orderBookSubscriberByUser.containsKey(user)) {
            investClient.unsubscribe(user, InvestClient.InstrumentType.ORDER_BOOK);
            orderBookSubscriberByUser.remove(user);
        }
        if (candlesSubscriberByUser.containsKey(user)) {
            investClient.unsubscribe(user, InvestClient.InstrumentType.CANDLE);
            candlesSubscriberByUser.remove(user);
        }
        if (lastPricesSubscriberByUser.containsKey(user)) {
            investClient.unsubscribe(user, InvestClient.InstrumentType.LAST_PRICE);
            lastPricesSubscriberByUser.remove(user);
        }
        userService.makeUserInactive(user);
    }

    /**
     * Добавление последней цены
     */
    @Override
    public void addLastPrice(LastPrice lastPrice) {
        priceService.addLastPrice(lastPrice);
    }

    /**
     * Проверка на то, что стратегия заданного пользователя может провести операцию по заданной бумаге.
     *
     * <p>
     * На данный момент есть два критерия возможности проведения операции для пользователя:
     *     <ul>
     *         <li>Время, т.е. пользователь может совершать операции не чаще чем раз в <code>OPERATIONS_PERIOD_SECONDS</code></li>
     *         <li>Бюджет пользователя: стратегия не может превысить выставленный пользователем лимит</li>
     *    </ul>
     * </p>
     */
    private boolean canMakeOperationNow(User user, Instant now, BigDecimal price, String figi) {
        var alreadySpent = priceService.getBalance(user);
        var instrument = investClient.getInstrument(user, figi);
        var priceInRubles = priceService.getPriceInRubles(user, price, figi).multiply(BigDecimal.valueOf(instrument.getLot()));
        var newBalance = alreadySpent.add(priceInRubles);
        if (newBalance.compareTo(user.maxBalance()) > 0) {
            log.debug("Недостаточно средств для операции, требуется {} RUB, а осталось {} RUB", priceInRubles, user.maxBalance().subtract(alreadySpent));
            return false;
        }
        log.debug("Нельзя провести операцию сейчас: операцию можно провести только раз в {} секунд", OPERATIONS_PERIOD_SECONDS);
        return !lastBoughtByUser.containsKey(user) || lastBoughtByUser.get(user).plusSeconds(OPERATIONS_PERIOD_SECONDS).isBefore(now);
    }

    /**
     * Проверка на то, что стратегия заданного пользователя может провести операцию по заданной бумаге.
     *
     * <p>
     * На данный момент есть два критерия возможности проведения операции для пользователя:
     *     <ul>
     *         <li>Время, т.е. пользователь может совершать операции не чаще чем раз в <code>OPERATIONS_PERIOD_SECONDS</code></li>
     *         <li>Бюджет пользователя: стратегия не может превысить выставленный пользователем лимит</li>
     *    </ul>
     * </p>
     */
    private boolean canMakeOperationNow(User user, BigDecimal price, String figi) {
        return canMakeOperationNow(user, Instant.now(), price, figi);
    }

    /**
     * Продажа всех ценных бумаг пользователя, которые были накоплены стратегией.
     */
    @Override
    public void sellAll(String accountId) {
        log.info("Запрос на продажу всех активов {}", accountId);
        var user = userService.findUserByAccountId(accountId);
        priceService.deleteRequestsForUser(user);
        priceService.getRemainingInstruments(user).forEach((figi, quantity) -> {
            var instrument = investClient.getInstrument(user, figi);
            if (!instrument.getInstrumentType().equalsIgnoreCase("currency")) {
                investClient.sellMarket(user, figi, quantity, priceService.getLastPrice(user, figi)).join();
            }
        });
    }
}
