package com.josh.ardhi.controllers.auth;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.ClientSignUpForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.repositories.AppUserRepository;
import com.josh.ardhi.services.AuthService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

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
        String dashboardRedirect = resolveAuthenticatedDashboardRedirect();
        if (dashboardRedirect != null) {
            return dashboardRedirect;
        }
        if(error != null){
            model.addAttribute("message", "Invalid login credentials");
        }
        return "auth/login";
    }

    private String resolveAuthenticatedDashboardRedirect() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        if (roles.contains("ROLE_ADMIN")) {
            return "redirect:/admin";
        }
        if (roles.contains("ROLE_CLIENT")) {
            return "redirect:/client";
        }
        return null;
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
    @Transactional
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
    @Transactional
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

    @GetMapping("/client-signup")
    public String ClientSignUpForm(Model model){
        var signupForm = new ClientSignUpForm();
        model.addAttribute("signUpForm", signupForm);
        return "auth/client_signup";
    }

    @HxRequest
    @PostMapping("/process-client-signup")
    @Transactional
    public String processClientSignup(@ModelAttribute("signUpForm") @Valid ClientSignUpForm signUpForm, BindingResult bindingResult){
        if(!StringUtils.hasText(signUpForm.getPassword()) && !signUpForm.getPassword().trim().equals(signUpForm.getConfirmPassword())){
            bindingResult.rejectValue("password", "password.mismatch", "Password do not match");
            return "auth/client_signup::client-signup-form";
        }

        Optional<AppUser> existingUser = appUserRepository.findFirstByEmailIgnoreCase(signUpForm.getEmail());
        if(existingUser.isPresent()){
            bindingResult.rejectValue("email", "email.exists", "User with similar email address already exists.");
            return "auth/client_signup::client-signup-form";
        }

        if(bindingResult.hasErrors()){
            return "auth/client_signup::client-signup-form";
        }
        authService.clientSignup(signUpForm);

        return "auth/client_signup_success::client_signup_success";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(name="token", required = false) String token, Model model){

        if(!StringUtils.hasText(token)){
            model.addAttribute("message", "Your email is not verified");
            return "auth/resend_email_verification_token_form";
        }

        Optional<AppUser> user = appUserRepository.findFirstByEmailVerificationToken(token);
        if(user.isEmpty()){
            return "auth/resend_email_verification_token_form";
        }

        model.addAttribute("token", token);
        return "auth/verify_email";
    }

    @PostMapping("/process-verify-email")
    public String processVerifyEmail(@RequestParam String token, Model model) {

        try {
            authService.confirmClientEmail(token);
            return "auth/verify_email_success";
        } catch (AppException e) {
            Optional<AppUser> user = appUserRepository.findFirstByEmailVerificationToken(token);
            if(!user.isEmpty()) {
                model.addAttribute("email", user.get().getEmail());
            }
           return "auth/verify_email_failed";
        }

    }

    @PostMapping("/resend-email-verification-token")
    public String resendEmailVerificationToken(@RequestParam String email, Model model){
        authService.resendEmailConfirmationToken(email);
        model.addAttribute("message", "Email verification token has been sent");
        return "auth/verify_email_failed";
    }
}
