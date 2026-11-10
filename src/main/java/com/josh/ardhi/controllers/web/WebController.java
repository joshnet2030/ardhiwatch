package com.josh.ardhi.controllers.web;

import com.josh.ardhi.forms.ContactMessageForm;
import com.josh.ardhi.services.ContactMessageService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Set;

@Controller
public class WebController {

    private final ContactMessageService contactMessageService;

    public WebController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    @ModelAttribute("dashboardUrl")
    public String populateDashboardUrl() {
        return resolveDashboardPath();
    }

    @GetMapping("")
    public String index() {
        String dashboardPath = resolveDashboardPath();
        if (dashboardPath != null) {
            return "redirect:" + dashboardPath;
        }
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

    private String resolveDashboardPath() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        if (roles.contains("ROLE_ADMIN")) {
            return "/admin";
        }
        if (roles.contains("ROLE_CLIENT")) {
            return "/client";
        }
        return null;
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
