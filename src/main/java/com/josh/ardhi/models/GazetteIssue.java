package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gazette_issue")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GazetteIssue extends Auditable {

    private Integer volumeNumber;
    private Integer publicationDate;

    @ManyToOne
    @JoinColumn(name = "pdf_system_file_id")
    private SystemFile pdfSystemFile;

}
