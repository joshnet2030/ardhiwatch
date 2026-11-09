package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.CreateUserForm;
import com.josh.ardhi.models.AppUser;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface AppUserService {

    public AppUser create(String createdBy, CreateUserForm form);

    AppUser createUser(CreateUserForm form) throws AppException;

    AppUser getById(UUID id) throws AppException;

    List<AppUser> getAllUsers();

    Page<AppUser> searchUsers(String q, String role, String status, int page, int size);

}
