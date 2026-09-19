package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name="imail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Imail extends Auditable {

    private String fromEmail;

    private String toEmail;

    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    private boolean wasSentSuccessfully;

    ZonedDateTime lastSendingAttemptAt;

    int sendingAttemptCount;

    @Column(columnDefinition = "text")
    private String failureMessage;
}
