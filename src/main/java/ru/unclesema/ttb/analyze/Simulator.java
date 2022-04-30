package ru.unclesema.ttb.analyze;

import lombok.RequiredArgsConstructor;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.unclesema.ttb.cache.CandlesCache;
import ru.unclesema.ttb.cache.InstrumentsCache;
import ru.unclesema.ttb.model.CachedInstrument;
import ru.unclesema.ttb.strategy.Strategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class Simulator {
    private final Strategy strategy;
    private final CandlesCache candlesCache;
    private final List<String> figiList;
    private final AnalyzeClient client;
    private final InstrumentsCache instrumentsCache;

    private static BigDecimal candleToBigDecimal(HistoricCandle candle) {
        return BigDecimal.valueOf(candle.getClose().getUnits() + ((double) candle.getClose().getNano()) / 1_000_000_000);
    }

    public SimulationResult simulate(Instant from, Instant to) {
        List<List<HistoricCandle>> candles = new ArrayList<>();
        for (String figi : figiList) {
            candles.add(candlesCache.getCandles(figi, from, to));
        }
        BigDecimal initBalance = client.getBalance();
        for (int i = 0; i < candles.get(0).size(); i++) {
            for (int j = 0; j < candles.size(); j++) {
                String figi = figiList.get(j);
                HistoricCandle candle = candles.get(j).get(i);
                BigDecimal price = candleToBigDecimal(candle);
                if (strategy.buy()) {
                    client.buy(figi, price);
                } else if (strategy.sell()) {
                    client.sell(figi, price);
                }
            }
        }
        Map<String, Integer> indexByFigi = IntStream.range(0, figiList.size()).boxed().collect(Collectors.toMap(figiList::get, i -> i));
        BigDecimal resultBalance = client.getBalance();
        List<CachedInstrument> remaining = client.getFigi().stream().map(instrumentsCache::getInstrumentByFigi).toList();
        BigDecimal remainingCost = BigDecimal.ZERO;
        for (CachedInstrument instrument : remaining) {
            int index = indexByFigi.get(instrument.figi());
            List<HistoricCandle> instrumentCandles = candles.get(index);
            remainingCost = remainingCost.add(candleToBigDecimal(instrumentCandles.get(instrumentCandles.size() - 1)));
        }
        return new SimulationResult(initBalance, resultBalance, remainingCost, remaining);
    }
}
