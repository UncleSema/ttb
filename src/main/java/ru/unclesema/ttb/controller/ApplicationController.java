package ru.unclesema.ttb.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.unclesema.ttb.analyze.SimulationResult;
import ru.unclesema.ttb.service.ApplicationService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService service;

    @PostMapping("/simulate")
    public SimulationResult simulate(@RequestBody Request request) {
        return service.simulate(request.figiList(), request.from().toInstant(ZoneOffset.UTC), request.to().toInstant(ZoneOffset.UTC));
    }

    @GetMapping("/figi")
    public String figi() {
        return service.figi();
    }
}

record Request(List<String> figiList,
               @JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime from,
               @JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime to) {

}
