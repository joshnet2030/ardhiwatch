package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.ClientSignUpForm;
import com.josh.ardhi.models.AppSession;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.AppUserRole;
import com.josh.ardhi.models.Imail;
import com.josh.ardhi.repositories.AppSessionRepository;
import com.josh.ardhi.repositories.AppUserRepository;
import com.josh.ardhi.repositories.ImailRepository;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.AuthService;
import com.josh.ardhi.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AppSessionRepository appSessionRepository;
    private final AppUserService appUserService;
    private final ImailRepository imailRepository;
    @Value("${app.url}")
    public String appUrl;

    final private AppUserRepository appUserRepository;
    final private PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthServiceImpl(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, EmailService emailService, AppSessionRepository appSessionRepository, AppUserService appUserService, ImailRepository imailRepository){
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.appSessionRepository = appSessionRepository;
        this.appUserService = appUserService;
        this.imailRepository = imailRepository;
    }

    @Override
    public AppSession login(String userAgent, String ipAddress, String username, String password) throws AppException {
        if (appUserRepository.count() == 0){
            AppUser systemAdmin = AppUser.builder()
                    .email(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .fullName("System Admin")
                    .build();

            systemAdmin.setRole(AppUserRole.ADMIN);

            appUserRepository.save(systemAdmin);
        }

        AppUser user = appUserRepository.findFirstByEmailIgnoreCase(username)
                .orElseThrow(() -> new AppException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AppException("Invalid username or password");
        }

        var newSession = AppSession.builder()
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .user(user)
                .build();

        appSessionRepository.save(newSession);

        return newSession;

    }

    @Override
    public void sendPasswordResetLink(String email) {
        email = email.trim();
        AppUser user = appUserRepository.findFirstByEmailIgnoreCase(email).orElse(null);
        if( user == null){
            log.info("attempt to reset password with user who does not exist {}", email);
            return;
        }
        String passwordResetToken = UUID.randomUUID().toString();

        user.setPasswordResetToken(passwordResetToken);
        user.setPasswordResetTokenSentAt(ZonedDateTime.now());
        appUserRepository.save(user);

        log.info("Password reset token: {}", passwordResetToken);

        String passwordResetUrl = appUrl+"/reset-password/"+passwordResetToken;
        emailService.queueEmail( email,"Password reset", "Your password reset link is "+passwordResetUrl);
    }

    @Override
    public void resetPassword(AppUser user, String newPassword) {
        String encryptedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encryptedPassword);
        user.setPasswordResetTokenSentAt(null);
        user.setPasswordResetToken(null);
        appUserRepository.save(user);
    }

    @Override
    public AppUser clientSignup(ClientSignUpForm clientSignUpForm) {
       var appUser = appUserService.create(null, clientSignUpForm);
       appUser.setRole(clientSignUpForm.getRole());
       appUserRepository.save(appUser);

       sendEmailConfirmationToken(appUser);

       return appUser;
    }

    @Override
    public void resendEmailConfirmationToken(String email) {
        Optional<AppUser> optionalAppUser = appUserRepository.findFirstByEmailIgnoreCase(email.trim());
        if(optionalAppUser.isPresent()) {
            sendEmailConfirmationToken(optionalAppUser.get());
        }
    }

    @Override
    public void confirmClientEmail(String token) throws AppException {
        AppUser appUser = appUserRepository.findFirstByEmailVerificationToken(token).orElse(null);
        if(appUser == null){
            log.info("Invalid token");
            throw new AppException("Invalid email verification token.");
        }
        if(appUser.isEmailVerified()){
            log.info("email already verified");
            throw new AppException("Invalid email verification token.");
        }
        if(appUser.getEmailVerificationTokenExpiresAt().isBefore(ZonedDateTime.now())){
            log.info("Email verification token is expired.");
            throw new AppException("Invalid email verification token.");
        }
        appUser.setEmailVerified(true);
        appUser.setEmailVerifiedAt(ZonedDateTime.now());
        appUserRepository.save(appUser);
    }

    @Override
    public void logout(UUID appSessionId) {
        AppSession appSession = appSessionRepository.findById(appSessionId).orElse(null);
        if(appSession != null){
            appSession.setActive(false);
            appSession.setDeactivationMemo("Logged out");
            appSessionRepository.save(appSession);
        }
    }

    private void sendEmailConfirmationToken(AppUser appUser){
        String emailVerificationToken = UUID.randomUUID().toString();
        String emailConfirmationMessage = "Your email confirmation link: "+appUrl+"/verify-email?token="+emailVerificationToken;
        emailService.queueEmail(appUser.getEmail(), "Email confirmation", emailConfirmationMessage);
        appUser.setEmailVerified(false);
        appUser.setEmailVerificationToken(emailVerificationToken);
        appUser.setEmailVerificationTokenExpiresAt(ZonedDateTime.now().plusHours(24));
        appUserRepository.save(appUser);
    }

}
