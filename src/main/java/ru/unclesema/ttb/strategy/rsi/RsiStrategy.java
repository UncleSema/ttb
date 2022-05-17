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
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RsiStrategy implements CandleStrategy {
    private int rsiPeriod = 14;
    private BigDecimal upperRsiThreshold = BigDecimal.valueOf(70);
    private BigDecimal lowerRsiThreshold = BigDecimal.valueOf(30);
    private BigDecimal takeProfit = BigDecimal.valueOf(15);
    private BigDecimal stopLoss = BigDecimal.valueOf(5);

    private final List<Candle> lastCandlesList = new ArrayList<>();

    @Override
    public StrategyDecision addCandle(Candle candle) {
        if (lastCandlesList.size() == rsiPeriod) {
            lastCandlesList.remove(0);
            lastCandlesList.add(candle);
        }
        if (rsi().compareTo(upperRsiThreshold) >= 0) {
            return StrategyDecision.BUY;
        }
        if (rsi().compareTo(lowerRsiThreshold) <= 0) {
            return StrategyDecision.SELL;
        }
        return StrategyDecision.NOTHING;
    }

    private BigDecimal rsi() {
        BigDecimal totalGain = BigDecimal.ZERO;
        int gainAmount = 0;
        BigDecimal totalLoss = BigDecimal.ZERO;
        int lossAmount = 0;

        for (int i = 1; i < lastCandlesList.size(); i++) {
            BigDecimal candleClosePrice = Utility.toBigDecimal(lastCandlesList.get(i).getClose());
            BigDecimal prevCandleClosePrice = Utility.toBigDecimal(lastCandlesList.get(i - 1).getClose());
            BigDecimal change = candleClosePrice.subtract(prevCandleClosePrice);
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

        BigDecimal avgGain = totalGain.divide(BigDecimal.valueOf(gainAmount), RoundingMode.DOWN);
        if (avgGain.equals(BigDecimal.ZERO)) avgGain = BigDecimal.ONE;

        BigDecimal avgLoss = totalLoss.divide(BigDecimal.valueOf(lossAmount), RoundingMode.DOWN);
        if (avgLoss.equals(BigDecimal.ZERO)) avgLoss = BigDecimal.ONE;

        BigDecimal rs = avgGain.divide(avgLoss, RoundingMode.DOWN).abs();
        BigDecimal hundred = BigDecimal.valueOf(100);
        return hundred.subtract(hundred.divide(BigDecimal.ONE.add(rs), RoundingMode.DOWN));
    }

    @Override
    public String getName() {
        return "RSI";
    }

    @Override
    public double getTakeProfit() {
        return takeProfit.doubleValue();
    }

    @Override
    public double getStopLoss() {
        return stopLoss.doubleValue();
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
