package ru.unclesema.ttb.strategy;

import ru.tinkoff.piapi.contract.v1.OrderBook;

/**
 * Интерфейс стратегии, работающей со стаканом
 */
public interface OrderBookStrategy extends Strategy {
    /**
     * Метод, к которому обращается основной сервис, когда Tinkoff Api ему передает новый стакан.
     *
     * @param orderBook новый стакан
     * @return покупать / продавать / ничего не делать
     */
    StrategyDecision addOrderBook(OrderBook orderBook);
}
