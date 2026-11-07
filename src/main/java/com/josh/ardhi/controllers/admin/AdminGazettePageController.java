package com.josh.ardhi.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/gazette-page")
public class AdminGazettePageController {

    @GetMapping("/show/{id}/overview")
    public String overview(){
        return "";
    }
}
