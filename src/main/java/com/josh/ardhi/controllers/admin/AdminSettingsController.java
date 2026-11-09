package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.AppSettingForm;
import com.josh.ardhi.models.AppSetting;
import com.josh.ardhi.services.AppSettingService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private static final int HISTORY_PAGE_SIZE = 10;

    private final AppSettingService appSettingService;

    public AdminSettingsController(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
    }

    @GetMapping("")
    public String settings(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        AppSetting activeSettings = appSettingService.getActiveSettings();
        Page<AppSetting> historyPage = appSettingService.getSettingsHistory(page, HISTORY_PAGE_SIZE);
        model.addAttribute("settingsForm", AppSettingForm.fromEntity(activeSettings));
        model.addAttribute("activeSettings", activeSettings);
        model.addAttribute("settingsHistoryPage", historyPage);
        model.addAttribute("settingsHistory", historyPage.getContent());
        return "admin/settings";
    }

    @HxRequest
    @GetMapping("/history")
    public String historyFragment(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        Page<AppSetting> historyPage = appSettingService.getSettingsHistory(page, HISTORY_PAGE_SIZE);
        model.addAttribute("settingsHistoryPage", historyPage);
        model.addAttribute("settingsHistory", historyPage.getContent());
        return "admin/settings :: historyCard";
    }

    @HxRequest
    @PostMapping("/general")
    public String saveGeneral(@Validated(AppSettingForm.GeneralGroup.class) @ModelAttribute("settingsForm") AppSettingForm settingsForm,
                              BindingResult bindingResult,
                              Model model,
                              HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "admin/settings :: generalCard";
        }
        try {
            AppSetting saved = appSettingService.updateGeneral(settingsForm);
            model.addAttribute("settingsForm", AppSettingForm.fromEntity(saved));
            model.addAttribute("generalSuccess", "General settings saved successfully.");
            response.setHeader("HX-Trigger", "settingsUpdated");
        } catch (AppException e) {
            model.addAttribute("generalFailure", e.getMessage());
        }
        return "admin/settings :: generalCard";
    }

    @HxRequest
    @PostMapping("/matching")
    public String saveMatching(@Validated(AppSettingForm.MatchingGroup.class) @ModelAttribute("settingsForm") AppSettingForm settingsForm,
                               BindingResult bindingResult,
                               Model model,
                               HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "admin/settings :: matchingCard";
        }
        try {
            AppSetting saved = appSettingService.updateMatching(settingsForm);
            model.addAttribute("settingsForm", AppSettingForm.fromEntity(saved));
            model.addAttribute("matchingSuccess", "Matching and Alert settings saved successfully.");
            response.setHeader("HX-Trigger", "settingsUpdated");
        } catch (AppException e) {
            model.addAttribute("matchingFailure", e.getMessage());
        }
        return "admin/settings :: matchingCard";
    }

    @HxRequest
    @PostMapping("/security")
    public String saveSecurity(@Validated(AppSettingForm.SecurityGroup.class) @ModelAttribute("settingsForm") AppSettingForm settingsForm,
                               BindingResult bindingResult,
                               Model model,
                               HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "admin/settings :: securityCard";
        }
        try {
            AppSetting saved = appSettingService.updateSecurity(settingsForm);
            model.addAttribute("settingsForm", AppSettingForm.fromEntity(saved));
            model.addAttribute("securitySuccess", "Security settings saved successfully.");
            response.setHeader("HX-Trigger", "settingsUpdated");
        } catch (AppException e) {
            model.addAttribute("securityFailure", e.getMessage());
        }
        return "admin/settings :: securityCard";
    }

    @HxRequest
    @PostMapping("/mpesa")
    public String saveMpesa(@Validated(AppSettingForm.MpesaGroup.class) @ModelAttribute("settingsForm") AppSettingForm settingsForm,
                            BindingResult bindingResult,
                            Model model,
                            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "admin/settings :: mpesaCard";
        }
        try {
            AppSetting saved = appSettingService.updateMpesa(settingsForm);
            model.addAttribute("settingsForm", AppSettingForm.fromEntity(saved));
            model.addAttribute("mpesaSuccess", "M-Pesa settings saved successfully.");
            response.setHeader("HX-Trigger", "settingsUpdated");
        } catch (AppException e) {
            model.addAttribute("mpesaFailure", e.getMessage());
        }
        return "admin/settings :: mpesaCard";
    }

    @HxRequest
    @PostMapping("/sms")
    public String saveSms(@Validated(AppSettingForm.SmsGroup.class) @ModelAttribute("settingsForm") AppSettingForm settingsForm,
                          BindingResult bindingResult,
                          Model model,
                          HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "admin/settings :: smsCard";
        }
        try {
            AppSetting saved = appSettingService.updateSms(settingsForm);
            model.addAttribute("settingsForm", AppSettingForm.fromEntity(saved));
            model.addAttribute("smsSuccess", "SMS Gateway settings saved successfully.");
            response.setHeader("HX-Trigger", "settingsUpdated");
        } catch (AppException e) {
            model.addAttribute("smsFailure", e.getMessage());
        }
        return "admin/settings :: smsCard";
    }

    @HxRequest
    @PostMapping("/email")
    public String saveEmail(@Validated(AppSettingForm.EmailGroup.class) @ModelAttribute("settingsForm") AppSettingForm settingsForm,
                            BindingResult bindingResult,
                            Model model,
                            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "admin/settings :: emailCard";
        }
        try {
            AppSetting saved = appSettingService.updateEmail(settingsForm);
            model.addAttribute("settingsForm", AppSettingForm.fromEntity(saved));
            model.addAttribute("emailSuccess", "Email (SMTP) settings saved successfully.");
            response.setHeader("HX-Trigger", "settingsUpdated");
        } catch (AppException e) {
            model.addAttribute("emailFailure", e.getMessage());
        }
        return "admin/settings :: emailCard";
    }

    @HxRequest
    @PostMapping("/retention")
    public String saveRetention(@Validated(AppSettingForm.RetentionGroup.class) @ModelAttribute("settingsForm") AppSettingForm settingsForm,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "admin/settings :: retentionCard";
        }
        try {
            AppSetting saved = appSettingService.updateRetention(settingsForm);
            model.addAttribute("settingsForm", AppSettingForm.fromEntity(saved));
            model.addAttribute("retentionSuccess", "Data Retention settings saved successfully.");
            response.setHeader("HX-Trigger", "settingsUpdated");
        } catch (AppException e) {
            model.addAttribute("retentionFailure", e.getMessage());
        }
        return "admin/settings :: retentionCard";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "settings";
    }
}
