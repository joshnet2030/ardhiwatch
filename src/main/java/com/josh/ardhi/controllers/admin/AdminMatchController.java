package com.josh.ardhi.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/matches")
public class AdminMatchController {

    @GetMapping()
    public String list(){
        return "admin/matches/list";
    }

    @GetMapping("/show/{id}")
    public String overview(Model model){
        model.addAttribute("currentTab", "overview");
        return "admin/matches/tabs/overview";
    }

    @GetMapping("/show/{id}/notifications")
    public String notifications(Model model){
        model.addAttribute("currentTab", "notifications");
        return "admin/matches/tabs/notifications";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu(){
        return "matches";
    }
}
