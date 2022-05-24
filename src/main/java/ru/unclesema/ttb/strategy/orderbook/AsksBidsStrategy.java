package ru.unclesema.ttb.strategy.orderbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.unclesema.ttb.strategy.OrderBookStrategy;
import ru.unclesema.ttb.strategy.StrategyDecision;

import java.util.Map;

/**
 * Реализация стратегии, работающей со стаканом.
 *
 * <p> Стратегия использует параметр <code>asksBidsRatio</code>: если отношение запросов на покупку больше запросов на продажу в
 * <code>asksBidsRatio</code> раз, то стратегия покупает бумагу, если меньше в <code>asksBidsRatio</code> раз, то продает. </p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AsksBidsStrategy implements OrderBookStrategy {
    private double takeProfit = 0.5;
    private double stopLoss = 1;
    private double asksBidsRatio = 2;

    @Override
    public StrategyDecision addOrderBook(OrderBook orderBook) {
        var asks = 0L;
        var bids = 0L;
        for (var order : orderBook.getAsksList()) {
            asks += order.getQuantity();
        }
        for (var order : orderBook.getBidsList()) {
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
    public Map<String, Object> getUIAttributes() {
        return Map.of("takeProfit", takeProfit, "stopLoss", stopLoss, "asksBidsRatio", asksBidsRatio);
    }
}
