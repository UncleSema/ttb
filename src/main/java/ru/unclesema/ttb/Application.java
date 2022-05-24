package ru.unclesema.ttb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class Application {
    private final StartupRequest startupRequest;
    private final ApplicationService service;

    /**
     * Обработка запроса о создании пользователя при старте
     *
     * <p>Почитать поподробнее можно <a href="https://github.com/UncleSema/ttb/#%D1%83%D0%B4%D0%BE%D0%B1%D0%BD%D1%8B%D0%B9-%D1%81%D0%BF%D0%BE%D1%81%D0%BE%D0%B1-%D1%81%D0%BE%D0%B7%D0%B4%D0%B0%D0%BD%D0%B8%D1%8F-%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D1%82%D0%B5%D0%BB%D1%8F-%D0%BF%D1%80%D0%B8-%D1%81%D1%82%D0%B0%D1%80%D1%82%D0%B5">тут</a></p>
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
