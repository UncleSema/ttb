package ru.unclesema.ttb.service.front;

import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OperationType;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.Strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с UI
 */
public interface FrontService {
    /**
     * @return всех существующих пользователей
     */
    List<User> getAllUsers();

    /**
     * @return пользователь с заданным accountId
     * @throws IllegalArgumentException, если пользователь не найден
     */
    User findUser(String accountId);

    /**
     * @return имя заданного инструмента
     */
    String getInstrumentName(User user, String figi);

    /**
     * @return заданный инструмент
     */
    Instrument getInstrument(User user, String figi);

    /**
     * Конвертирует <code>LastPrice</code> в <code>String</code>
     */
    String lastPriceToString(User user, BigDecimal quantity, String figi);

    /**
     * Конвертирует <code>MoneyValue</code> в <code>String</code>
     */
    String moneyValueToString(User user, String figi, MoneyValue value);

    /**
     * @return отчёт по стратегии
     */
    StrategyStatement getStatement(User user);

    /**
     * Конвертирует заработанные стратегией средства в <code>String</code>
     */
    String printBenefits(User user);

    /**
     * Конвертирует <code>OperationType</code> в <code>String</code>
     */
    String operationTypeToString(OperationType operationType);

    /**
     * @return возвращает дату операции
     */
    LocalDateTime getDate(Operation op);

    /**
     * Получение купленных, но ещё не проданных стратегией, бумаг.
     */
    Map<String, Long> getRemainingInstruments(User user);

    /**
     * Проверка на то, что пользователь активный
     */
    boolean isActive(User user);

    /**
     * Получить доступные для выбора стратегии.
     */
    List<Strategy> getAvailableStrategies();

    /**
     * Получить доступные для выбора стратегии, использующие свечи.
     */
    List<CandleStrategy> getAvailableCandleStrategies();

    /**
     * Получить все операции, произведенных с начала работы приложения по текущий момент.
     */
    List<Operation> getOperations(User user);

    /**
     * Получить количество потраченных стратегией рублей.
     */
    BigDecimal getBalance(User user);

    /**
     * Проверка на то, что пользователь с данным <code>accountId</code> существует.
     */
    boolean contains(String accountId);
}
