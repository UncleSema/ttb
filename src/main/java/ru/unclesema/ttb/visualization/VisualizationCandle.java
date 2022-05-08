package ru.unclesema.ttb.visualization;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.unclesema.ttb.model.CachedCandle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode
public class VisualizationCandle implements Comparable<VisualizationCandle> {
    public boolean wasBought = false;
    public boolean wasSold = false;
    @Getter
    private final LocalDateTime ts;
    @Getter
    private final BigDecimal closePrice;

    public VisualizationCandle(LocalDateTime ts, BigDecimal closePrice) {
        this.ts = ts;
        this.closePrice = closePrice;
    }

    public static VisualizationCandle of(CachedCandle cachedCandle) {
        return new VisualizationCandle(cachedCandle.ts(), cachedCandle.closePrice());
    }

    @Override
    public int compareTo(VisualizationCandle visualizationCandle) {
        return ts.compareTo(visualizationCandle.ts);
    }
}
