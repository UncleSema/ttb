package ru.unclesema.ttb.strategy.orderbook;

import ru.unclesema.ttb.strategy.StrategyConfig;

import java.util.Map;

public record OrderBookConfig(double takeProfit, double stopLoss) implements StrategyConfig {
    private static final double TAKE_PROFIT = 0.5;
    private static final double STOP_LOSS = 1;

    public static final Map<String, Double> defaultParameters = Map.of("takeProfit", TAKE_PROFIT, "stopLoss", STOP_LOSS);

    public OrderBookConfig() {
        this(TAKE_PROFIT, STOP_LOSS);
    }

    @Override
    public String getName() {
        return "Стакан";
    }

    @Override
    public String getDescription() {
        return "Стратегия, которая следит за стаканом";
    }
}
