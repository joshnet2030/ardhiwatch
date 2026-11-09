package com.josh.ardhi.controllers.client;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/client")
public class ClientController {

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("currentMenu", "dashboard");
        return "client/dashboard";
    }

    @GetMapping("/keywords")
    public String keywords(Model model) {
        model.addAttribute("currentMenu", "keywords");
        return "client/keywords";
    }

    @GetMapping("/matches")
    public String matches(Model model) {
        model.addAttribute("currentMenu", "matches");
        return "client/matches/list";
    }

    @GetMapping("/matches/show/{id}")
    public String showMatch(@PathVariable String id, Model model) {
        model.addAttribute("currentMenu", "matches");
        model.addAttribute("matchId", id);
        return "client/matches/show";
    }

    @GetMapping("/subscription")
    public String subscription(Model model) {
        model.addAttribute("currentMenu", "subscription");
        return "client/subscription";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("currentMenu", "settings");
        return "client/settings";
    }
}
