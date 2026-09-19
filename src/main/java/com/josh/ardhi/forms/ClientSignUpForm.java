package com.josh.ardhi.forms;

import com.josh.ardhi.models.AppUserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientSignUpForm implements CreateUserForm {
    @NotBlank(message="Full name is required")
    String fullName;
    @NotBlank(message = "Email is required")
    String email;
    @NotBlank(message = "Phone number is required")
    String phoneNumber;
    @NotBlank(message = "Password is required")
    String password;
    @NotBlank
    String confirmPassword;

    @AssertTrue(message = "You must agree to the terms and conditions.")
    boolean agreeTerms;

    @Override
    public AppUserRole getRole() {
        return AppUserRole.CLIENT;
    }
}
