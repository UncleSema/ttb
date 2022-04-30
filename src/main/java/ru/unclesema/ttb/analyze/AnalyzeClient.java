package ru.unclesema.ttb.analyze;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.unclesema.ttb.cache.InstrumentsCache;
import ru.unclesema.ttb.cache.PortfolioStorage;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.model.CachedInstrument;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AnalyzeClient implements InvestClient {

    @Getter
    private BigDecimal balance = BigDecimal.valueOf(15_000);
    private final InstrumentsCache instrumentsCache;
    private final PortfolioStorage portfolio;

    @Override
    public boolean buy(String figi, BigDecimal price) {
        CachedInstrument instrument = instrumentsCache.getInstrumentByFigi(figi);
        log.info("Поступил запрос на покупку {} за {}.", instrument, price);
        if (balance.compareTo(price) >= 0) {
            balance = balance.subtract(price);
            portfolio.add(figi);
            log.info("Покупка совершена. Новый баланс: {}", balance);
            return true;
        }
        log.info("Ошибка: нехватка средств. Текущий баланс: {}", balance);
        return false;
    }

    @Override
    public boolean sell(String figi, BigDecimal price) {
        CachedInstrument instrument = instrumentsCache.getInstrumentByFigi(figi);
        log.info("Поступил запрос на продажу {} за {}.", instrument, price);
        if (portfolio.contains(figi)) {
            balance = balance.add(price);
            portfolio.remove(figi);
            log.info("Продажа совершена. Новый баланс: {}", balance);
            return true;
        }
        log.info("Ошибка: нехватка инструмента в портфеле. Текущий баланс: {}", balance);
        return false;
    }

    public List<String> getFigi() {
        return portfolio.getAll();
    }
}
