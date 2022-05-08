package ru.unclesema.ttb.strategy.ml;

import jsat.classifiers.CategoricalData;
import jsat.classifiers.ClassificationDataSet;
import jsat.classifiers.DataPoint;
import ru.unclesema.ttb.model.CachedCandle;
import ru.unclesema.ttb.model.InstrumentData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSet {
    private final int slices = 100;
    private final BigDecimal commission = BigDecimal.valueOf(1 - 0.1 / 100);
    private final BigDecimal goalProfit = BigDecimal.valueOf(0.01);

    public ClassificationDataSet of(List<InstrumentData> dataList) {
        Map<String, Integer> indexByFigi = new HashMap<>();
        int lastIndex = 0;
        CategoricalData instruments = new CategoricalData(dataList.size());
        CategoricalData goal = new CategoricalData(3);
        for (InstrumentData data : dataList) {
            String figi = data.instrument().figi();
            if (!indexByFigi.containsKey(figi)) {
                indexByFigi.put(figi, lastIndex++);
            }
            int currentFigiIndex = indexByFigi.get(figi);
            List<DataPoint> dataPoints = new ArrayList<>();
            for (int i = slices; i + slices < data.cachedCandles().size(); i += slices) {
                List<CachedCandle> earlierCandles = data.cachedCandles().subList(0, i);
                List<CachedCandle> laterCandles = data.cachedCandles().subList(i, data.cachedCandles().size());

                double[] numericalValues = earlierCandles.stream()
                        .map(CachedCandle::closePrice)
                        .mapToDouble(BigDecimal::doubleValue)
                        .toArray();

                int[] categoricalValues = {currentFigiIndex,};
                CategoricalData[] categoricalData = {instruments, goal};
//                DataPoint dataPoint = new DataPoint(new DenseVector(numericalValues), categoricalValues, )
//                dataPoints.add()
            }
        }
//        ClassificationDataSet dataSet = new ClassificationDataSet();
        return null;
    }

    private boolean shouldLong(BigDecimal initialPrice, List<CachedCandle> candles) {
        LocalDateTime lastDay = candles.get(0).ts();
        BigDecimal goalPrice = goalProfit.add(BigDecimal.ONE).multiply(initialPrice);
        BigDecimal currentCommission = commission;
        for (CachedCandle candle : candles) {
            LocalDateTime now = candle.ts();
            if (lastDay.plusDays(1).isBefore(now)) {
                currentCommission = currentCommission.multiply(commission);
            }
            BigDecimal price = candle.closePrice().multiply(currentCommission);
            if (goalPrice.compareTo(price) <= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldShort(BigDecimal initialPrice, List<CachedCandle> candles) {
        LocalDateTime lastDay = candles.get(0).ts();
        BigDecimal goalPrice = BigDecimal.ONE.subtract(goalProfit).multiply(initialPrice);
        BigDecimal currentCommission = commission;
        for (CachedCandle candle : candles) {
            LocalDateTime now = candle.ts();
            if (lastDay.plusDays(1).isBefore(now)) {
                currentCommission = currentCommission.multiply(commission);
            }
            BigDecimal price = candle.closePrice().multiply(currentCommission);
            if (goalPrice.compareTo(price) >= 0) {
                return true;
            }
        }
        return false;
    }
}
