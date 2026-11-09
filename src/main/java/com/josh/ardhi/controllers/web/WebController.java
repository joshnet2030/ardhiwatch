package com.josh.ardhi.controllers.web;

import com.josh.ardhi.forms.ContactMessageForm;
import com.josh.ardhi.services.ContactMessageService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class WebController {

    private final ContactMessageService contactMessageService;

    public WebController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    @GetMapping("")
    public String index() {
        return "index";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "privacy_policy";
    }

    @GetMapping("/contact-us")
    public String contactUs(Model model) {
        model.addAttribute("contactMessageForm", new ContactMessageForm());
        return "contact-us";
    }

    @HxRequest
    @PostMapping("/process-contact-us")
    @Transactional
    public String processContactUs(
            @ModelAttribute("contactMessageForm") @Valid ContactMessageForm contactMessageForm,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "contact-us::contact-us-form";
        }

        contactMessageService.submit(contactMessageForm);

        model.addAttribute("success", true);
        return "contact-us::contact-us-form";
    }

}
