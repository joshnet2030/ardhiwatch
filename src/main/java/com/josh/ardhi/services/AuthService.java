package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.ClientSignUpForm;
import com.josh.ardhi.models.AppSession;
import com.josh.ardhi.models.AppUser;

import java.util.UUID;

public interface AuthService {

    public AppSession login(String userAgent, String ipAddress, String username, String password) throws AppException;

    public void sendPasswordResetLink(String email);

    public void resetPassword(AppUser user, String newPassword);

    public AppUser clientSignup(ClientSignUpForm clientSignUpForm);

    public void resendEmailConfirmationToken(String email);

    public void confirmClientEmail(String email, String token) throws AppException;

    public void logout(UUID appSessionId);
}
