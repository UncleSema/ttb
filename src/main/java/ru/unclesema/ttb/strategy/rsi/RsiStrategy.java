package ru.unclesema.ttb.strategy.rsi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.tinkoff.piapi.contract.v1.Candle;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.StrategyDecision;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализация стратегии, основанной на подсчёте <code>RSI</code>.
 * <p>
 * Стратегия использует следущие параметры:
 *     <ul>
 *         <li> rsiPeriod — период, за который считается RSI </li>
 *         <li> upperRsiThreshold — верхняя граница RSI (в %), сигнализирующая поход в long </li>
 *         <li> lowerRsiThreshold — нижняя граница RSI (в %), сигнализирующая поход в short </li>
 *     </ul>
 * </p>
 *
 * <p> Реализация взята из <a href="https://github.com/hondasmx/rsi_strategy"> репозитория на GitHub </a> </p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RsiStrategy implements CandleStrategy {
    private int rsiPeriod = 14;
    private double upperRsiThreshold = 70;
    private double lowerRsiThreshold = 30;
    private double takeProfit = 5;
    private double stopLoss = 10;

    private final Map<String, List<Candle>> lastCandlesByFigi = new HashMap<>();

    @Override
    public StrategyDecision addCandle(Candle candle) {
        if (!lastCandlesByFigi.containsKey(candle.getFigi())) {
            lastCandlesByFigi.put(candle.getFigi(), new ArrayList<>());
        }
        var lastCandles = lastCandlesByFigi.get(candle.getFigi());
        if (lastCandles.size() == rsiPeriod) {
            lastCandles.remove(0);
        }
        lastCandles.add(candle);
        var rsi = rsi(candle.getFigi()).doubleValue();
        if (rsi >= upperRsiThreshold) {
            return StrategyDecision.BUY;
        }
        if (rsi <= lowerRsiThreshold) {
            return StrategyDecision.SELL;
        }
        return StrategyDecision.NOTHING;
    }

    private BigDecimal rsi(String figi) {
        var totalGain = BigDecimal.ZERO;
        var gainAmount = 0;
        var totalLoss = BigDecimal.ZERO;
        var lossAmount = 0;

        var lastCandles = lastCandlesByFigi.get(figi);
        for (int i = 1; i < lastCandles.size(); i++) {
            var candleClosePrice = Utility.toBigDecimal(lastCandles.get(i).getClose());
            var prevCandleClosePrice = Utility.toBigDecimal(lastCandles.get(i - 1).getClose());
            var change = candleClosePrice.subtract(prevCandleClosePrice);
            if (candleClosePrice.equals(prevCandleClosePrice)) continue;

            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                totalGain = totalGain.add(change);
                gainAmount++;
            } else {
                totalLoss = totalLoss.add(change);
                lossAmount++;
            }
        }
        if (gainAmount == 0) gainAmount = 1;
        if (lossAmount == 0) lossAmount = 1;

        var avgGain = totalGain.divide(BigDecimal.valueOf(gainAmount), RoundingMode.DOWN);
        if (avgGain.equals(BigDecimal.ZERO)) avgGain = BigDecimal.ONE;

        var avgLoss = totalLoss.divide(BigDecimal.valueOf(lossAmount), RoundingMode.DOWN);
        if (avgLoss.equals(BigDecimal.ZERO)) avgLoss = BigDecimal.ONE;

        var rs = avgGain.divide(avgLoss, RoundingMode.DOWN).abs();
        var hundred = BigDecimal.valueOf(100);
        return hundred.subtract(hundred.divide(BigDecimal.ONE.add(rs), RoundingMode.DOWN));
    }

    @Override
    public String getName() {
        return "RSI";
    }

    @Override
    public double getTakeProfit() {
        return takeProfit;
    }

    @Override
    public double getStopLoss() {
        return stopLoss;
    }

    @Override
    public Map<String, Object> getUIAttributes() {
        return Map.of("takeProfit", takeProfit,
                "stopLoss", stopLoss,
                "upperRsiThreshold", upperRsiThreshold,
                "lowerRsiThreshold", lowerRsiThreshold,
                "rsiPeriod", rsiPeriod);
    }
}
