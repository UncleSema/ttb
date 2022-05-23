package ru.unclesema.ttb.service.front;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OperationType;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.service.ApplicationService;
import ru.unclesema.ttb.service.PortfolioService;
import ru.unclesema.ttb.service.PriceService;
import ru.unclesema.ttb.service.UserService;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.Strategy;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FrontService {
    private static final Instant appStartTime = Instant.now();
    private final List<Strategy> availableStrategies;
    private final List<CandleStrategy> availableCandleStrategies;
    private final ApplicationService applicationService;
    private final PriceService priceService;
    private final UserService userService;
    private final InvestClient investClient;
    private final PortfolioService portfolioService;

    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    public User findUser(String accountId) {
        return userService.findUserByAccountId(accountId);
    }

    public String getInstrumentName(User user, String figi) {
        return getInstrument(user, figi).getName();
    }

    public Instrument getInstrument(User user, String figi) {
        return investClient.getInstrument(user, figi);
    }


    public String lastPriceToString(User user, BigDecimal quantity, String figi) {
        String currency = getInstrument(user, figi).getCurrency();
        BigDecimal lastPrice = priceService.getLastPrice(user, figi);
        return lastPrice.multiply(quantity).doubleValue() + " " + currency.toUpperCase();
    }

    public String moneyValueToString(User user, String figi, MoneyValue value) {
        Instrument instrument = getInstrument(user, figi);
        return Utility.toBigDecimal(value).doubleValue() + " " + instrument.getCurrency().toUpperCase();
    }

    public StrategyStatement getStatement(User user) {
        Map<String, BigDecimal> benefitByCurrency = new HashMap<>();
        for (var entry : getRemainingInstruments(user).entrySet()) {
            var instrument = investClient.getInstrument(user, entry.getKey());
            var amount = entry.getValue();
            var benefit = benefitByCurrency.getOrDefault(instrument.getCurrency(), BigDecimal.ZERO);
            benefit = benefit.add(priceService.getLastPrice(user, instrument.getFigi()).multiply(BigDecimal.valueOf(amount)));
            benefitByCurrency.put(instrument.getCurrency(), benefit);
        }
        var operations = getOperations(user);
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

    public String printBenefits(User user) {
        StrategyStatement statement = getStatement(user);
        Map<String, BigDecimal> benefitByCurrency = statement.benefitByCurrency();
        if (benefitByCurrency.isEmpty()) {
            return "пока ничего :(";
        }
        return benefitByCurrency.entrySet().stream().map(entry -> entry.getValue().doubleValue() + " " + entry.getKey().toUpperCase()).collect(Collectors.joining(", "));
    }

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

    public boolean isActive(User user) {
        return userService.isActive(user);
    }

    public List<Strategy> getAvailableStrategies() {
        return availableStrategies;
    }

    public List<CandleStrategy> getAvailableCandleStrategies() {
        return availableCandleStrategies;
    }

    public List<Operation> getOperations(User user) {
        return investClient.getOperations(user, appStartTime, Instant.now()).join();
    }

    public BigDecimal getBalance(User user) {
        return priceService.getBalance(user);
    }

    public LocalDateTime getDate(Operation op) {
        return LocalDateTime.ofInstant(Utility.toInstant(op.getDate()), ZoneId.systemDefault());
    }

    public Map<String, Long> getRemainingInstruments(User user) {
        List<Operation> operations = getOperations(user);
        Map<String, Long> remainingInstruments = new HashMap<>();
        for (Operation op : operations) {
            if (op.getInstrumentType().equalsIgnoreCase("currency")) continue;
            if (op.getOperationType() == OperationType.OPERATION_TYPE_BUY) {
                remainingInstruments.merge(op.getFigi(), op.getQuantity(), Long::sum);
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_SELL) {
                Long cur = remainingInstruments.getOrDefault(op.getFigi(), 0L);
                if (cur == op.getQuantity()) {
                    remainingInstruments.remove(op.getFigi());
                } else {
                    remainingInstruments.put(op.getFigi(), cur - op.getQuantity());
                }
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_BROKER_FEE) {

            } else {
                log.error("Неизвестный тип операции: {}", op);
            }
        }
        return remainingInstruments;
//        return portfolioService.getRemaining(user);
    }

    public boolean contains(String accountId) {
        return userService.contains(accountId);
    }
}
