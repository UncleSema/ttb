package ru.unclesema.ttb.model;

import lombok.Builder;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.PortfolioPosition;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Position;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;

@Builder
public record CachedPosition(String figi, BigDecimal quantity, BigDecimal currentPrice, String currency) {

    public static CachedPosition of(Position position) {
        Money currentPrice = position.getCurrentPrice();
        return CachedPosition
                .builder()
                .figi(position.getFigi())
                .quantity(position.getQuantity())
                .currentPrice(currentPrice.getValue())
                .currency(currentPrice.getCurrency().getCurrencyCode())
                .build();
    }

    public static CachedPosition of(PortfolioPosition position) {
        MoneyValue currentPrice = position.getCurrentPrice();
        return CachedPosition
                .builder()
                .figi(position.getFigi())
                .quantity(Utility.toBigDecimal(position.getQuantity()))
                .currentPrice(Utility.toBigDecimal(currentPrice.getUnits(), currentPrice.getNano()))
                .currency(currentPrice.getCurrency())
                .build();
    }
}
