package ru.unclesema.ttb;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.unclesema.ttb.cache.PortfolioStorage;
import ru.unclesema.ttb.client.InvestClient;

@Component
public class ApplicationModule {

    @Bean
    public InvestClient getInvestClient() {
        return new InvestClient();
    }

//    @Bean
//    public AnalyzeClient getAnalyzeClient(InstrumentsCache instrumentsCache, PortfolioStorage storage) {
//        return new AnalyzeClient(instrumentsCache, storage);
//    }

//    @Bean
//    public UserService getUserService() {
//        return new UserService();
//    }

    @Bean
    public PortfolioStorage getPortfolioStorage() {
        return new PortfolioStorage();
    }

//    @Bean
//    public CandlesCache getCandlesCache(InvestApi api, InstrumentsCache instrumentsCache) {
//        return new CandlesCache(api, instrumentsCache);
//    }
//
//    @Bean
//    public InstrumentsCache getInstrumentsCache(InvestApi api) {
//        return new InstrumentsCache(api);
//    }
}
