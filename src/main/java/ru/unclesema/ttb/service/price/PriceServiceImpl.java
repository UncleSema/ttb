package ru.unclesema.ttb.service.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OperationType;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.model.UserMode;
import ru.unclesema.ttb.service.analyze.AnalyzeService;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Сервис, отвечающий за работу с балансом / последними ценами / stop запросами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceServiceImpl implements PriceService {
    private final InvestClient client;
    private final AnalyzeService analyzeService;

    private final Map<String, BigDecimal> lastPriceByFigi = new ConcurrentHashMap<>();
    private final Map<String, Queue<StopRequest>> openStopRequests = new ConcurrentHashMap<>();
    private final Map<User, BigDecimal> spentByUser = new ConcurrentHashMap<>();

    /**
     * Метод ищет последнюю цену среди добавленных (если не находит, отправляет запрос к api).
     */
    @Override
    public BigDecimal getLastPrice(User user, String figi) {
        if (figi.equalsIgnoreCase("FG0000000000")) {
            return BigDecimal.ONE;
        }
        if (user.mode() == UserMode.ANALYZE) {
            LastPrice lastPrice = analyzeService.getLastPrice(user, figi);
            return Utility.toBigDecimal(lastPrice.getPrice());
        }
        if (lastPriceByFigi.containsKey(figi)) {
            return lastPriceByFigi.get(figi);
        }
        return client.loadLastPrice(user, figi).join();
    }

    /**
     * Метод находит все инструменты, которые ещё не были проданы
     *
     * <p>Метод смотрит на последние операции пользователя, храня <code>Map</code> и прибавляя 1 к инструменту, в случае покупки, и вычитая 1 иначе.</p>
     * <p>
     * Другие возможные реализации:
     * <ul>
     *     <li> Portfolio, метод будет делать запрос к api, кэшируя его. Такая реализация не очень хороша из-за `несинхронизованности` с операциями,
     *     которые используются в UI. </li>
     *     <li> LastTrades, сервис подпишется на LastTrades, по которым будет считать оставшиеся инструменты. Такая реализация не очень хороша,
     *     из-за того, что подписаться на LastTrades можно только при торговле на бирже</li>
     * </ul>
     * </p>
     */
    @Override
    public Map<String, Long> getRemainingInstruments(User user) {
        List<Operation> operations = client.getOperations(user);
        Map<String, Long> remainingInstruments = new HashMap<>();
        for (Operation op : operations) {
            if (op.getInstrumentType().equalsIgnoreCase("currency")) continue;
            if (op.getOperationType() == OperationType.OPERATION_TYPE_BUY) {
                remainingInstruments.merge(op.getFigi(), op.getQuantity(), Long::sum);
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_SELL) {
                Long cur = remainingInstruments.getOrDefault(op.getFigi(), 0L);
                if (cur == op.getQuantity()) {
                    remainingInstruments.remove(op.getFigi());
                } else {
                    remainingInstruments.put(op.getFigi(), cur - op.getQuantity());
                }
            } else if (op.getOperationType() != OperationType.OPERATION_TYPE_BROKER_FEE) {
                log.error("Неизвестный тип операции: {}", op);
            }
        }
        return remainingInstruments;
    }

    /**
     * Добавить последнюю цену.
     */
    @Override
    public void addLastPrice(LastPrice price) {
        lastPriceByFigi.put(price.getFigi(), Utility.toBigDecimal(price.getPrice()));
        checkStopRequests(price);
    }

    /**
     * Метод обрабатывает новую операцию и добавляет стоп запросы
     */
    @Override
    public void processNewOperation(User user, String figi, BigDecimal takeProfit, BigDecimal price, BigDecimal stopLoss, OrderDirection direction) {
        var stopRequestDirection = direction == OrderDirection.ORDER_DIRECTION_BUY ? OrderDirection.ORDER_DIRECTION_SELL : OrderDirection.ORDER_DIRECTION_BUY;
        var instrument = client.getInstrument(user, figi);
        addStopRequest(new StopRequest(user, figi, takeProfit, stopLoss, stopRequestDirection));
        addToBalance(user, getPriceInRubles(user, price, figi).multiply(BigDecimal.valueOf(instrument.getLot())));
    }

    /**
     * Метод перебирает все стоп запросы, выставляя на биржу нужные.
     */
    @Override
    public void checkStopRequests(LastPrice price) {
        if (!openStopRequests.containsKey(price.getFigi())) return;
        var requests = openStopRequests.get(price.getFigi());
        var lastPrice = Utility.toBigDecimal(price.getPrice());
        requests.removeIf(request -> {
            var user = request.user();
            var figi = request.figi();
            var instrument = client.getInstrument(user, figi);
            if (request.direction() == OrderDirection.ORDER_DIRECTION_SELL) {
                // Сработала стоп заявка для позиции в лонг
                BigDecimal sellPrice;
                if (request.takeProfit().compareTo(lastPrice) <= 0) {
                    sellPrice = request.takeProfit();
                } else if (request.stopLoss().compareTo(lastPrice) > 0) {
                    sellPrice = request.stopLoss();
                } else {
                    return false;
                }
                log.info("Сработала стоп-заяка для {}, продажа по цене {}", figi, sellPrice);
                var response = client.sellMarket(user, figi, sellPrice).join();
                if (response != null) {
                    subtractFromBalance(user, getPriceInRubles(user, sellPrice, figi).multiply(BigDecimal.valueOf(instrument.getLot())));
                }
                return response != null;
            } else if (request.direction() == OrderDirection.ORDER_DIRECTION_BUY) {
                // Сработала стоп заявка для позиции в шорт
                BigDecimal buyPrice;
                if (request.takeProfit().compareTo(lastPrice) >= 0) {
                    buyPrice = request.takeProfit();
                } else if (request.stopLoss().compareTo(lastPrice) < 0) {
                    buyPrice = request.stopLoss();
                } else {
                    return false;
                }
                log.info("Сработала стоп-заяка для {}, покупка по цене {}", figi, buyPrice);
                var response = client.buyMarket(user, figi, buyPrice).join();
                if (response != null) {
                    subtractFromBalance(user, getPriceInRubles(user, buyPrice, figi).multiply(BigDecimal.valueOf(instrument.getLot())));
                }
                return response != null;
            }
            return false;
        });
    }

    /**
     * Удаляет все стоп запросы для пользователя
     */
    @Override
    public void deleteRequestsForUser(User user) {
        log.info("Удаление всех запросов для пользователя {}", user);
        for (String figi : user.figis()) {
            if (!openStopRequests.containsKey(figi)) {
                continue;
            }
            var requests = openStopRequests.get(figi);
            requests.removeIf(r -> r.user().equals(user));
        }
    }

    /**
     * Переводит указанную цену в рубли
     */
    @Override
    public BigDecimal getPriceInRubles(User user, BigDecimal price, String figi) {
        String currency = client.getInstrument(user, figi).getCurrency();
        if (currency.equalsIgnoreCase("rub")) {
            return price;
        }
        var optionalCurrency = client.loadAllCurrencies(user).stream().filter(c -> c.getIsoCurrencyName().equalsIgnoreCase(currency)).findAny();
        if (optionalCurrency.isEmpty()) {
            throw new IllegalArgumentException("Не получается найти валюту " + currency);
        }
        BigDecimal lastCurrencyPrice = getLastPrice(user, optionalCurrency.get().getFigi());
        return lastCurrencyPrice.multiply(price);
    }

    /**
     * Возвращает количество рублей, потраченных стратегией
     */
    @Override
    public BigDecimal getBalance(User user) {
        return spentByUser.getOrDefault(user, BigDecimal.ZERO);
    }

    /**
     * Добавляет указанное количество рублей к уже потраченным (нужно, чтобы стратегия не вышла за лимит, поставленный пользователем)
     *
     * <p>Используется, например, при покупке бумаги стратегией </p>
     */
    private void addToBalance(User user, BigDecimal price) {
        spentByUser.merge(user, price, BigDecimal::add);
    }

    /**
     * Возвращает потраченные деньги (нужно, чтобы стратегия не вышла за лимит, поставленный пользователем)
     * <p>Используется, например, при продаже бумаги стратегией </p>
     */
    private void subtractFromBalance(User user, BigDecimal price) {
        spentByUser.merge(user, price, BigDecimal::subtract);
    }

    /**
     * Добавить новый стоп запрос
     */
    private void addStopRequest(StopRequest request) {
        if (!openStopRequests.containsKey(request.figi())) {
            openStopRequests.put(request.figi(), new ConcurrentLinkedQueue<>());
        }
        openStopRequests.get(request.figi()).add(request);
    }
}

