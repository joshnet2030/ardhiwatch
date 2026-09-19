package com.josh.ardhi.controllers;

import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.repositories.AppUserRepository;
import com.josh.ardhi.services.AuthService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.http.HttpResponse;

@Controller
public class AuthController {

    private final AuthService authService;
    private final AppUserRepository appUserRepository;

    public AuthController(AuthService authService, AppUserRepository appUserRepository) {
        this.authService = authService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model ){
        if(error != null){
            model.addAttribute("message", "Invalid login credentials");
        }
        return "auth/login";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ForgotPasswordForm {
        @NotBlank
        private String email;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model){
        model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @HxRequest
    @PostMapping("/process-forgot-password")
    public String processForgotPassword(@ModelAttribute @Valid ForgotPasswordForm forgotPasswordForm, BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            return "auth/forgot-password::forgot-password-form";
        }

        authService.sendPasswordResetLink(forgotPasswordForm.getEmail());

        model.addAttribute("message", "Password reset link has been sent to your email");
        return "auth/forgot-password::forgot-password-form";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ResetPasswordForm {
        @NotBlank String passwordResetToken;
        @NotBlank String newPassword;
        @NotBlank String newPasswordConfirmation;
    }

    @GetMapping("/reset-password/{token}")
    public String resetPassword(Model model, @PathVariable String token){
        AppUser user = appUserRepository.findFirstByPasswordResetToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));
        model.addAttribute("passwordResetToken", token);
        model.addAttribute("resetPasswordForm", new ResetPasswordForm());
        return "auth/reset-password";
    }

    @HxRequest
    @PostMapping("/process-reset-password")
    public String processResetPassword(HttpServletResponse httpResponse, @ModelAttribute @Valid ResetPasswordForm resetPasswordForm, BindingResult bindingResult, Model model){
        if(resetPasswordForm.getNewPassword() != null && !resetPasswordForm.newPassword.equals(resetPasswordForm.newPasswordConfirmation)){
            bindingResult.addError(new FieldError("resetPasswordForm", "newPassword", "Passwords do not match"));
        }
        if(bindingResult.hasErrors()){
            return "auth/reset-password::reset-password-form";
        }

        AppUser user = appUserRepository.findFirstByPasswordResetToken(resetPasswordForm.getPasswordResetToken()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        authService.resetPassword(user, resetPasswordForm.getNewPassword());

        model.addAttribute("message", "Your password has been set. Proceed to <a href='/login'>login</a>");

        return "auth/reset-password::reset-password-form";
    }
}
