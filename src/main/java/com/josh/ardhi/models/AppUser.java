package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "app_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppUser extends Auditable {

    @Column(name = "email")
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private AppUserRole role;

    private boolean isEmailVerified;

    private String emailVerificationToken;

    private ZonedDateTime emailVerificationTokenExpiresAt;

    private ZonedDateTime emailVerifiedAt;

    @Column(name = "last_login_at")
    private ZonedDateTime lastLoginAt;

    @Column(name = "password_reset_token")
    String passwordResetToken;

    @Column(name = "password_reset_token_sent_at")
    ZonedDateTime passwordResetTokenSentAt;

    @Column(name = "internal_notes", columnDefinition = "text")
    private String internalNotes;


}
