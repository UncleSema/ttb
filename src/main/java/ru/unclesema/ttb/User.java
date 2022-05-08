package ru.unclesema.ttb;

import lombok.Builder;
import ru.unclesema.ttb.strategy.Strategy;

import java.util.List;

@Builder(builderMethodName = "defaultBuilder")
public record User(String token, UserMode mode, String accountId, List<String> figis, Strategy strategy) {
    public static UserBuilder builder(String token, UserMode mode) {
        return defaultBuilder().token(token).mode(mode);
    }
}
