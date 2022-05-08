package ru.unclesema.ttb.cache;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.core.InvestApi;
import ru.unclesema.ttb.model.CachedCandle;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CandlesCache {
    private final InvestApi api;
    private final InstrumentsCache instrumentsCache;

    private static final CandleInterval interval = CandleInterval.CANDLE_INTERVAL_HOUR;

    private static final Map<CandleInterval, Integer> maxQueryIntervalDays = Map.of(
            CandleInterval.CANDLE_INTERVAL_1_MIN, 1,
            CandleInterval.CANDLE_INTERVAL_5_MIN, 1,
            CandleInterval.CANDLE_INTERVAL_15_MIN, 1,
            CandleInterval.CANDLE_INTERVAL_HOUR, 7,
            CandleInterval.CANDLE_INTERVAL_DAY, 365
    );

    /**
     * Собирает свечи за заданный промежуток времени.
     * Так как у Invest Api <a href="https://tinkoff.github.io/investAPI/faq_marketdata/#_7"> есть ограничения </a> по интервалу запроса,
     * то метод "дробит" запрос к api на несколько, максимально возможных по размеру, а затем склеивает ответы.
     *
     * @param figi <code>figi</code> инструмента
     * @param from с какого момента времени брать свечи
     * @param to   до какого момента времени брать свечи
     * @return <code>List</code> c кэшированными свечами
     */
    public List<CachedCandle> getCandles(@NonNull String figi, @NonNull LocalDateTime from, @NonNull LocalDateTime to) {
        // TODO optimize
        final int maxDays = maxQueryIntervalDays.get(interval);
        List<HistoricCandle> response = new ArrayList<>();
        while (from.plusDays(maxDays).isBefore(to)) {
            response.addAll(makeRequest(figi, from, from.plusDays(maxDays)));
            from = from.plusDays(maxDays);
        }
        if (!from.isEqual(to)) {
            response.addAll(makeRequest(figi, from, to));
        }
        return response.stream().map(CachedCandle::of).toList();
    }

    private List<HistoricCandle> makeRequest(String figi, LocalDateTime from, LocalDateTime to) {
        return api.getMarketDataService().getCandlesSync(figi, from.toInstant(ZoneOffset.UTC), to.toInstant(ZoneOffset.UTC), interval);
    }
}
