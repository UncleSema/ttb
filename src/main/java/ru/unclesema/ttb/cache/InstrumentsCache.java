package ru.unclesema.ttb.cache;

import lombok.RequiredArgsConstructor;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.core.InvestApi;
import ru.unclesema.ttb.model.CachedInstrument;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class InstrumentsCache {
    private final Map<String, CachedInstrument> instrumentByFigi = new HashMap<>();
    private final InvestApi api;

    private CachedInstrument loadInstrument(String figi) {
        Instrument instrument = api.getInstrumentsService().getInstrumentByFigiSync(figi);
        CachedInstrument cachedInstrument = CachedInstrument.of(instrument);
        instrumentByFigi.put(figi, cachedInstrument);
        return cachedInstrument;
    }

    public CachedInstrument getInstrumentByFigi(String figi) {
        if (!instrumentByFigi.containsKey(figi)) {
            return loadInstrument(figi);
        }
        return instrumentByFigi.get(figi);
    }
}
