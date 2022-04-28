package ru.unclesema.ttb;

import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.strategy.TestStrategy;

import java.util.List;

public class ApplicationConfig {
    private final String token;

    public ApplicationConfig() {
        token = System.getenv("TOKEN");
    }

    public String getToken() {
        return token;
    }

    public List<Strategy> getStrategies() {
        return List.of(new TestStrategy());
    }
}
