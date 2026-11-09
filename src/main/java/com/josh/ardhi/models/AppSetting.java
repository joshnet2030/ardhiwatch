package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting extends Auditable {

    // General
    @Builder.Default
    private String appName = "Dokezo";

    @Builder.Default
    private String supportEmail = "support@dokezo.co.ke";

    @Builder.Default
    private String supportPhone = "+254 700 000 000";

    @Builder.Default
    private String supportHours = "Monday to Friday, 8:00 to 17:00 EAT";

    // Matching and Alerts
    @Builder.Default
    private Integer fuzzyThreshold = 80;

    @Builder.Default
    private Integer linkExpiryDays = 30;

    @Builder.Default
    private boolean manualReview = true;

    @Builder.Default
    private boolean autoApproveExact = false;

    @Builder.Default
    private Integer lowConfidence = 70;

    // Security
    @Builder.Default
    private Integer sessionHours = 8;

    @Builder.Default
    private Integer maxAttempts = 5;

    @Builder.Default
    private Integer lockMinutes = 15;

    @Builder.Default
    private Integer minPassword = 8;

    @Builder.Default
    private Integer resetHours = 1;

    @Builder.Default
    private Integer verifyHours = 48;

    // M-Pesa
    @Builder.Default
    private String mpesaEnv = "production";

    @Builder.Default
    private String mpesaShortcode = "174379";

    @Builder.Default
    private String mpesaCallback = "https://dokezo.co.ke/webhooks/mpesa";

    @Column(columnDefinition = "text")
    private String mpesaKey;

    @Column(columnDefinition = "text")
    private String mpesaSecret;

    @Column(columnDefinition = "text")
    private String mpesaPasskey;

    // SMS
    @Builder.Default
    private String smsProvider = "africastalking";

    @Builder.Default
    private String smsUsername = "dokezo";

    @Builder.Default
    private String smsSender = "DOKEZO";

    @Column(columnDefinition = "text")
    private String smsKey;

    // Email (SMTP)
    @Builder.Default
    private String smtpHost = "smtp.example.com";

    @Builder.Default
    private Integer smtpPort = 587;

    @Builder.Default
    private String smtpSecurity = "starttls";

    @Builder.Default
    private String smtpUser = "system@dokezo.co.ke";

    @Column(columnDefinition = "text")
    private String smtpPassword;

    @Builder.Default
    private String fromAddress = "system@dokezo.co.ke";

    @Builder.Default
    private String fromName = "Dokezo";

    // Data Retention
    @Builder.Default
    private Integer retainLogs = 12;

    @Builder.Default
    private Integer retainContact = 24;

    @Builder.Default
    private Integer retainClosed = 12;

    public AppSetting createNewVersionCopy() {
        return AppSetting.builder()
                .appName(this.appName)
                .supportEmail(this.supportEmail)
                .supportPhone(this.supportPhone)
                .supportHours(this.supportHours)
                .fuzzyThreshold(this.fuzzyThreshold)
                .linkExpiryDays(this.linkExpiryDays)
                .manualReview(this.manualReview)
                .autoApproveExact(this.autoApproveExact)
                .lowConfidence(this.lowConfidence)
                .sessionHours(this.sessionHours)
                .maxAttempts(this.maxAttempts)
                .lockMinutes(this.lockMinutes)
                .minPassword(this.minPassword)
                .resetHours(this.resetHours)
                .verifyHours(this.verifyHours)
                .mpesaEnv(this.mpesaEnv)
                .mpesaShortcode(this.mpesaShortcode)
                .mpesaCallback(this.mpesaCallback)
                .mpesaKey(this.mpesaKey)
                .mpesaSecret(this.mpesaSecret)
                .mpesaPasskey(this.mpesaPasskey)
                .smsProvider(this.smsProvider)
                .smsUsername(this.smsUsername)
                .smsSender(this.smsSender)
                .smsKey(this.smsKey)
                .smtpHost(this.smtpHost)
                .smtpPort(this.smtpPort)
                .smtpSecurity(this.smtpSecurity)
                .smtpUser(this.smtpUser)
                .smtpPassword(this.smtpPassword)
                .fromAddress(this.fromAddress)
                .fromName(this.fromName)
                .retainLogs(this.retainLogs)
                .retainContact(this.retainContact)
                .retainClosed(this.retainClosed)
                .build();
    }
}
