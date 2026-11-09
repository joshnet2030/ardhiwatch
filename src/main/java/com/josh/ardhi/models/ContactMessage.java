package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contact_message")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactMessage extends Auditable {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "is_read")
    private boolean isRead = false;

}
