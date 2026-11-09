package com.josh.ardhi.forms;

import com.josh.ardhi.models.AppSetting;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AppSettingForm {

    public interface GeneralGroup {}
    public interface MatchingGroup {}
    public interface SecurityGroup {}
    public interface MpesaGroup {}
    public interface SmsGroup {}
    public interface EmailGroup {}
    public interface RetentionGroup {}

    // 1. General
    @NotBlank(message = "Service name is required", groups = GeneralGroup.class)
    @Size(max = 150, message = "Service name must be under 150 characters", groups = GeneralGroup.class)
    private String appName;

    @NotBlank(message = "Support email is required", groups = GeneralGroup.class)
    @Email(message = "Enter a valid email address", groups = GeneralGroup.class)
    private String supportEmail;

    @NotBlank(message = "Support phone is required", groups = GeneralGroup.class)
    private String supportPhone;

    @NotBlank(message = "Support hours are required", groups = GeneralGroup.class)
    private String supportHours;

    // 2. Matching and Alerts
    @NotNull(message = "Fuzzy threshold is required", groups = MatchingGroup.class)
    @Min(value = 50, message = "Must be between 50% and 100%", groups = MatchingGroup.class)
    @Max(value = 100, message = "Must be between 50% and 100%", groups = MatchingGroup.class)
    private Integer fuzzyThreshold;

    @NotNull(message = "Link expiry days is required", groups = MatchingGroup.class)
    @Min(value = 1, message = "Must be at least 1 day", groups = MatchingGroup.class)
    private Integer linkExpiryDays;

    private boolean manualReview;

    private boolean autoApproveExact;

    @NotNull(message = "OCR confidence threshold is required", groups = MatchingGroup.class)
    @Min(value = 0, message = "Must be between 0% and 100%", groups = MatchingGroup.class)
    @Max(value = 100, message = "Must be between 0% and 100%", groups = MatchingGroup.class)
    private Integer lowConfidence;

    // 3. Security
    @NotNull(message = "Session length is required", groups = SecurityGroup.class)
    @Min(value = 1, message = "Must be at least 1 hour", groups = SecurityGroup.class)
    private Integer sessionHours;

    @NotNull(message = "Max login attempts is required", groups = SecurityGroup.class)
    @Min(value = 3, message = "Must be at least 3 attempts", groups = SecurityGroup.class)
    private Integer maxAttempts;

    @NotNull(message = "Lock duration is required", groups = SecurityGroup.class)
    @Min(value = 1, message = "Must be at least 1 minute", groups = SecurityGroup.class)
    private Integer lockMinutes;

    @NotNull(message = "Minimum password length is required", groups = SecurityGroup.class)
    @Min(value = 8, message = "Minimum password length must be at least 8", groups = SecurityGroup.class)
    private Integer minPassword;

    @NotNull(message = "Reset link expiry is required", groups = SecurityGroup.class)
    @Min(value = 1, message = "Must be at least 1 hour", groups = SecurityGroup.class)
    private Integer resetHours;

    @NotNull(message = "Verification link expiry is required", groups = SecurityGroup.class)
    @Min(value = 1, message = "Must be at least 1 hour", groups = SecurityGroup.class)
    private Integer verifyHours;

    // 4. M-Pesa
    @NotBlank(message = "Environment is required", groups = MpesaGroup.class)
    private String mpesaEnv;

    @NotBlank(message = "Paybill / Till number is required", groups = MpesaGroup.class)
    private String mpesaShortcode;

    private String mpesaCallback;

    private String mpesaKey;

    private String mpesaSecret;

    private String mpesaPasskey;

    // 5. SMS
    @NotBlank(message = "SMS provider is required", groups = SmsGroup.class)
    private String smsProvider;

    @NotBlank(message = "SMS username is required", groups = SmsGroup.class)
    private String smsUsername;

    @NotBlank(message = "Sender ID is required", groups = SmsGroup.class)
    @Size(max = 11, message = "Sender ID cannot exceed 11 characters", groups = SmsGroup.class)
    private String smsSender;

    private String smsKey;

    // 6. Email (SMTP)
    @NotBlank(message = "SMTP host is required", groups = EmailGroup.class)
    private String smtpHost;

    @NotNull(message = "SMTP port is required", groups = EmailGroup.class)
    @Min(value = 1, message = "Enter a valid port", groups = EmailGroup.class)
    @Max(value = 65535, message = "Enter a valid port", groups = EmailGroup.class)
    private Integer smtpPort;

    @NotBlank(message = "SMTP security is required", groups = EmailGroup.class)
    private String smtpSecurity;

    @NotBlank(message = "SMTP username is required", groups = EmailGroup.class)
    private String smtpUser;

    private String smtpPassword;

    @NotBlank(message = "From address is required", groups = EmailGroup.class)
    @Email(message = "Enter a valid email address", groups = EmailGroup.class)
    private String fromAddress;

    @NotBlank(message = "From name is required", groups = EmailGroup.class)
    private String fromName;

    // 7. Data Retention
    @NotNull(message = "Log retention period is required", groups = RetentionGroup.class)
    @Min(value = 1, message = "Must be at least 1 month", groups = RetentionGroup.class)
    private Integer retainLogs;

    @NotNull(message = "Contact message retention is required", groups = RetentionGroup.class)
    @Min(value = 1, message = "Must be at least 1 month", groups = RetentionGroup.class)
    private Integer retainContact;

    @NotNull(message = "Closed account retention is required", groups = RetentionGroup.class)
    @Min(value = 0, message = "Must be 0 or more months", groups = RetentionGroup.class)
    private Integer retainClosed;

    public static AppSettingForm fromEntity(AppSetting setting) {
        AppSettingForm form = new AppSettingForm();
        form.setAppName(setting.getAppName());
        form.setSupportEmail(setting.getSupportEmail());
        form.setSupportPhone(setting.getSupportPhone());
        form.setSupportHours(setting.getSupportHours());

        form.setFuzzyThreshold(setting.getFuzzyThreshold());
        form.setLinkExpiryDays(setting.getLinkExpiryDays());
        form.setManualReview(setting.isManualReview());
        form.setAutoApproveExact(setting.isAutoApproveExact());
        form.setLowConfidence(setting.getLowConfidence());

        form.setSessionHours(setting.getSessionHours());
        form.setMaxAttempts(setting.getMaxAttempts());
        form.setLockMinutes(setting.getLockMinutes());
        form.setMinPassword(setting.getMinPassword());
        form.setResetHours(setting.getResetHours());
        form.setVerifyHours(setting.getVerifyHours());

        form.setMpesaEnv(setting.getMpesaEnv());
        form.setMpesaShortcode(setting.getMpesaShortcode());
        form.setMpesaCallback(setting.getMpesaCallback());

        form.setSmsProvider(setting.getSmsProvider());
        form.setSmsUsername(setting.getSmsUsername());
        form.setSmsSender(setting.getSmsSender());

        form.setSmtpHost(setting.getSmtpHost());
        form.setSmtpPort(setting.getSmtpPort());
        form.setSmtpSecurity(setting.getSmtpSecurity());
        form.setSmtpUser(setting.getSmtpUser());
        form.setFromAddress(setting.getFromAddress());
        form.setFromName(setting.getFromName());

        form.setRetainLogs(setting.getRetainLogs());
        form.setRetainContact(setting.getRetainContact());
        form.setRetainClosed(setting.getRetainClosed());
        return form;
    }
}
