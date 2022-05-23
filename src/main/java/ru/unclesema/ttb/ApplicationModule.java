package ru.unclesema.ttb;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.Descriptors;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.orderbook.OrderBookStrategyImpl;
import ru.unclesema.ttb.strategy.rsi.RsiStrategy;

import java.util.List;

@Component
public class ApplicationModule {
    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        module = module.addKeyDeserializer(Descriptors.FieldDescriptor.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) {
                return key;
            }
        });
        return new ObjectMapper().registerModule(module);
    }

    @Bean
    public List<Strategy> availableStrategies() {
        return List.of(new OrderBookStrategyImpl(), new RsiStrategy());
    }

    @Bean
    public List<CandleStrategy> availableCandleStrategies(List<Strategy> availableStrategies) {
        return availableStrategies.stream()
                .filter(CandleStrategy.class::isInstance)
                .map(CandleStrategy.class::cast)
                .toList();
    }
}
