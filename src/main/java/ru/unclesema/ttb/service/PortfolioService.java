package ru.unclesema.ttb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderTrade;
import ru.tinkoff.piapi.contract.v1.OrderTrades;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final Map<User, Map<String, Long>> remaining = new HashMap<>();
    private final Map<User, BigDecimal> spent = new HashMap<>();
    private final Map<User, Queue<BigDecimal>> orderQueue = new HashMap<>();
    private final PriceService priceService;

    public void addOrderTrades(User user, OrderTrades orderTrades) {
        if (!remaining.containsKey(user)) {
            remaining.put(user, new HashMap<>());
        }
        var quantityByFigi = remaining.get(user);
        for (OrderTrade trade : orderTrades.getTradesList()) {
            if (orderTrades.getDirection() == OrderDirection.ORDER_DIRECTION_BUY) {
                BigDecimal price = Utility.toBigDecimal(trade.getPrice())
                        .multiply(BigDecimal.valueOf(trade.getQuantity()));
                BigDecimal priceInRubles = priceService.getPriceInRubles(user, price, orderTrades.getFigi());
                quantityByFigi.merge(orderTrades.getFigi(), trade.getQuantity(), Long::sum);
                spent.merge(user, priceInRubles, BigDecimal::add);
                if (!orderQueue.containsKey(user)) {
                    orderQueue.put(user, new ArrayDeque<>());
                }
                orderQueue.get(user).add(priceInRubles);
            } else {
                quantityByFigi.merge(orderTrades.getFigi(), trade.getQuantity(), (a, b) -> a - b);
                BigDecimal price = orderQueue.get(user).poll();
                spent.merge(user, price, BigDecimal::subtract);
            }
        }
        if (quantityByFigi.get(orderTrades.getFigi()) == 0) {
            quantityByFigi.remove(orderTrades.getFigi());
        }
    }

    public Map<String, Long> getRemaining(User user) {
        return remaining.getOrDefault(user, Map.of());
    }

    public BigDecimal getSpent(User user) {
        return spent.getOrDefault(user, BigDecimal.ZERO);
    }
}
