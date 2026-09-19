package com.josh.ardhi.security;

import com.josh.ardhi.repositories.AppUserRepository;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityAuditorAware implements AuditorAware<String> {

    final private AppUserRepository appUserRepository;

    public SecurityAuditorAware(AppUserRepository appUserRepository){
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If no user is logged in (e.g., during registration/public pages), default to "SYSTEM"
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of("SYSTEM");
        }
        return Optional.of(authentication.getName());

    }
}
