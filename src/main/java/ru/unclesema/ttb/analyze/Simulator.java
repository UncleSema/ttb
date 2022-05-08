package ru.unclesema.ttb.analyze;

import lombok.RequiredArgsConstructor;
import ru.unclesema.ttb.cache.CandlesCache;
import ru.unclesema.ttb.cache.InstrumentsCache;
import ru.unclesema.ttb.strategy.Strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class Simulator {
    private final Strategy strategy;
    private final CandlesCache candlesCache;
    private final List<String> figiList;
    //    private final AnalyzeClient client;
    private final InstrumentsCache instrumentsCache;

    public SimulationResult simulate(LocalDateTime from, LocalDateTime to) {
//        List<VisualizationData> candles = new ArrayList<>();
//        for (String figi : figiList) {
//            if (!figi.isEmpty()) {
//                candles.add(new VisualizationData(instrumentsCache.getInstrumentByFigi(figi), candlesCache.getCandles(figi, from, to).stream().map(VisualizationCandle::of).toList()));
//            }
//        }
//        BigDecimal initBalance = client.getBalance();
//        for (int i = 0; i < candles.get(0).visualizationCandles().size(); i++) {
//            for (int j = 0; j < candles.size(); j++) {
//                if (i < candles.get(j).visualizationCandles().size()) {
//                    String figi = figiList.get(j);
//                    VisualizationCandle candle = candles.get(j).visualizationCandles().get(i);
//                    BigDecimal price = candle.getClosePrice();
//                    if (strategy.buy()) {
//                        if (client.buy(figi, price)) {
//                            candle.wasBought = true;
//                        }
//                    } else if (strategy.sell()) {
//                        if (client.sell(figi, price)) {
//                            candle.wasSold = true;
//                        }
//                    }
//                }
//            }
//        }
//        Map<String, Integer> indexByFigi = IntStream.range(0, figiList.size()).boxed().collect(Collectors.toMap(figiList::get, i -> i));
//        BigDecimal resultBalance = client.getBalance();
//        List<CachedInstrument> remaining = client.getFigi().stream().map(instrumentsCache::getInstrumentByFigi).toList();
//        BigDecimal remainingCost = BigDecimal.ZERO;
//        for (CachedInstrument instrument : remaining) {
//            int index = indexByFigi.get(instrument.figi());
//            List<VisualizationCandle> instrumentCandles = candles.get(index).visualizationCandles();
//            remainingCost = remainingCost.add(instrumentCandles.get(instrumentCandles.size() - 1).getClosePrice());
//        }
//        return new SimulationResult(initBalance, resultBalance, remainingCost, remaining, candles);
        return new SimulationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
    }
}
