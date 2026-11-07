package com.josh.ardhi.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("")
    public String index(){
        return "index";
    }


    @GetMapping("/terms")
    public String terms(){
        return "terms";
    }


    @GetMapping("/privacy-policy")
    public String privacyPolicy(){
        return "privacy_policy";
    }
}
