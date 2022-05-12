package ru.unclesema.ttb;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class NewUserRequest {
    private String token;
    private UserMode mode;
    private BigDecimal maxBalance;
    private String accountId;
    private List<String> figis;
    private Map<String, String> strategyParameters;
}
