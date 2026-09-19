package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.repositories.AppUserRepository;
import com.josh.ardhi.services.AuthService;
import com.josh.ardhi.services.EmailService;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Value("${app.url}")
    public String appUrl;

    final private AppUserRepository appUserRepository;
    final private PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthServiceImpl(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, EmailService emailService){
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public AppUser login(String username, String password) throws AppException {
        if (appUserRepository.count() == 0){
            AppUser systemAdmin = AppUser.builder()
                    .email(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .isAdmin(true)
                    .fullName("System Admin")
                    .build();

            appUserRepository.save(systemAdmin);
        }

        AppUser user = appUserRepository.findFirstByEmailIgnoreCase(username)
                .orElseThrow(() -> new AppException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AppException("Invalid username or password");
        }

        return user;

    }

    @Override
    public void sendPasswordResetLink(String email) {
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
        emailService.sendEmail(email, "Password reset", "Your password reset link is "+passwordResetUrl);
    }

    @Override
    public void resetPassword(AppUser user, String newPassword) {
        String encryptedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encryptedPassword);
        user.setPasswordResetTokenSentAt(null);
        user.setPasswordResetToken(null);
        appUserRepository.save(user);
    }
}
