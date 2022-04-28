package ru.unclesema.ttb;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.service.ApplicationService;

@Component
public class ApplicationModule {

    @Bean
    public ApplicationConfig getApplicationConfig() {
        return new ApplicationConfig();
    }

    @Bean
    public InvestClient getInvestClient(ApplicationConfig config) {
        return new InvestClient(config);
    }

    @Bean
    public ApplicationService getApplicationService(InvestClient client, ApplicationConfig config) {
        return new ApplicationService(client, config);
    }
}
