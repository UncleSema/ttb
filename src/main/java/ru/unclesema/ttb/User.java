package ru.unclesema.ttb;

import ru.tinkoff.piapi.core.InvestApi;
import ru.unclesema.ttb.strategy.Strategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class User {
    private final String token;
    private final UserMode mode;
    private final BigDecimal maxBalance;
    private final String accountId;
    private final List<String> figis;
    private final Strategy strategy;
    private final InvestApi api;

    private static final String APP_NAME = "ru.unclesema.ttb";

    public User(String token, UserMode mode, BigDecimal maxBalance, String accountId, List<String> figis,
                Strategy strategy) {
        this.token = token;
        this.mode = mode;
        this.maxBalance = maxBalance;
        this.figis = figis;
        this.strategy = strategy;
        if (mode == UserMode.SANDBOX) {
            this.api = InvestApi.createSandbox(token, APP_NAME);
            this.accountId = api.getSandboxService().openAccountSync();
        } else {
            this.api = InvestApi.create(token, APP_NAME);
            this.accountId = accountId;
        }
    }

    public String token() {
        return token;
    }

    public UserMode mode() {
        return mode;
    }

    public BigDecimal maxBalance() {
        return maxBalance;
    }

    public String accountId() {
        return accountId;
    }

    public List<String> figis() {
        return figis;
    }

    public Strategy strategy() {
        return strategy;
    }

    public InvestApi api() {
        return api;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (User) obj;
        return Objects.equals(this.token, that.token) &&
                Objects.equals(this.mode, that.mode) &&
                Objects.equals(this.maxBalance, that.maxBalance) &&
                Objects.equals(this.accountId, that.accountId) &&
                Objects.equals(this.figis, that.figis) &&
                Objects.equals(this.strategy, that.strategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, mode, maxBalance, accountId, figis, strategy);
    }

    @Override
    public String toString() {
        return "User[" +
                "mode=" + mode + ", " +
                "maxBalance=" + maxBalance + ", " +
                "accountId=" + accountId + ", " +
                "figis=" + figis + "]";
    }

}
