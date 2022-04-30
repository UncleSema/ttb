package ru.unclesema.ttb.client;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class SandboxInvestClient implements InvestClient {
    @Override
    public boolean buy(String figi, BigDecimal price) {
        return false;
    }

    @Override
    public boolean sell(String figi, BigDecimal price) {
        return false;
    }
}
