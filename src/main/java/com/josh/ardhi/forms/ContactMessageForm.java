package com.josh.ardhi.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContactMessageForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Name must be 150 characters or fewer")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    private String phone;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must be 200 characters or fewer")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 3000, message = "Message must be between 10 and 3000 characters")
    private String message;

}
