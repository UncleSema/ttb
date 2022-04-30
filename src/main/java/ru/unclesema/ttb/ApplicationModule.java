package ru.unclesema.ttb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.tinkoff.piapi.core.InvestApi;
import ru.unclesema.ttb.analyze.AnalyzeClient;
import ru.unclesema.ttb.cache.CandlesCache;
import ru.unclesema.ttb.cache.InstrumentsCache;
import ru.unclesema.ttb.cache.PortfolioStorage;
import ru.unclesema.ttb.service.ApplicationService;

@Component
public class ApplicationModule {

    @Value("${app.config.token}")
    private String token;

    @Bean
    public ApplicationConfig getApplicationConfig() {
        return new ApplicationConfig();
    }

    @Bean
    public ApplicationService getApplicationService(CandlesCache candlesCache, AnalyzeClient client, InvestApi api, InstrumentsCache instrumentsCache) {
        return new ApplicationService(candlesCache, client, api, instrumentsCache);
    }

    @Bean
    public AnalyzeClient getAnalyzeClient(InstrumentsCache instrumentsCache, PortfolioStorage storage) {
        return new AnalyzeClient(instrumentsCache, storage);
    }

    @Bean
    public PortfolioStorage getPortfolioStorage() {
        return new PortfolioStorage();
    }

    @Bean
    public CandlesCache getCandlesCache(InvestApi api) {
        return new CandlesCache(api);
    }

    @Bean
    public InvestApi getInvestApi() {
        return InvestApi.createSandbox(token);
    }

    @Bean
    public InstrumentsCache getInstrumentsCache(InvestApi api) {
        return new InstrumentsCache(api);
    }
}
