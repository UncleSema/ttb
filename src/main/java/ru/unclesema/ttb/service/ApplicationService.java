package ru.unclesema.ttb.service;

import ru.tinkoff.piapi.contract.v1.Share;
import ru.unclesema.ttb.ApplicationConfig;
import ru.unclesema.ttb.client.InvestClient;

import java.util.List;

public class ApplicationService {
    private final InvestClient client;
    private final ApplicationConfig config;

    public ApplicationService(InvestClient client, ApplicationConfig config) {
        this.client = client;
        this.config = config;
    }

    public List<Share> getShares() {
        return client.getAllShares();
    }
}
