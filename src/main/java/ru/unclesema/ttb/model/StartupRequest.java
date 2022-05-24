package ru.unclesema.ttb.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "user")
@Component
@Getter
@Setter
@ToString
public class StartupRequest {
    private String token;
    private UserMode mode;
    private BigDecimal maxBalance;
    private String accountId;
    private List<String> figis;
    private Map<String, String> strategyParameters;
    private Boolean strategyEnable;
}
