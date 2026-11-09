package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Column;
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

@Entity
@Table(name = "gazette_page")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GazettePage extends Auditable {

    @ManyToOne
    @JoinColumn(name = "gazette_issue_id", nullable = false)
    private GazetteIssue gazetteIssue;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @ManyToOne
    @JoinColumn(name = "pdf_system_file_id")
    private SystemFile pdfSystemFile;

    @Column(name = "ocr_text", columnDefinition = "text")
    private String ocrText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private GazettePageStatus status = GazettePageStatus.OCR_DONE;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

}
