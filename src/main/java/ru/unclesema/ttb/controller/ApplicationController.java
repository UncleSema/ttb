package ru.unclesema.ttb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.unclesema.ttb.model.NewUserRequest;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.service.ApplicationService;
import ru.unclesema.ttb.service.front.FrontService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Controller
@RequiredArgsConstructor
public class ApplicationController {
    private final FrontService frontService;
    private final ApplicationService service;

    @GetMapping("/")
    public ModelAndView index(ModelMap model) {
        model.addAttribute("newUserRequest", new NewUserRequest());
        model.addAttribute("frontService", frontService);
        return new ModelAndView("add", model);
    }

    @GetMapping("/{accountId}")
    public ModelAndView index(@PathVariable String accountId, ModelMap model) {
        if (frontService.contains(accountId)) {
            model.addAttribute("frontService", frontService);
            model.addAttribute(frontService.findUser(accountId));
            return new ModelAndView("index", model);
        }
        return new ModelAndView("redirect:/", model);
    }

    @PostMapping("/simulate")
    public String simulate(String accountId, LocalDateTime from, LocalDateTime to, CandleInterval interval) {
        service.simulate(frontService.findUser(accountId), from.toInstant(ZoneOffset.UTC), to.toInstant(ZoneOffset.UTC), interval);
        return "redirect:/" + accountId;
    }

    @PostMapping("/user")
    public String addNewUser(NewUserRequest request) {
        User user = service.addNewUser(request);
        return "redirect:/" + user.accountId();
    }

    @PostMapping("/strategy/enable")
    public String enableStrategy(String accountId) {
        service.enableStrategyForUser(accountId);
        return "redirect:/" + accountId;
    }

    @PostMapping("/strategy/disable")
    public String disableStrategy(String accountId) {
        service.disableStrategyForUser(accountId);
        return "redirect:/" + accountId;
    }

    @PostMapping("/sell-all")
    public String sellAll(String accountId) {
        service.sellAll(accountId);
        return "redirect:/" + accountId;
    }
}
