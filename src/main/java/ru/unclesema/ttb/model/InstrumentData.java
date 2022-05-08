package ru.unclesema.ttb.model;

import java.util.List;

public record InstrumentData(CachedInstrument instrument, List<CachedCandle> cachedCandles) {
}
