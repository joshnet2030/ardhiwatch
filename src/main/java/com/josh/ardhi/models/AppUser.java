package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name="app_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppUser extends Auditable {

    @Column(name="email")
    private String email;

    @Column(name="full_name")
    private String fullName;

    @Column(name="phone")
    private String phone;

    @Column(name="password_hash")
    private String passwordHash;

    @Column(name="is_admin")
    private boolean isAdmin;

    @Column(name="is_clinet")
    private boolean isClient;

    @Column(name="last_login_at")
    private ZonedDateTime lastLoginAt;

    @Column(name="password_reset_token")
    String passwordResetToken;

    @Column(name="password_reset_token_sent_at")
    ZonedDateTime passwordResetTokenSentAt;

}
