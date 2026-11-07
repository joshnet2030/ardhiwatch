package com.josh.ardhi.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/subscriptions")
public class AdminSubscriptionController {

    @GetMapping("")
    public String list(){
        return "admin/subscriptions/list";
    }

    @GetMapping("/show/{id}")
    public String overview(Model model){
        model.addAttribute("currentTab", "overview");
        return "admin/subscriptions/tabs/overview";
    }

    @GetMapping("/show/{id}/transactions")
    public String transactions(Model model){
        model.addAttribute("currentTab", "transactions");
        return "admin/subscriptions/tabs/transactions";
    }

    @GetMapping("/show/{id}/clients")
    public String clients(Model model){
        model.addAttribute("currentTab", "clients");
        return "admin/subscriptions/tabs/clients";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu(){
        return "subscriptions";
    }
}
