package com.josh.ardhi.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/clients")
public class AdminClientController {

    @GetMapping("")
    public String list(){
        return "admin/clients/list";
    }

    @GetMapping("/show/{id}")
    public String overview(Model model){
        model.addAttribute("currentTab", "overview");
        return "admin/clients/tabs/overview";
    }

    @GetMapping("/show/{id}/keywords")
    public String keywords(Model model){
        model.addAttribute("currentTab", "keywords");
        return "admin/clients/tabs/keywords";
    }

    @GetMapping("/show/{id}/matches")
    public String matches(Model model){
        model.addAttribute("currentTab", "matches");
        return "admin/clients/tabs/matches";
    }

    @GetMapping("/show/{id}/notifications")
    public String notifications(Model model){
        model.addAttribute("currentTab", "notifications");
        return "admin/clients/tabs/notifications";
    }

    @GetMapping("/show/{id}/subscriptions")
    public String subscriptions(Model model){
        model.addAttribute("currentTab", "subscriptions");
        return "admin/clients/tabs/subscriptions";
    }

    @GetMapping("/show/{id}/transactions")
    public String transactions(Model model){
        model.addAttribute("currentTab", "transactions");
        return "admin/clients/tabs/transactions";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu(){
        return "clients";
    }
}
