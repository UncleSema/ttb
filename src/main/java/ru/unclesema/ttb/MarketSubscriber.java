package ru.unclesema.ttb;

import lombok.extern.slf4j.Slf4j;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.unclesema.ttb.service.ApplicationService;

@Slf4j
public record MarketSubscriber(ApplicationService service) implements StreamProcessor<MarketDataResponse> {
    @Override
    public void process(MarketDataResponse response) {
        if (response.hasOrderbook()) {
            log.debug(String.valueOf(response.getOrderbook()));
            service.addOrderBook(response.getOrderbook());
        }
        if (response.hasLastPrice()) {
            log.debug(String.valueOf(response.getLastPrice()));
            service.addLastPrice(response.getLastPrice());
        }
        if (response.hasCandle()) {
            log.debug(String.valueOf(response.getCandle()));
            service.addCandle(response.getCandle());
        }
    }
}
