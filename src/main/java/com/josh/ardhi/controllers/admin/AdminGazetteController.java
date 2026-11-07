package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.forms.GazetteUploadForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/gazettes")
public class AdminGazetteController {

    @GetMapping("")
    public String index(Model model){
        GazetteUploadForm uploadForm = new GazetteUploadForm();
        model.addAttribute("uploadForm", uploadForm);
        return "admin/gazettes/list";
    }

    @GetMapping("/show/{id}")
    public String overview(@PathVariable String id, Model model){
        model.addAttribute("tab", "overview");
        return "admin/gazettes/tabs/overview";
    }

    @GetMapping("/show/{id}/pages")
    public String gazetteViewPages(@PathVariable String id, Model model){
        model.addAttribute("tab", "pages");
        return "admin/gazettes/tabs/pages";
    }

    @GetMapping("/matches-review")
    public String matchesReview(){
        return "admin/gazettes/matches_review_list";
    }

    @GetMapping("/matches-review/show/id")
    public String matchesReviewRecord(){
        return "admin/gazettes/matches_review_record";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu(){
        return "gazettes";
    }
}
