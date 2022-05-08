package ru.unclesema.ttb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceService {
    private final InvestClient client;

    private final HashMap<String, BigDecimal> lastPriceByFigi = new HashMap<>();
    private final HashMap<String, List<TakeProfitRequest>> takeProfits = new HashMap<>();
    private final HashMap<String, List<StopLossRequest>> stopLosses = new HashMap<>();

    public void addLastPrice(LastPrice price) {
        lastPriceByFigi.put(price.getFigi(), Utility.toBigDecimal(price.getPrice()));
        checkTakeProfits(price);
        checkStopLosses(price);
    }

    public BigDecimal getLastPrice(String figi) {
        return lastPriceByFigi.get(figi);
    }

    public void addTakeProfit(TakeProfitRequest request) {
        if (!takeProfits.containsKey(request.figi())) {
            takeProfits.put(request.figi(), new ArrayList<>());
        }
        takeProfits.get(request.figi()).add(request);
    }

    public void addStopLoss(StopLossRequest request) {
        if (!stopLosses.containsKey(request.figi())) {
            stopLosses.put(request.figi(), new ArrayList<>());
        }
        stopLosses.get(request.figi()).add(request);
    }

    private void checkTakeProfits(LastPrice price) {
        if (!takeProfits.containsKey(price.getFigi())) return;
        List<TakeProfitRequest> requests = takeProfits.getOrDefault(price.getFigi(), List.of());
        BigDecimal lastPrice = Utility.toBigDecimal(price.getPrice());
        requests.removeIf(request -> {
            if (request.price().compareTo(lastPrice) <= 0) {
                if (client.buy(request.user(), request.figi(), request.price())) {
                    return true;
                }
            }
            return false;
        });
    }

    private void checkStopLosses(LastPrice price) {
        if (!stopLosses.containsKey(price.getFigi())) return;
        List<StopLossRequest> requests = stopLosses.getOrDefault(price.getFigi(), List.of());
        BigDecimal lastPrice = Utility.toBigDecimal(price.getPrice());
        requests.removeIf(request -> {
            if (request.price().compareTo(lastPrice) >= 0) {
                if (client.sell(request.preferences(), request.figi(), request.price())) {
                    return true;
                }
            }
            return false;
        });
    }
}

record TakeProfitRequest(User user, String figi, BigDecimal price) {

}

record StopLossRequest(User preferences, String figi, BigDecimal price) {

}
