package ru.unclesema.ttb.model;

import com.google.protobuf.Timestamp;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.Quotation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record CachedCandle(LocalDateTime ts,
                           BigDecimal closePrice) implements Comparable<CachedCandle> {
    public static CachedCandle of(HistoricCandle candle) {
        Timestamp ts = candle.getTime();
        LocalDateTime time = LocalDateTime.ofEpochSecond(ts.getSeconds(), ts.getNanos(), ZoneOffset.UTC);
        return new CachedCandle(time, quotationToBigDecimal(candle.getClose()));
    }

    private static BigDecimal quotationToBigDecimal(Quotation quotation) {
        return BigDecimal.valueOf(quotation.getUnits() + ((double) quotation.getNano()) / 1_000_000_000);
    }

    @Override
    public int compareTo(CachedCandle cachedCandle) {
        return ts.compareTo(cachedCandle.ts);
    }
}
