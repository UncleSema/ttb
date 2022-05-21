package ru.unclesema.ttb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.unclesema.ttb.NewUserRequest;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.service.ApplicationService;
import ru.unclesema.ttb.service.front.FrontService;

@Controller
@RequiredArgsConstructor
public class ApplicationController {
    private final FrontService frontService;
    private final ApplicationService service;

    @GetMapping("/")
    public ModelAndView index(ModelMap model) {
//        try {
        model.addAttribute("newUserRequest", new NewUserRequest());
        model.addAttribute("frontService", frontService);
        return new ModelAndView("add", model);
//        } catch (Exception e) {
//            model.addAttribute(e);
//            return new ModelAndView("redirect:/app-error", model);
//        }
    }

    @GetMapping("/{accountId}")
    public ModelAndView index(@PathVariable String accountId, ModelMap model) {
//        try {
        if (frontService.contains(accountId)) {
            model.addAttribute("frontService", frontService);
            model.addAttribute(frontService.findUser(accountId));
            return new ModelAndView("index", model);
        }
        return new ModelAndView("redirect:/", model);
//        } catch (Exception e) {
//            model.addAttribute(e);
//            return new ModelAndView("redirect:/app-error", model);
//        }
    }

    @PostMapping("/user")
    public String addNewUser(NewUserRequest request) {
//        try {
        User user = service.addNewUser(request);
//        } catch (Exception e) {
//            System.err.println(e);
//            model.addAttribute(e);
//            return new ModelAndView("redirect:/app-error", model);
//        }
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

    @GetMapping("/app-error")
    public String exceptionHandler(Model model, Exception e) {
        model.addAttribute("msg", e.getMessage());
        return "error-page";
    }
}
