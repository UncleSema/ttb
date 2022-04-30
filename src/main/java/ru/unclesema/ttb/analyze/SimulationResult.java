package ru.unclesema.ttb.analyze;

import ru.unclesema.ttb.model.CachedInstrument;

import java.math.BigDecimal;
import java.util.List;

public record SimulationResult(BigDecimal initBalance, BigDecimal resultBalance, BigDecimal remainingCost,
                               BigDecimal delta, List<CachedInstrument> remaining) {
    public SimulationResult(BigDecimal initBalance, BigDecimal resultBalance, BigDecimal remainingCost, List<CachedInstrument> remaining) {
        this(initBalance, resultBalance, remainingCost, remainingCost.add(resultBalance).subtract(initBalance), remaining);
    }
}
