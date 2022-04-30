package ru.unclesema.ttb.client;

import java.math.BigDecimal;

public class MarketInvestClient implements InvestClient {

    @Override
    public boolean buy(String figi, BigDecimal price) {
        return false;
    }

    @Override
    public boolean sell(String figi, BigDecimal price) {
        return false;
    }
}
