package ru.unclesema.ttb.analyze;

import ru.unclesema.ttb.model.CachedInstrument;
import ru.unclesema.ttb.visualization.VisualizationData;

import java.math.BigDecimal;
import java.util.List;

public record SimulationResult(BigDecimal initBalance, BigDecimal resultBalance, BigDecimal remainingCost,
                               BigDecimal delta, List<CachedInstrument> remaining, List<VisualizationData> history) {
    public SimulationResult(BigDecimal initBalance, BigDecimal resultBalance, BigDecimal remainingCost, List<CachedInstrument> remaining, List<VisualizationData> history) {
        this(initBalance, resultBalance, remainingCost, remainingCost.add(resultBalance).subtract(initBalance), remaining, history);
    }
}

