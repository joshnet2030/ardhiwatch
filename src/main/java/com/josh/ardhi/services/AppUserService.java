package com.josh.ardhi.services;

import com.josh.ardhi.forms.CreateUserForm;
import com.josh.ardhi.models.AppUser;

public interface AppUserService {

    public AppUser create(String createdBy, CreateUserForm form);

}
