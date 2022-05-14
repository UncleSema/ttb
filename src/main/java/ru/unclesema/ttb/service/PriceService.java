package ru.unclesema.ttb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.client.InvestClient;
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

    private final Map<String, BigDecimal> lastPriceByFigi = new ConcurrentHashMap<>();
    private final Map<String, Queue<StopRequest>> openStopRequests = new ConcurrentHashMap<>();

    public BigDecimal getLastPrice(User user, String figi) {
        if (figi.equalsIgnoreCase("FG0000000000")) {
            return BigDecimal.ONE;
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

    public void addStopRequest(StopRequest request) {
        if (!openStopRequests.containsKey(request.figi())) {
            openStopRequests.put(request.figi(), new ConcurrentLinkedQueue<>());
        }
        openStopRequests.get(request.figi()).add(request);
    }

    private void checkStopRequests(LastPrice price) {
        if (!openStopRequests.containsKey(price.getFigi())) return;
        Queue<StopRequest> requests = openStopRequests.get(price.getFigi());
        BigDecimal lastPrice = Utility.toBigDecimal(price.getPrice());
        requests.removeIf(request -> {
            if (request.direction() == OrderDirection.ORDER_DIRECTION_SELL) {
                BigDecimal sellPrice;
                if (request.takeProfit().compareTo(lastPrice) <= 0) {
                    sellPrice = request.takeProfit();
                } else if (request.stopLoss().compareTo(lastPrice) > 0) {
                    sellPrice = request.stopLoss();
                } else {
                    return false;
                }
                log.info("Сработала стоп-заяка для {}, продажа по цене {}", price.getFigi(), sellPrice);
                return client.sellMarket(request.user(), request.figi(), sellPrice).join() != null;
            } else if (request.direction() == OrderDirection.ORDER_DIRECTION_BUY) {
                BigDecimal buyPrice;
                if (request.takeProfit().compareTo(lastPrice) >= 0) {
                    buyPrice = request.takeProfit();
                } else if (request.stopLoss().compareTo(lastPrice) < 0) {
                    buyPrice = request.stopLoss();
                } else {
                    return false;
                }
                log.info("Сработала стоп-заяка для {}, покупка по цене {}", price.getFigi(), buyPrice);
                return client.buyMarket(request.user(), request.figi(), buyPrice).join() != null;
            }
            return false;
        });
    }
}

record StopRequest(User user, String figi, BigDecimal takeProfit, BigDecimal stopLoss, OrderDirection direction) {
}

