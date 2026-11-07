package com.josh.ardhi.repositories;

import com.josh.ardhi.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findFirstByEmailIgnoreCase(String username);

    Optional<AppUser> findFirstByPasswordResetToken(String token);

    Optional<AppUser> findFirstByEmailIgnoreCaseAndEmailVerificationToken(String email, String emailVerificationToken);

    Optional<AppUser> findFirstByEmailVerificationToken(String token);
}
