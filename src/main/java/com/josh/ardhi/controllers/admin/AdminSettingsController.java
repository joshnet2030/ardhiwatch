package com.josh.ardhi.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    @GetMapping("")
    public String settings(){
        return "/admin/settings";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu(){
        return "settings";
    }
}

