package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.AdminClientForm;
import com.josh.ardhi.forms.CreateUserForm;
import com.josh.ardhi.models.AppUser;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AppUserService {

    public AppUser create(String createdBy, CreateUserForm form);

    AppUser createUser(CreateUserForm form) throws AppException;

    AppUser getById(UUID id) throws AppException;

    List<AppUser> getAllUsers();

    Page<AppUser> searchUsers(String q, String role, String status, int page, int size);

    Page<AppUser> searchClients(String q, String verified, String status, int page, int size);

    AppUser createClient(AdminClientForm form) throws AppException;

    AppUser updateClient(UUID id, AdminClientForm form) throws AppException;

    AppUser updateClientStatus(UUID id, boolean active, String deactivationMemo) throws AppException;

    AppUser updateInternalNotes(UUID id, String internalNotes) throws AppException;

    AppUser addToWalletBalance(UUID id, BigDecimal amount) throws AppException;

    AppUser deductFromWalletBalance(UUID id, BigDecimal amount) throws AppException;

    long countActiveClients();

}
