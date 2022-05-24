package ru.unclesema.ttb.service.front;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OperationType;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.service.price.PriceService;
import ru.unclesema.ttb.service.user.UserService;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для работы с UI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FrontServiceImpl implements FrontService {
    private final List<Strategy> availableStrategies;
    private final List<CandleStrategy> availableCandleStrategies;
    private final PriceService priceService;
    private final UserService userService;
    private final InvestClient investClient;

    /**
     * @return всех существующих пользователей
     */
    @Override
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * @return пользователь с заданным accountId
     * @throws IllegalArgumentException, если пользователь не найден
     */
    @Override
    public User findUser(String accountId) {
        return userService.findUserByAccountId(accountId);
    }

    /**
     * @return имя заданного инструмента
     */
    @Override
    public String getInstrumentName(User user, String figi) {
        return getInstrument(user, figi).getName();
    }

    /**
     * @return заданный инструмент
     */
    @Override
    public Instrument getInstrument(User user, String figi) {
        return investClient.getInstrument(user, figi);
    }

    /**
     * Конвертирует <code>LastPrice</code> в <code>String</code>
     */
    @Override
    public String lastPriceToString(User user, BigDecimal quantity, String figi) {
        String currency = getInstrument(user, figi).getCurrency();
        BigDecimal lastPrice = priceService.getLastPrice(user, figi);
        return lastPrice.multiply(quantity).doubleValue() + " " + currency.toUpperCase();
    }

    /**
     * Конвертирует <code>MoneyValue</code> в <code>String</code>
     */
    @Override
    public String moneyValueToString(User user, String figi, MoneyValue value) {
        Instrument instrument = getInstrument(user, figi);
        return Utility.toBigDecimal(value).abs().doubleValue() + " " + instrument.getCurrency().toUpperCase();
    }

    /**
     * @return отчёт по стратегии
     */
    @Override
    public StrategyStatement getStatement(User user) {
        Map<String, BigDecimal> benefitByCurrency = new HashMap<>();
        // Обрабатываем купленные, но ещё не проданные бумаги
        for (var entry : priceService.getRemainingInstruments(user).entrySet()) {
            var instrument = investClient.getInstrument(user, entry.getKey());
            var amount = entry.getValue();
            var benefit = benefitByCurrency.getOrDefault(instrument.getCurrency(), BigDecimal.ZERO);
            benefit = benefit.add(priceService.getLastPrice(user, instrument.getFigi()).multiply(BigDecimal.valueOf(amount)));
            benefitByCurrency.put(instrument.getCurrency(), benefit);
        }
        var operations = getOperations(user);
        // Обрабатываем уже совершённые операции
        for (var op : operations) {
            if (op.getInstrumentType().equalsIgnoreCase("currency")) continue;
            var instrument = getInstrument(user, op.getFigi());
            var benefit = benefitByCurrency.getOrDefault(instrument.getCurrency(), BigDecimal.ZERO);
            var payment = Utility.toBigDecimal(op.getPayment()).abs();
            if (op.getOperationType() == OperationType.OPERATION_TYPE_BUY) {
                benefit = benefit.subtract(payment);
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_SELL) {
                benefit = benefit.add(payment);
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_BROKER_FEE) {
                benefit = benefit.subtract(payment);
            } else {
                log.error("Неизвестная операция {}", op.getOperationType());
            }
            benefitByCurrency.put(instrument.getCurrency(), benefit);
        }
        return new StrategyStatement(benefitByCurrency, operations);
    }

    /**
     * Конвертирует заработанные стратегией средства в <code>String</code>
     */
    @Override
    public String printBenefits(User user) {
        var statement = getStatement(user);
        var benefitByCurrency = statement.benefitByCurrency();
        if (benefitByCurrency.isEmpty()) {
            return "пока ничего :(";
        }
        return benefitByCurrency.entrySet()
                .stream()
                .map(entry -> entry.getValue().doubleValue() + " " + entry.getKey().toUpperCase())
                .collect(Collectors.joining(", "));
    }

    /**
     * Конвертирует <code>OperationType</code> в <code>String</code>
     */
    @Override
    public String operationTypeToString(OperationType operationType) {
        if (operationType == OperationType.OPERATION_TYPE_BUY) {
            return "Покупка";
        }
        if (operationType == OperationType.OPERATION_TYPE_SELL) {
            return "Продажа";
        }
        if (operationType == OperationType.OPERATION_TYPE_INPUT) {
            return "Пополнение";
        }
        if (operationType == OperationType.OPERATION_TYPE_OUTPUT) {
            return "Снятие";
        }
        if (operationType == OperationType.OPERATION_TYPE_BROKER_FEE) {
            return "Комиссия брокера";
        }
        return operationType.name();
    }

    /**
     * @return возвращает дату операции
     */
    @Override
    public LocalDateTime getDate(Operation op) {
        return LocalDateTime.ofInstant(Utility.toInstant(op.getDate()), ZoneId.systemDefault());
    }

    /**
     * Получение купленных, но ещё не проданных стратегией, бумаг.
     */
    @Override
    public Map<String, Long> getRemainingInstruments(User user) {
        return priceService.getRemainingInstruments(user);
    }

    /**
     * Проверка на то, что пользователь активный
     */
    @Override
    public boolean isActive(User user) {
        return userService.isActive(user);
    }

    /**
     * Получить доступные для выбора стратегии.
     */
    @Override
    public List<Strategy> getAvailableStrategies() {
        return availableStrategies;
    }

    /**
     * Получить доступные для выбора стратегии, использующие свечи.
     */
    @Override
    public List<CandleStrategy> getAvailableCandleStrategies() {
        return availableCandleStrategies;
    }

    /**
     * Получить все операции, произведенных с начала работы приложения по текущий момент.
     */
    @Override
    public List<Operation> getOperations(User user) {
        return investClient.getOperations(user);
    }

    /**
     * Получить количество потраченных стратегией рублей.
     */
    @Override
    public BigDecimal getBalance(User user) {
        return priceService.getBalance(user);
    }

    /**
     * Проверка на то, что пользователь с данным <code>accountId</code> существует.
     */
    @Override
    public boolean contains(String accountId) {
        return userService.contains(accountId);
    }
}
