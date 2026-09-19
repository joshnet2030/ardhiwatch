package com.josh.ardhi.services.impl;

import com.josh.ardhi.forms.CreateUserForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.repositories.AppUserRepository;
import com.josh.ardhi.services.AppUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class AppUserServiceImpl implements AppUserService {


    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;

    public AppUserServiceImpl(PasswordEncoder passwordEncoder, AppUserRepository appUserRepository) {
        this.passwordEncoder = passwordEncoder;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AppUser create(String createdBy, CreateUserForm form) {
        var passwordHash = passwordEncoder.encode(form.getPassword());
        var emailVerificationToken = UUID.randomUUID().toString();
        var newAppuser = AppUser.builder()
                .fullName(form.getFullName())
                .email(form.getEmail())
                .phone(form.getPhoneNumber())
                .passwordHash(passwordHash)
                .emailVerificationToken(emailVerificationToken)
                .emailVerificationTokenExpiresAt(ZonedDateTime.now().plusHours(24))
                .build();
        newAppuser.setRole(form.getRole());
        appUserRepository.save(newAppuser);
        return newAppuser;
    }
}
