package ru.unclesema.ttb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.unclesema.ttb.model.NewUserRequest;
import ru.unclesema.ttb.model.StartupRequest;
import ru.unclesema.ttb.model.User;
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

    /**
     * Обработка запроса о создании пользователя при старте (читайте подробнее в README)
     */
    @PostConstruct
    public void startup() {
        log.info("Запрос полученный при старте {}", startupRequest);
        try {
            NewUserRequest request = new NewUserRequest(startupRequest.getToken(), startupRequest.getMode(), startupRequest.getMaxBalance(), startupRequest.getAccountId(), startupRequest.getFigis(), startupRequest.getStrategyParameters());
            User user = service.addNewUser(request);
            if (Boolean.TRUE.equals(startupRequest.getStrategyEnable())) {
                service.enableStrategyForUser(user.accountId());
            }
        } catch (Exception e) {
            log.info("Не получилось создать пользователя при старте: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
