package ru.unclesema.ttb.strategy;

public class TestStrategy implements Strategy {

    private final TestConfig config = new TestConfig();

    @Override
    public boolean buy() {
        return false;
    }

    @Override
    public boolean sell() {
        return false;
    }

    @Override
    public StrategyConfig getConfig() {
        return config;
    }
}

class TestConfig implements StrategyConfig {

    @Override
    public String getName() {
        return "Test strategy";
    }

    @Override
    public String getDescription() {
        return "Check whether it works good";
    }
}