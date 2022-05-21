package ru.unclesema.ttb;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.service.ApplicationService;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.orderbook.OrderBookStrategyImpl;
import ru.unclesema.ttb.strategy.rsi.RsiStrategy;

import java.util.List;

@Component
public class ApplicationModule {
    @Bean
    public TradesSubscriber getTradesSubscriber(ApplicationService service) {
        return new TradesSubscriber(service);
    }

    @Bean
    public InvestClient getInvestClient() {
        return new InvestClient();
    }

    @Bean
    public List<Strategy> availableStrategies() {
        return List.of(new OrderBookStrategyImpl(), new RsiStrategy());
    }

    @Bean
    public List<CandleStrategy> availableCandleStrategies(List<Strategy> availableStrategies) {
        return availableStrategies.stream().filter(s -> s instanceof CandleStrategy).map(s -> (CandleStrategy) s).toList();
    }
}
