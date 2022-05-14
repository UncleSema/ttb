package ru.unclesema.ttb.service.front;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.Instrument;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Operation;
import ru.tinkoff.piapi.contract.v1.OperationType;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.service.ApplicationService;
import ru.unclesema.ttb.service.PriceService;
import ru.unclesema.ttb.service.UserService;
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
    private final ApplicationService applicationService;
    private final PriceService priceService;
    private final UserService userService;
    private final InvestClient investClient;

    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    public String getInstrumentName(User user, String figi) {
        if (figi.equalsIgnoreCase("FG0000000000")) {
            return "Российский рубль";
        }
        return getInstrument(user, figi).getName();
    }

    public Instrument getInstrument(User user, String figi) {
        return investClient.getInstrument(user, figi);
    }

    public Map<Instrument, Long> getRemainingInstruments(User user) {
        List<Operation> operations = getOperations(user);
        Map<Instrument, Long> remainingInstruments = new HashMap<>();
        for (Operation op : operations) {
            if (op.getInstrumentType().equalsIgnoreCase("currency")) continue;
            Instrument instrument = getInstrument(user, op.getFigi());
            if (op.getOperationType() == OperationType.OPERATION_TYPE_BUY) {
                remainingInstruments.merge(instrument, op.getQuantity(), Long::sum);
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_SELL) {
                Long cur = remainingInstruments.getOrDefault(instrument, 0L);
                if (cur == op.getQuantity()) {
                    remainingInstruments.remove(instrument);
                } else {
                    remainingInstruments.put(instrument, cur - op.getQuantity());
                }
            } else {
                log.error("Неизвестный тип операции: {}", op);
            }
        }
        return remainingInstruments;
    }


    public String lastPriceToString(User user, BigDecimal quantity, String figi) {
        if (figi.equalsIgnoreCase("FG0000000000")) {
            return quantity.doubleValue() + " RUB";
        }
        String currency = getInstrument(user, figi).getCurrency();
        BigDecimal lastPrice = priceService.getLastPrice(user, figi);
        return lastPrice.multiply(quantity).doubleValue() + " " + currency.toUpperCase();
    }

    public String moneyValueToString(User user, String figi, MoneyValue value) {
        Instrument instrument = getInstrument(user, figi);
        return value.getUnits() + "." + value.getNano() / 1_000_000 + " " + instrument.getCurrency().toUpperCase();
    }

    public StrategyStatement getStatement(User user) {
        Map<String, BigDecimal> benefitByCurrency = new HashMap<>();
        for (Map.Entry<Instrument, Long> entry : getRemainingInstruments(user).entrySet()) {
            Instrument instrument = entry.getKey();
            Long amount = entry.getValue();
            BigDecimal benefit = benefitByCurrency.getOrDefault(instrument.getCurrency(), BigDecimal.ZERO);
            benefit = benefit.add(priceService.getLastPrice(user, instrument.getFigi()).multiply(BigDecimal.valueOf(amount)));
            benefitByCurrency.put(instrument.getCurrency(), benefit);
        }
        List<Operation> operations = getOperations(user);
        for (Operation op : operations) {
            if (op.getInstrumentType().equalsIgnoreCase("currency")) continue;
            Instrument instrument = getInstrument(user, op.getFigi());
            BigDecimal benefit = benefitByCurrency.getOrDefault(instrument.getCurrency(), BigDecimal.ZERO);
            BigDecimal payment = Utility.toBigDecimal(op.getPayment().getUnits(), op.getPayment().getNano());
            if (op.getOperationType() == OperationType.OPERATION_TYPE_SELL) {
                benefit = benefit.add(payment);
            } else if (op.getOperationType() == OperationType.OPERATION_TYPE_BUY) {
                benefit = benefit.subtract(payment);
            } else {
                log.error("Неизвестный тип операции: {}", op);
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
        return benefitByCurrency.entrySet()
                .stream()
                .map(entry -> entry.getValue().doubleValue() + " " + entry.getKey().toUpperCase())
                .collect(Collectors.joining(", "));
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
        return operationType.name();
    }

    public boolean isActive(User user) {
        return applicationService.isActive(user);
    }

    public List<Strategy> getAvailableStrategies() {
        return availableStrategies;
    }

    public List<Operation> getOperations(User user) {
        return investClient.getOperations(user, appStartTime, Instant.now()).join();
    }

    public LocalDateTime getDate(Operation op) {
        return LocalDateTime.ofInstant(Utility.toInstant(op.getDate()), ZoneId.systemDefault());
    }
}
