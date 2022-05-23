package ru.unclesema.ttb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.model.UserMode;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {
    private final InvestClient client;
    private final AnalyzeService analyzeService;

    private final Map<String, BigDecimal> lastPriceByFigi = new ConcurrentHashMap<>();
    private final Map<String, Queue<StopRequest>> openStopRequests = new ConcurrentHashMap<>();
    private final Map<User, BigDecimal> spentByUser = new ConcurrentHashMap<>();

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

    public void addLastPrice(LastPrice price) {
        lastPriceByFigi.put(price.getFigi(), Utility.toBigDecimal(price.getPrice()));
        checkStopRequests(price);
    }

    public BigDecimal getBalance(User user) {
        return spentByUser.getOrDefault(user, BigDecimal.ZERO);
    }

    private void addToBalance(User user, BigDecimal price) {
        spentByUser.merge(user, price, BigDecimal::add);
    }

    private void subtractFromBalance(User user, BigDecimal price) {
        spentByUser.merge(user, price, BigDecimal::subtract);
    }

    public void processNewOperation(User user, String figi, BigDecimal takeProfit, BigDecimal price, BigDecimal stopLoss, OrderDirection direction) {
        var stopRequestDirection = direction == OrderDirection.ORDER_DIRECTION_BUY ? OrderDirection.ORDER_DIRECTION_SELL : OrderDirection.ORDER_DIRECTION_BUY;
        addStopRequest(new StopRequest(user, figi, takeProfit, stopLoss, stopRequestDirection));
        addToBalance(user, getPriceInRubles(user, price, figi));
    }

    public void addStopRequest(StopRequest request) {
        if (!openStopRequests.containsKey(request.figi())) {
            openStopRequests.put(request.figi(), new ConcurrentLinkedQueue<>());
        }
        openStopRequests.get(request.figi()).add(request);
    }

    public void checkStopRequests(LastPrice price) {
        if (!openStopRequests.containsKey(price.getFigi())) return;
        var requests = openStopRequests.get(price.getFigi());
        var lastPrice = Utility.toBigDecimal(price.getPrice());
        requests.removeIf(request -> {
            var user = request.user();
            var figi = request.figi();
            if (request.direction() == OrderDirection.ORDER_DIRECTION_SELL) {
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
                    subtractFromBalance(user, getPriceInRubles(user, sellPrice, figi));
                }
                return response != null;
            } else if (request.direction() == OrderDirection.ORDER_DIRECTION_BUY) {
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
                    subtractFromBalance(user, getPriceInRubles(user, buyPrice, figi));
                }
                return response != null;
            }
            return false;
        });
    }

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
}

record StopRequest(User user, String figi, BigDecimal takeProfit, BigDecimal stopLoss, OrderDirection direction) {
}

