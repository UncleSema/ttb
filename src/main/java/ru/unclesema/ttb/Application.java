package ru.unclesema.ttb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.unclesema.ttb.service.ApplicationService;

import javax.annotation.PostConstruct;

@EnableConfigurationProperties(value = StartupRequest.class)
@SpringBootApplication
@Slf4j
public class Application {

    @Autowired
    public StartupRequest startupRequest;

    @Autowired
    public ApplicationService service;

    @PostConstruct
    public void startup() {
        log.info("Запрос полученный при старте {}", startupRequest);
        if (startupRequest.isValid()) {
            NewUserRequest request = new NewUserRequest(startupRequest.getToken(), startupRequest.getMode(), startupRequest.getMaxBalance(), startupRequest.getAccountId(), startupRequest.getFigis(), startupRequest.getStrategyParameters());
            User user = service.addNewUser(request);
            if (Boolean.TRUE.equals(startupRequest.getStrategyEnable())) {
                service.enableStrategyForUser(user.accountId());
            }
        } else {
            log.info("Параметры запуска не найдены/не валидны, новый пользователь при запуске создан не будет");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
