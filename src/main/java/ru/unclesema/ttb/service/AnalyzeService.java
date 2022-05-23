package ru.unclesema.ttb.service;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyzeService {
    private final Map<User, List<Operation>> operations = new HashMap<>();
    private final Map<User, Map<String, LastPrice>> lastPriceByUser = new HashMap<>();
    private final Map<User, Timestamp> timeByUser = new HashMap<>();

    public void processOrder(User user, String figi, long quantity, BigDecimal price, OrderDirection direction) {
        OperationType operationType = (direction == OrderDirection.ORDER_DIRECTION_BUY ? OperationType.OPERATION_TYPE_BUY : OperationType.OPERATION_TYPE_SELL);
        if (!operations.containsKey(user)) {
            operations.put(user, new ArrayList<>());
        }
        operations.get(user).add(Operation.newBuilder()
                .setOperationType(operationType)
                .setFigi(figi)
                .setQuantity(quantity)
                .setDate(timeByUser.getOrDefault(user, Timestamp.getDefaultInstance()))
                .setPrice(Utility.toMoneyValue(price))
                .setPayment(Utility.toMoneyValue(price))
                .build()
        );
    }

    public List<Operation> getOperations(User user) {
        return operations.getOrDefault(user, List.of());
    }

    public LastPrice getLastPrice(User user, String figi) {
        return lastPriceByUser
                .getOrDefault(user, Map.of())
                .getOrDefault(figi, LastPrice.getDefaultInstance());
    }

    public void addLastPrice(User user, LastPrice lastPrice) {
        if (!lastPriceByUser.containsKey(user)) {
            lastPriceByUser.put(user, new HashMap<>());
        }
        lastPriceByUser.get(user).put(lastPrice.getFigi(), lastPrice);
    }

    public void addTimestampForUser(User user, Timestamp timestamp) {
        timeByUser.put(user, timestamp);
    }

    public void processNewCandle(User user, Candle candle) {
        addLastPrice(user, Utility.toLastPrice(candle));
        addTimestampForUser(user, candle.getTime());
    }
}
