package ru.unclesema.ttb.model;

import ru.tinkoff.piapi.contract.v1.Instrument;

public record CachedInstrument(String figi, String name) {
    public static CachedInstrument of(Instrument instrument) {
        return new CachedInstrument(instrument.getFigi(), instrument.getName());
    }

    @Override
    public String toString() {
        return String.format("%s (figi: %s)", name, figi);
    }
}
