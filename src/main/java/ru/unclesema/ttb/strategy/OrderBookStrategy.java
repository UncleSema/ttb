package ru.unclesema.ttb.strategy;

import ru.tinkoff.piapi.contract.v1.OrderBook;

public interface OrderBookStrategy extends Strategy {
    StrategyDecision addOrderBook(OrderBook orderBook);
}
