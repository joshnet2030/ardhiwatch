package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.AppSettingForm;
import com.josh.ardhi.models.AppSetting;
import com.josh.ardhi.repositories.AppSettingRepository;
import com.josh.ardhi.services.AppSettingService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Consumer;

@Service
public class AppSettingServiceImpl implements AppSettingService {

    private final AppSettingRepository appSettingRepository;

    public AppSettingServiceImpl(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    @Override
    @Transactional
    public AppSetting getActiveSettings() {
        return appSettingRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .orElseGet(() -> appSettingRepository.save(AppSetting.builder().build()));
    }

    @Override
    public List<AppSetting> getSettingsHistory() {
        return appSettingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Page<AppSetting> getSettingsHistory(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? size : 10;
        return appSettingRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize));
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppSetting updateGeneral(AppSettingForm form) throws AppException {
        return saveNewVersion("General", next -> {
            next.setAppName(form.getAppName().trim());
            next.setSupportEmail(form.getSupportEmail().trim());
            next.setSupportPhone(form.getSupportPhone().trim());
            next.setSupportHours(form.getSupportHours().trim());
        });
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppSetting updateMatching(AppSettingForm form) throws AppException {
        return saveNewVersion("Matching and Alerts", next -> {
            next.setFuzzyThreshold(form.getFuzzyThreshold());
            next.setLinkExpiryDays(form.getLinkExpiryDays());
            next.setManualReview(form.isManualReview());
            next.setAutoApproveExact(form.isAutoApproveExact());
            next.setLowConfidence(form.getLowConfidence());
        });
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppSetting updateSecurity(AppSettingForm form) throws AppException {
        return saveNewVersion("Security", next -> {
            next.setSessionHours(form.getSessionHours());
            next.setMaxAttempts(form.getMaxAttempts());
            next.setLockMinutes(form.getLockMinutes());
            next.setMinPassword(form.getMinPassword());
            next.setResetHours(form.getResetHours());
            next.setVerifyHours(form.getVerifyHours());
        });
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppSetting updateMpesa(AppSettingForm form) throws AppException {
        return saveNewVersion("M-Pesa", next -> {
            next.setMpesaEnv(form.getMpesaEnv().trim());
            next.setMpesaShortcode(form.getMpesaShortcode().trim());
            if (StringUtils.hasText(form.getMpesaCallback())) {
                next.setMpesaCallback(form.getMpesaCallback().trim());
            }
            if (StringUtils.hasText(form.getMpesaKey())) {
                next.setMpesaKey(form.getMpesaKey().trim());
            }
            if (StringUtils.hasText(form.getMpesaSecret())) {
                next.setMpesaSecret(form.getMpesaSecret().trim());
            }
            if (StringUtils.hasText(form.getMpesaPasskey())) {
                next.setMpesaPasskey(form.getMpesaPasskey().trim());
            }
        });
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppSetting updateSms(AppSettingForm form) throws AppException {
        return saveNewVersion("SMS Gateway", next -> {
            next.setSmsProvider(form.getSmsProvider().trim());
            next.setSmsUsername(form.getSmsUsername().trim());
            next.setSmsSender(form.getSmsSender().trim());
            if (StringUtils.hasText(form.getSmsKey())) {
                next.setSmsKey(form.getSmsKey().trim());
            }
        });
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppSetting updateEmail(AppSettingForm form) throws AppException {
        return saveNewVersion("Email (SMTP)", next -> {
            next.setSmtpHost(form.getSmtpHost().trim());
            next.setSmtpPort(form.getSmtpPort());
            next.setSmtpSecurity(form.getSmtpSecurity().trim());
            next.setSmtpUser(form.getSmtpUser().trim());
            if (StringUtils.hasText(form.getSmtpPassword())) {
                next.setSmtpPassword(form.getSmtpPassword());
            }
            next.setFromAddress(form.getFromAddress().trim());
            next.setFromName(form.getFromName().trim());
        });
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public AppSetting updateRetention(AppSettingForm form) throws AppException {
        return saveNewVersion("Data Retention", next -> {
            next.setRetainLogs(form.getRetainLogs());
            next.setRetainContact(form.getRetainContact());
            next.setRetainClosed(form.getRetainClosed());
        });
    }

    private AppSetting saveNewVersion(String sectionName, Consumer<AppSetting> mutator) throws AppException {
        try {
            AppSetting current = getActiveSettings();
            AppSetting nextVersion = current.createNewVersionCopy();
            mutator.accept(nextVersion);

            List<AppSetting> activeRecords = appSettingRepository.findAllByIsActiveTrue();
            for (AppSetting active : activeRecords) {
                active.setActive(false);
                active.setDeactivationMemo("Superseded by updated " + sectionName + " settings");
            }
            if (!activeRecords.isEmpty()) {
                appSettingRepository.saveAllAndFlush(activeRecords);
            }

            nextVersion.setActive(true);
            return appSettingRepository.save(nextVersion);
        } catch (Exception e) {
            throw new AppException("Failed to save " + sectionName + " settings: " + e.getMessage());
        }
    }
}
