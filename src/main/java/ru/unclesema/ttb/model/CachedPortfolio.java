package ru.unclesema.ttb.model;

import lombok.Builder;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.PortfolioResponse;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.unclesema.ttb.utility.Utility;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CachedPortfolio(BigDecimal expectedYield, Money totalAmountCurrencies, List<CachedPosition> positions) {
    public static CachedPortfolio of(Portfolio portfolio) {
        return CachedPortfolio
                .builder()
                .expectedYield(portfolio.getExpectedYield())
                .totalAmountCurrencies(portfolio.getTotalAmountCurrencies())
                .positions(portfolio.getPositions().stream().map(CachedPosition::of).toList())
                .build();
    }

    public static CachedPortfolio of(PortfolioResponse portfolio) {
        return CachedPortfolio
                .builder()
                .expectedYield(Utility.toBigDecimal(portfolio.getExpectedYield()))
                .totalAmountCurrencies(Money.fromResponse(portfolio.getTotalAmountCurrencies()))
                .positions(portfolio.getPositionsList().stream().map(CachedPosition::of).toList())
                .build();
    }
}
