package ru.unclesema.ttb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.unclesema.ttb.NewUserRequest;
import ru.unclesema.ttb.service.ApplicationService;
import ru.unclesema.ttb.service.front.FrontService;

@Controller
@RequiredArgsConstructor
public class ApplicationController {
    private final FrontService frontService;
    private final ApplicationService service;
//    @PostMapping("/simulate")
//    public String simulate(@ModelAttribute SimulationRequest request, Model model) {
//        SimulationResult result = service.simulate(request.getFigiList(), request.getFrom().atStartOfDay(), request.getTo().atStartOfDay());
//        model.addAttribute("result", result);
//        System.err.println(result);
//        return "result";
//    }

    @GetMapping("/")
    public ModelAndView index(ModelMap model) {
//        try {
        model.addAttribute("newUserRequest", new NewUserRequest());
        model.addAttribute("frontService", frontService);
        return new ModelAndView("index", model);
//        } catch (Exception e) {
//            model.addAttribute(e);
//            return new ModelAndView("redirect:/app-error", model);
//        }
    }

    @PostMapping("/user")
    public ModelAndView addNewUser(ModelMap model, NewUserRequest request) {
//        try {
        service.addNewUser(request);
//        } catch (Exception e) {
//            System.err.println(e);
//            model.addAttribute(e);
//            return new ModelAndView("redirect:/app-error", model);
//        }
        return new ModelAndView("redirect:/");
    }

    @PostMapping("/strategy/enable")
    public String enableStrategy(String accountId) {
        service.enableStrategyForUser(accountId);
        return "redirect:/";
    }

    @PostMapping("/strategy/disable")
    public String disableStrategy(Integer userHash) {
        service.disableStrategyForUser(userHash);
        return "redirect:/";
    }

    @PostMapping("/sell-all")
    public String sellAll(String accountId) {
        service.sellAll(accountId);
        return "redirect:/";
    }

    @GetMapping("/app-error")
    public String exceptionHandler(Model model, Exception e) {
        model.addAttribute("msg", e.getMessage());
        return "error-page";
    }

}
