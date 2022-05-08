package ru.unclesema.ttb.strategy;

public interface StrategyConfig {
    String getName();

    String getDescription();

    double takeProfit();

    double stopLoss();
}
