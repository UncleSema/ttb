package ru.unclesema.ttb.utility;

import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;

public class Utility {
    private Utility() {
        throw new IllegalStateException("Утилитный класс!");
    }

    public static Quotation toQuotation(BigDecimal num) {
        return Quotation.newBuilder()
                .setUnits(num.longValue())
                .setNano(num.remainder(BigDecimal.ONE).movePointRight(num.scale()).abs().intValue())
                .build();
    }

    public static BigDecimal toBigDecimal(Quotation quotation) {
        BigDecimal nanos = BigDecimal.valueOf(quotation.getNano());
        return BigDecimal.valueOf(quotation.getUnits()).add(nanos.movePointLeft(nanos.precision()));
    }
}
