package ru.unclesema.ttb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.unclesema.ttb.service.ApplicationService;

@Controller
public class ApplicationController {
    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        return service.getShares().toString();
    }
}
