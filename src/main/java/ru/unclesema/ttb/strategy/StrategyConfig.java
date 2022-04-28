package ru.unclesema.ttb.strategy;

public interface StrategyConfig {
    String getName();

    default String getDescription() {
        return "";
    }
}
