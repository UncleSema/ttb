package ru.unclesema.ttb.strategy;

import ru.tinkoff.piapi.contract.v1.Candle;

public interface CandleStrategy extends Strategy {
    StrategyDecision addCandle(Candle orderBook);
}
