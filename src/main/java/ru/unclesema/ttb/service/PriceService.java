package ru.unclesema.ttb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceService {
    private final InvestClient client;

    private final Map<String, LastPrice> lastPriceByFigi = new ConcurrentHashMap<>();
    private final Map<String, Queue<TakeProfitRequest>> takeProfits = new ConcurrentHashMap<>();
    private final Map<String, Queue<StopLossRequest>> stopLosses = new ConcurrentHashMap<>();

    public LastPrice getLastPrice(User user, String figi) {
        if (figi.equalsIgnoreCase("FG0000000000")) {
            return LastPrice.newBuilder().setPrice(Quotation.newBuilder().setUnits(1).build()).setFigi("FG0000000000").build();
        }
        if (lastPriceByFigi.containsKey(figi)) {
            return lastPriceByFigi.get(figi);
        }
        return loadLastPrice(user, figi);
    }

    @Cacheable(value = "5s")
    public LastPrice loadLastPrice(User user, String figi) {
        log.warn("Запрос к api о последней цене для {}. Он точно не должен быть закеширован?", figi);
        return user.api().getMarketDataService().getLastPricesSync(List.of(figi)).get(0);
    }

    public void addLastPrice(LastPrice price) {
        lastPriceByFigi.put(price.getFigi(), price);
        checkTakeProfits(price);
        checkStopLosses(price);
    }

    public void addTakeProfit(TakeProfitRequest request) {
        if (!takeProfits.containsKey(request.figi())) {
            takeProfits.put(request.figi(), new ConcurrentLinkedQueue<>());
        }
        takeProfits.get(request.figi()).add(request);
    }

    public void addStopLoss(StopLossRequest request) {
        if (!stopLosses.containsKey(request.figi())) {
            stopLosses.put(request.figi(), new ConcurrentLinkedQueue<>());
        }
        stopLosses.get(request.figi()).add(request);
    }

    private void checkTakeProfits(LastPrice price) {
        if (!takeProfits.containsKey(price.getFigi())) return;
        Queue<TakeProfitRequest> requests = takeProfits.get(price.getFigi());
        BigDecimal lastPrice = Utility.toBigDecimal(price.getPrice());
        requests.removeIf(request -> {
            if (request.price().compareTo(lastPrice) <= 0) {
                if (request.direction() == OrderDirection.ORDER_DIRECTION_SELL && client.sellMarket(request.user(), request.figi(), request.price())) {
                    return true;
                }
            } else {
                if (request.direction() == OrderDirection.ORDER_DIRECTION_BUY && client.buyMarket(request.user(), request.figi(), request.price())) {
                    return true;
                }
            }
            return false;
        });
    }

    private void checkStopLosses(LastPrice price) {
        if (!stopLosses.containsKey(price.getFigi())) return;
        Queue<StopLossRequest> requests = stopLosses.get(price.getFigi());
        BigDecimal lastPrice = Utility.toBigDecimal(price.getPrice());
        requests.removeIf(request -> {
            if (request.price().compareTo(lastPrice) >= 0) {
                if (request.direction() == OrderDirection.ORDER_DIRECTION_SELL && client.sellMarket(request.user(), request.figi(), request.price())) {
                    return true;
                }
            } else {
                if (request.direction() == OrderDirection.ORDER_DIRECTION_BUY && client.buyMarket(request.user(), request.figi(), request.price())) {
                    return true;
                }
            }
            return false;
        });
    }

}

record TakeProfitRequest(User user, String figi, BigDecimal price, OrderDirection direction) {

}

record StopLossRequest(User user, String figi, BigDecimal price, OrderDirection direction) {

}
