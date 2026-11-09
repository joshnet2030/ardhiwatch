package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.AppSettingForm;
import com.josh.ardhi.models.AppSetting;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AppSettingService {

    AppSetting getActiveSettings();

    List<AppSetting> getSettingsHistory();

    Page<AppSetting> getSettingsHistory(int page, int size);

    AppSetting updateGeneral(AppSettingForm form) throws AppException;

    AppSetting updateMatching(AppSettingForm form) throws AppException;

    AppSetting updateSecurity(AppSettingForm form) throws AppException;

    AppSetting updateMpesa(AppSettingForm form) throws AppException;

    AppSetting updateSms(AppSettingForm form) throws AppException;

    AppSetting updateEmail(AppSettingForm form) throws AppException;

    AppSetting updateRetention(AppSettingForm form) throws AppException;

}
