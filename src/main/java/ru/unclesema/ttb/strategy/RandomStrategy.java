package ru.unclesema.ttb.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Random;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RandomStrategy implements Strategy {
    private double takeProfit = 0.5;
    private double stopLoss = 1;
    private int seed = 1;

    private final Random random = new Random(seed);

    @Override
    public String getName() {
        return "Random";
    }

    @Override
    public String getDescription() {
        return Strategy.super.getDescription();
    }

    @Override
    public double getTakeProfit() {
        return takeProfit;
    }

    @Override
    public double getStopLoss() {
        return stopLoss;
    }

    @Override
    public Map<String, Object> getUIAttributes() {
        return Map.of("takeProfit", takeProfit, "stopLoss", stopLoss, "seed", seed);
    }
}
