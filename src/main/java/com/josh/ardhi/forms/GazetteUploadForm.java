package com.josh.ardhi.forms;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class GazetteUploadForm {

    @NotNull
    private Integer volume;

    @NotNull
    private LocalDate issueDate;

    @NotNull
    MultipartFile pdfFile;

}
