package com.josh.ardhi.forms;

import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.AppUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AdminClientForm implements CreateUserForm {

    private UUID id;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String password;

    private boolean emailVerified = true;

    private boolean active = true;

    private String internalNotes;

    @Override
    public AppUserRole getRole() {
        return AppUserRole.CLIENT;
    }

    public static AdminClientForm fromEntity(AppUser user) {
        AdminClientForm form = new AdminClientForm();
        form.setId(user.getId());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhoneNumber(user.getPhone());
        form.setEmailVerified(user.isEmailVerified());
        form.setActive(user.isActive());
        form.setInternalNotes(user.getInternalNotes());
        return form;
    }

}
