package ru.unclesema.ttb.cache;

import lombok.RequiredArgsConstructor;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class CandlesCache {
    private final InvestApi api;
    private static final CandleInterval interval = CandleInterval.CANDLE_INTERVAL_5_MIN;

    /**
     * Собирает свечи за заданный промежуток времени
     *
     * @param figi <code>figi</code> инструмента
     * @param from с какого момента времени брать свечи
     * @param to   до какого момента времени брать свечи
     * @return лист свечей
     */
    public List<HistoricCandle> getCandles(String figi, Instant from, Instant to) {
        // TODO optimize
        return api.getMarketDataService().getCandlesSync(figi, from, to, interval);
    }
}
