package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "gazette_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GazetteIssue extends Auditable {

    private Integer volumeNumber;

    private Integer issueNumber;

    private LocalDate publicationDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GazetteIssueStatus status = GazetteIssueStatus.UPLOADED;

    @ManyToOne
    @JoinColumn(name = "pdf_system_file_id")
    private SystemFile pdfSystemFile;

}
