package ru.unclesema.ttb.controller;

import jsat.utils.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Position;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.UserMode;
import ru.unclesema.ttb.client.InvestClient;
import ru.unclesema.ttb.service.ApplicationService;
import ru.unclesema.ttb.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ApplicationController {
    private final InvestClient investClient;
    private final ApplicationService service;
    private final UserService userService;

//    @PostMapping("/simulate")
//    public String simulate(@ModelAttribute SimulationRequest request, Model model) {
//        SimulationResult result = service.simulate(request.getFigiList(), request.getFrom().atStartOfDay(), request.getTo().atStartOfDay());
//        model.addAttribute("result", result);
//        System.err.println(result);
//        return "result";
//    }


    @GetMapping("/")
    public String index(Model model) {
        List<User> users = userService.getAllUsers();
        Map<User, Portfolio> portfolioByUser = users
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        user -> user,
                        investClient::getPortfolio));
        Map<String, String> nameByFigi = portfolioByUser.values()
                .stream()
                .flatMap(portfolio -> portfolio.getPositions().stream().map(Position::getFigi))
                .distinct()
                .collect(Collectors.toUnmodifiableMap(
                        figi -> figi,
                        investClient::getNameByFigi));
        model.addAttribute("users", users);
        model.addAttribute("portfolioByUser", portfolioByUser);
        model.addAttribute("nameByFigi", nameByFigi);
        model.addAttribute("new-user", new User("", UserMode.SANDBOX, "", List.of(), null));
        return "index";
    }

    @PostMapping("user")
    public String addNewUser(User user) {
        service.addNewUser(user);
        return "redirect:/";
    }

    @GetMapping("figis")
    @ResponseBody
    public List<Pair<String, String>> figis(@RequestBody User user) {
        return service.figis(user).stream().map(e -> new Pair<>(e.getName(), e.getFigi())).toList();
    }
}
