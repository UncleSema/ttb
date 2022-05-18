package ru.unclesema.ttb;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.orderbook.OrderBookStrategyImpl;
import ru.unclesema.ttb.strategy.rsi.RsiStrategy;

import java.util.List;

@Component
public class ApplicationModule {

    @Bean
    public InvestClient getInvestClient() {
        return new InvestClient();
    }

    @Bean
    public List<Strategy> availableStrategies() {
        return List.of(new OrderBookStrategyImpl(), new RsiStrategy());
    }
}
