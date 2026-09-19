package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name="app_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSession extends Auditable {

    private String userAgent;

    private String ipAddress;

    @ManyToOne
    @JoinColumn(name="app_user_id")
    private AppUser user;

}
