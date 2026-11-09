package com.josh.ardhi.repositories;

import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.AppUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findFirstByEmailIgnoreCase(String username);

    Optional<AppUser> findFirstByPasswordResetToken(String token);

    Optional<AppUser> findFirstByEmailIgnoreCaseAndEmailVerificationToken(String email, String emailVerificationToken);

    Optional<AppUser> findFirstByEmailVerificationToken(String token);

    List<AppUser> findAllByOrderByCreatedAtDesc();

    long countByRoleAndIsActiveTrue(AppUserRole role);

    long countByIsActiveFalse();
}
