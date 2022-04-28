package ru.unclesema.ttb.client;

import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;
import ru.unclesema.ttb.ApplicationConfig;

import java.util.List;

public class InvestClient {
    private InvestApi api;
    private final ApplicationConfig config;

    public InvestClient(ApplicationConfig config) {
        this.api = InvestApi.createSandbox(config.getToken());

        this.config = config;
    }

    public List<Share> getAllShares() {
        List<Share> shares = List.of();
        try {
            shares = api.getInstrumentsService().getAllSharesSync();
        } catch (RuntimeException e) {
            System.err.println("Ошибка подключения к Tinkoff Invest. Вы верно ввели токен?");
        }
        return shares;
    }
}
