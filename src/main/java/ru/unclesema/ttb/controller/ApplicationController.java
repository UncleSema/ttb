package ru.unclesema.ttb.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.unclesema.ttb.analyze.SimulationRequest;
import ru.unclesema.ttb.analyze.SimulationResult;
import ru.unclesema.ttb.service.ApplicationService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService service;

    @PostMapping("/simulate")
    public String simulate(@ModelAttribute SimulationRequest request, Model model) {
        System.err.println(request);
        SimulationResult result = service.simulate(request.getFigiList(), request.getFrom().atStartOfDay().toInstant(ZoneOffset.UTC), request.getTo().atStartOfDay().toInstant(ZoneOffset.UTC));
        System.err.println(result);
        return "result";
    }

    @GetMapping("/figi")
    @ResponseBody
    public String figi() {
        return service.figi();
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("request", new SimulationRequest());
        return "index";
    }
}
