package ru.unclesema.ttb;

import lombok.extern.slf4j.Slf4j;
import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.unclesema.ttb.service.ApplicationService;

@Slf4j
public record OrderBookSubscriber(ApplicationService service,
                                  User user) implements StreamProcessor<MarketDataResponse> {
    @Override
    public void process(MarketDataResponse response) {
        if (response.hasOrderbook()) {
            log.info(String.valueOf(response.getOrderbook()));
            service.addNewOrderBook(this, response.getOrderbook());
        }
        if (response.hasLastPrice()) {
            log.info(String.valueOf(response.getLastPrice()));
            service.addLastPrice(response.getLastPrice());
        }
    }
}
