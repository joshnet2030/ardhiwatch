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

import java.time.LocalDateTime;

@Entity
@Table(name = "client_keyword_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientKeywordMatch extends Auditable {

    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private AppUser client;

    @ManyToOne
    @JoinColumn(name = "client_keyword_id", nullable = false)
    private ClientKeyword clientKeyword;

    @ManyToOne
    @JoinColumn(name = "gazette_issue_id", nullable = false)
    private GazetteIssue gazetteIssue;

    @ManyToOne
    @JoinColumn(name = "gazette_page_id", nullable = false)
    private GazettePage gazettePage;

    @ManyToOne
    @JoinColumn(name = "highlighted_pdf_system_file_id")
    private SystemFile highlightedPdfSystemFile;

    @Column(name = "keyword_text", nullable = false)
    private String keywordText;

    @Column(name = "matched_text", nullable = false, columnDefinition = "text")
    private String matchedText;

    @Column(name = "snippet_text", columnDefinition = "text")
    private String snippetText;

    @Column(name = "match_precision", nullable = false)
    @Builder.Default
    private String matchPrecision = "EXACT";

    @Column(name = "similarity_score", nullable = false)
    @Builder.Default
    private Integer similarityScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ClientKeywordMatchStatus status = ClientKeywordMatchStatus.PENDING_REVIEW;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "review_notes", columnDefinition = "text")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public String getShortCode() {
        if (getId() == null) {
            return "#0000";
        }
        return "#" + getId().toString().substring(0, 6).toUpperCase();
    }

    public String getSnippetPrefix() {
        String source = getEffectiveSnippetSource();
        int idx = findHighlightIndex(source);
        if (idx < 0) {
            return source;
        }
        return source.substring(0, idx);
    }

    public String getSnippetHighlight() {
        String source = getEffectiveSnippetSource();
        String target = getHighlightTarget(source);
        int idx = findHighlightIndex(source);
        if (idx < 0 || target.isEmpty()) {
            return "";
        }
        return source.substring(idx, Math.min(source.length(), idx + target.length()));
    }

    public String getSnippetSuffix() {
        String source = getEffectiveSnippetSource();
        String target = getHighlightTarget(source);
        int idx = findHighlightIndex(source);
        if (idx < 0 || target.isEmpty()) {
            return "";
        }
        return source.substring(Math.min(source.length(), idx + target.length()));
    }

    private String getEffectiveSnippetSource() {
        if (snippetText != null && !snippetText.isBlank()) {
            return snippetText;
        }
        return matchedText != null ? matchedText : "";
    }

    private String getHighlightTarget(String source) {
        if (matchedText != null && !matchedText.isBlank()
                && source.toLowerCase().contains(matchedText.trim().toLowerCase())) {
            return matchedText.trim();
        }
        if (keywordText != null && !keywordText.isBlank()
                && source.toLowerCase().contains(keywordText.trim().toLowerCase())) {
            return keywordText.trim();
        }
        return matchedText != null ? matchedText.trim() : "";
    }

    private int findHighlightIndex(String source) {
        String target = getHighlightTarget(source);
        if (source.isEmpty() || target.isEmpty()) {
            return -1;
        }
        return source.toLowerCase().indexOf(target.toLowerCase());
    }

}
