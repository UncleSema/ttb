package ru.unclesema.ttb.service.price;

import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.unclesema.ttb.model.User;

import java.math.BigDecimal;

record StopRequest(User user, String figi, BigDecimal takeProfit, BigDecimal stopLoss, OrderDirection direction) {
}
