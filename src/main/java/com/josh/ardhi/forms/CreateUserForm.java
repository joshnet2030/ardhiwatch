package com.josh.ardhi.forms;

import com.josh.ardhi.models.AppUserRole;

public interface CreateUserForm {

    String getFullName();

    String getEmail();

    String getPhoneNumber();

    String getPassword();

    AppUserRole getRole();
}
