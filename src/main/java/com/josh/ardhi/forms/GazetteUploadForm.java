package com.josh.ardhi.forms;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class GazetteUploadForm {

    @NotNull(message = "Volume is required")
    @Min(value = 1, message = "Volume must be at least 1")
    private Integer volumeNumber;

    @NotNull(message = "Issue number is required")
    @Min(value = 1, message = "Issue number must be at least 1")
    private Integer issueNumber;

    @NotNull(message = "Publication date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publicationDate;

    private MultipartFile pdfFile;

}
