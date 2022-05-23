package ru.unclesema.ttb.strategy;

import ru.tinkoff.piapi.contract.v1.Candle;

/**
 * Интерфейс стратегии, работающей со свечой.
 */
public interface CandleStrategy extends Strategy {
    /**
     * Метод, к которому обращается основной сервис, когда Tinkoff Api ему передает новую свечу.
     *
     * @param candle новая свеча.
     * @return покупать / продавать / ничего не делать.
     */
    StrategyDecision addCandle(Candle candle);
}
