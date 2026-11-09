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
@Table(name = "client_keyword")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientKeyword extends Auditable {

    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private AppUser client;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "keyword_type", nullable = false)
    @Builder.Default
    private KeywordType keywordType = KeywordType.TITLE;

    @Column(name = "match_precision", nullable = false)
    @Builder.Default
    private String matchPrecision = "EXACT";

    @Column(name = "notes")
    private String notes;

    @Column(name = "is_paused", nullable = false)
    @Builder.Default
    private boolean paused = false;

}
