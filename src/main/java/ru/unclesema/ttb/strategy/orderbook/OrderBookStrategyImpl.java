package ru.unclesema.ttb.strategy.orderbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.unclesema.ttb.strategy.OrderBookStrategy;
import ru.unclesema.ttb.strategy.StrategyDecision;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrderBookStrategyImpl implements OrderBookStrategy {
    private double takeProfit = 0.5;
    private double stopLoss = 1;
    private double asksBidsRatio = 2;

    @Override
    public StrategyDecision addOrderBook(OrderBook orderBook) {
        long asks = 0;
        long bids = 0;
        for (Order order : orderBook.getAsksList()) {
            asks += order.getQuantity();
        }
        for (Order order : orderBook.getBidsList()) {
            bids += order.getQuantity();
        }
        if (asks > asksBidsRatio * bids) {
            return StrategyDecision.SELL;
        }
        if (asksBidsRatio * asks < bids) {
            return StrategyDecision.BUY;
        }
        return StrategyDecision.NOTHING;
    }

    @Override
    public String getName() {
        return "Стакан";
    }

    @Override
    public String getDescription() {
        return "Стратегия, которая использует стакан";
    }

    @Override
    public Map<String, Object> getUIAttributes() {
        return Map.of("takeProfit", takeProfit, "stopLoss", stopLoss, "asksBidsRatio", asksBidsRatio);
    }
}
