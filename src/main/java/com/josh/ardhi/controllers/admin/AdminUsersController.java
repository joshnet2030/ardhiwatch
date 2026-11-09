package com.josh.ardhi.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

    @GetMapping("")
    public String list(){
        return "admin/users/list";
    }

    @GetMapping("/show/{id}")
    public String overview(Model model){
        model.addAttribute("currentTab", "overview");
        return "admin/users/tabs/overview";
    }

    @GetMapping("/show/{id}/activities")
    public String activities(Model model){
        model.addAttribute("currentTab", "activities");
        return "admin/users/tabs/activities";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu(){
        return "users";
    }
}
