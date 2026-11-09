package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.forms.GazetteUploadForm;
import com.josh.ardhi.services.GazetteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final GazetteService gazetteService;

    public AdminDashboardController(GazetteService gazetteService) {
        this.gazetteService = gazetteService;
    }

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("uploadForm", new GazetteUploadForm());
        model.addAttribute("gazetteIssues", gazetteService.getRecentIssues());
        return "admin/dashboard";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "dashboard";
    }
}
