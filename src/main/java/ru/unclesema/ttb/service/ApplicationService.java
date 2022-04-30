package ru.unclesema.ttb.service;

import lombok.RequiredArgsConstructor;
import ru.tinkoff.piapi.core.InvestApi;
import ru.unclesema.ttb.analyze.AnalyzeClient;
import ru.unclesema.ttb.analyze.SimulationResult;
import ru.unclesema.ttb.analyze.Simulator;
import ru.unclesema.ttb.cache.CandlesCache;
import ru.unclesema.ttb.cache.InstrumentsCache;
import ru.unclesema.ttb.strategy.TestStrategy;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ApplicationService {
    private final CandlesCache candlesCache;
    private final AnalyzeClient analyzeClient;
    private final InvestApi api;
    private final InstrumentsCache instrumentsCache;

    public SimulationResult simulate(List<String> figiList, Instant from, Instant to) {
        Simulator simulator = new Simulator(new TestStrategy(), candlesCache, figiList, analyzeClient, instrumentsCache);
        return simulator.simulate(from, to);
    }

    public String figi() {
        return api.getInstrumentsService().getAllSharesSync().stream().map(s -> s.getName() + " " + s.getFigi() + "\n").collect(Collectors.joining("\n"));
    }
}
