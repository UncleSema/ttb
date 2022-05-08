package ru.unclesema.ttb.strategy.orderbook;

import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.StrategyConfig;
import ru.unclesema.ttb.strategy.StrategyDecision;

public class OrderBookStrategy implements Strategy {

    @Override
    public StrategyDecision addOrderBook(OrderBook orderBook) {
        if (orderBook.getAsksCount() > 2 * orderBook.getBidsCount()) {
            return StrategyDecision.SELL;
        } else if (2 * orderBook.getAsksCount() < orderBook.getBidsCount()) {
            return StrategyDecision.BUY;
        }
        return StrategyDecision.NOTHING;
    }

    @Override
    public boolean buy() {
        return false;
    }

    @Override
    public boolean sell() {
        return false;
    }

    @Override
    public StrategyConfig getConfig() {
        return null;
    }
}
