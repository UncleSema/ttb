package ru.unclesema.ttb.model;

import ru.unclesema.ttb.strategy.Strategy;

import java.math.BigDecimal;
import java.util.List;

public record User(String token, UserMode mode, BigDecimal maxBalance, String accountId, List<String> figis,
                   Strategy strategy) {

    @Override
    public String toString() {
        return "User[" +
                "mode=" + mode + ", " +
                "maxBalance=" + maxBalance + ", " +
                "accountId=" + accountId + ", " +
                "figis=" + figis + "]";
    }
}
