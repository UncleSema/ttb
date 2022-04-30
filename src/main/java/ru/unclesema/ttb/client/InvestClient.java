package ru.unclesema.ttb.client;

import java.math.BigDecimal;

public interface InvestClient {
    boolean buy(String figi, BigDecimal price);

    boolean sell(String figi, BigDecimal price);
}
