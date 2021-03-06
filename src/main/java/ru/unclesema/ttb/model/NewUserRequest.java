package ru.unclesema.ttb.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequest {
    private String token;
    private UserMode mode;
    private BigDecimal maxBalance;
    private String accountId;
    private List<String> figis;
    private Map<String, String> strategyParameters;
}
