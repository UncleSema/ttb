package ru.unclesema.ttb;

import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;
import ru.tinkoff.piapi.core.stream.StreamProcessor;
import ru.unclesema.ttb.service.ApplicationService;

public record TradesSubscriber(ApplicationService service) implements StreamProcessor<TradesStreamResponse> {
    @Override
    public void process(TradesStreamResponse response) {
        if (response.hasOrderTrades()) {
            service.addOrderTrades(response.getOrderTrades());
        }
    }
}
