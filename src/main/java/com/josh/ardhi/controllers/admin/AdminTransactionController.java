package com.josh.ardhi.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/transactions")
public class AdminTransactionController {

    @GetMapping("")
    public String list(){
        return "admin/transactions/list";
    }

    @GetMapping("/show/{id}")
    public String overview(){
        return "admin/transactions/tabs/overview";
    }

    @ModelAttribute("currentMenu")
    private String currentMenu(){
        return "transactions";
    }
}
