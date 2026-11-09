package com.josh.ardhi.forms;

import com.josh.ardhi.models.ClientKeyword;
import com.josh.ardhi.models.KeywordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ClientKeywordForm {

    private UUID id;

    @NotBlank(message = "Keyword text is required")
    private String keyword;

    @NotNull(message = "Keyword type is required")
    private KeywordType keywordType = KeywordType.TITLE;

    @NotBlank(message = "Precision is required")
    private String matchPrecision = "EXACT";

    private String notes;

    public static ClientKeywordForm fromEntity(ClientKeyword entity) {
        ClientKeywordForm form = new ClientKeywordForm();
        form.setId(entity.getId());
        form.setKeyword(entity.getKeyword());
        form.setKeywordType(entity.getKeywordType());
        form.setMatchPrecision(entity.getMatchPrecision());
        form.setNotes(entity.getNotes());
        return form;
    }

}
