package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.AppUser;
import jakarta.validation.constraints.NotBlank;

public interface AuthService {

    public AppUser login(String username, String password) throws AppException;

    public void sendPasswordResetLink(String email);

    public void resetPassword(AppUser user, String newPassword);

}
