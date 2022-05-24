package ru.unclesema.ttb.service;

import ru.tinkoff.piapi.contract.v1.Candle;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.unclesema.ttb.model.NewUserRequest;
import ru.unclesema.ttb.model.User;

import java.time.Instant;

/**
 * Основной сервис приложения, который отвечает за добавление новых пользователей, создание новых заявок, удаление существующих
 */
public interface ApplicationService {
    /**
     * Метод обрабатывает запрос о новом пользователе, обрабатывая ошибки:
     * <ul>
     *     <li>Параметры стратегии, пришедшие с UI, должны содержать имя стратегии</li>
     *     <li>Стратегия с заданным именем должна существовать ровно одна</li>
     *     <li>Токен пользователя не должен быть пустым</li>
     *     <li>Если <code>mode == MARKET</code>, то accountId не должен быть пустым </li>
     *     <li>Пользователя с заданным accountId не должно существовать</li>
     *     <li>Ошибки обращений к API</li>
     * </ul>
     */
    User addNewUser(NewUserRequest request);

    /**
     * Метод добавляет информацию о новом стакане, о чём оповещает все стратегии, использующие стаканы.
     */
    void addOrderBook(OrderBook orderBook);

    /**
     * Метод добавляет информацию о новой свече, о чём оповещает все стратегии, использующие свечи.
     */
    void addCandle(Candle candle);

    /**
     * Метод симулирует работу стратегию на заданном временном промежутке.
     */
    void simulate(User user, Instant from, Instant to, CandleInterval interval);

    /**
     * Подписывает пользователя на:
     * <ul>
     *     <li>Последние цены на выбранные инструменты</li>
     *     <li>Последний стакан, если стратегия использует стакан</li>
     *     <li>Последние свечи, если стратегия использует свечи</li>
     * </ul>
     */
    void enableStrategyForUser(String accountId);

    /**
     * Метод отменяет все подписки пользователя, делает его `неактивным`.
     */
    void disableStrategyForUser(String accountId);

    /**
     * Добавление последней цены
     */
    void addLastPrice(LastPrice lastPrice);

    /**
     * Продажа всех ценных бумаг пользователя, которые были накоплены стратегией.
     */
    void sellAll(String accountId);
}
