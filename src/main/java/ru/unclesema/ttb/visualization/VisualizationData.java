package ru.unclesema.ttb.visualization;

import ru.unclesema.ttb.model.CachedInstrument;
import ru.unclesema.ttb.model.InstrumentData;

import java.util.List;

public record VisualizationData(CachedInstrument instrument, List<VisualizationCandle> visualizationCandles) {
    public VisualizationData of(InstrumentData instrumentData) {
        return new VisualizationData(instrumentData.instrument(), instrumentData.cachedCandles().stream().map(VisualizationCandle::of).toList());
    }
}
