package ru.unclesema.ttb.strategy;

import java.util.Map;

/**
 * Интерфейс, с помощью которого можно реализовать свою стратегию.
 */
public interface Strategy {
    String getName();

    default String getDescription() {
        return "";
    }

    double getTakeProfit();

    double getStopLoss();

    /**
     * @return параметры стратегии для UI.
     */
    Map<String, Object> getUIAttributes();
}

