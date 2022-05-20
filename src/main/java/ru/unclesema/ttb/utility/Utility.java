package ru.unclesema.ttb.utility;

import com.google.protobuf.Timestamp;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;
import java.time.Instant;

public class Utility {
    private Utility() {
        throw new IllegalStateException("Утилитный класс!");
    }

    public static Quotation toQuotation(BigDecimal value) {
        return Quotation.newBuilder()
                .setUnits(value.longValue())
                .setNano(value.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).intValue())
                .build();
    }

    public static BigDecimal toBigDecimal(Quotation quotation) {
        return toBigDecimal(quotation.getUnits(), quotation.getNano());
    }

    public static BigDecimal toBigDecimal(MoneyValue value) {
        return toBigDecimal(value.getUnits(), value.getNano());
    }

    public static BigDecimal toBigDecimal(long units, int nanos) {
        return units == 0 && nanos == 0 ?
                BigDecimal.ZERO :
                BigDecimal.valueOf(units).add(BigDecimal.valueOf(nanos, 9));

    }

    public static Instant toInstant(Timestamp ts) {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }
}
