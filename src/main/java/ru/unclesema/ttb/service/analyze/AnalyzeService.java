package ru.unclesema.ttb.service.analyze;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.unclesema.ttb.model.User;

import java.math.BigDecimal;
import java.util.List;

/**
 * Сервис, отвечающий за режим анализа.
 */
public interface AnalyzeService {
    /**
     * Метод добавляет новую операцию, совершенную во время симуляции работы стратегии
     */
    void processOrder(User user, String figi, long quantity, BigDecimal price, OrderDirection direction);

    /**
     * @return Совершённые стратегией операции
     */
    List<Operation> getOperations(User user);

    /**
     * @return Цену последней добавленной свечи
     */
    LastPrice getLastPrice(User user, String figi);

    /**
     * Добавить новую свечу
     */
    void processNewCandle(User user, Candle candle);
}
