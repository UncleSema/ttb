package ru.unclesema.ttb.utility;

import com.google.protobuf.Timestamp;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    public static MoneyValue toMoneyValue(BigDecimal value) {
        Quotation quotation = toQuotation(value);
        return MoneyValue.newBuilder()
                .setUnits(quotation.getUnits())
                .setNano(quotation.getNano())
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

    /**
     * Метод находит самую далёкую дату <code>to</code>, такую что промужуток с <code>from</code> по <code>to</code>
     * возможно запросить для получения исторических свечей.
     */
    public static Instant instantPlusCandleInterval(Instant from, CandleInterval interval) {
        return switch (interval) {
            case CANDLE_INTERVAL_1_MIN, CANDLE_INTERVAL_15_MIN, CANDLE_INTERVAL_5_MIN -> from.plus(1, ChronoUnit.DAYS);
            case CANDLE_INTERVAL_HOUR -> from.plus(7, ChronoUnit.DAYS);
            case CANDLE_INTERVAL_DAY -> from.plus(365, ChronoUnit.DAYS);
            default -> null;
        };
    }

    public static Candle toCandle(String figi, HistoricCandle candle) {
        return Candle.newBuilder()
                .setClose(candle.getClose())
                .setFigi(figi)
                .setHigh(candle.getHigh())
                .setClose(candle.getClose())
                .setTime(candle.getTime())
                .build();
    }

    public static LastPrice toLastPrice(Candle candle) {
        return LastPrice.newBuilder()
                .setPrice(candle.getClose())
                .setFigi(candle.getFigi())
                .setTime(candle.getTime())
                .build();
    }

    public static boolean checkExceptionCode(ApiRuntimeException exception, String code) {
        return exception.getMessage().startsWith(code);
    }
}
