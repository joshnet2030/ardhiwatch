package com.josh.ardhi.repositories;

import com.josh.ardhi.models.GazettePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GazettePageRepository extends JpaRepository<GazettePage, UUID> {

    List<GazettePage> findAllByGazetteIssueIdAndIsActiveTrueOrderByPageNumberAsc(UUID gazetteIssueId);

    Optional<GazettePage> findFirstByGazetteIssueIdAndPageNumberAndIsActiveTrue(UUID gazetteIssueId, Integer pageNumber);

    long countByGazetteIssueIdAndIsActiveTrue(UUID gazetteIssueId);

    @Query(value = "SELECT * FROM gazette_page " +
            "WHERE is_active = TRUE " +
            "AND ocr_text_tsv @@ websearch_to_tsquery('english', :query) " +
            "ORDER BY ts_rank(ocr_text_tsv, websearch_to_tsquery('english', :query)) DESC",
            nativeQuery = true)
    List<GazettePage> fullTextSearch(@Param("query") String query);

    @Query(value = "SELECT * FROM gazette_page " +
            "WHERE gazette_issue_id = :gazetteIssueId " +
            "AND is_active = TRUE " +
            "AND ocr_text IS NOT NULL " +
            "AND (ocr_text ILIKE CONCAT('%', :keyword, '%') " +
            "     OR ocr_text_tsv @@ phraseto_tsquery('english', :keyword)) " +
            "ORDER BY page_number ASC",
            nativeQuery = true)
    List<GazettePage> findExactMatchesInIssue(@Param("gazetteIssueId") UUID gazetteIssueId,
                                              @Param("keyword") String keyword);

    @Query(value = "SELECT * FROM gazette_page " +
            "WHERE gazette_issue_id = :gazetteIssueId " +
            "AND is_active = TRUE " +
            "AND ocr_text IS NOT NULL " +
            "AND word_similarity(:keyword, ocr_text) >= :minSimilarity " +
            "ORDER BY page_number ASC",
            nativeQuery = true)
    List<GazettePage> findFuzzyMatchesInIssue(@Param("gazetteIssueId") UUID gazetteIssueId,
                                              @Param("keyword") String keyword,
                                              @Param("minSimilarity") double minSimilarity);

    @Query(value = "SELECT COALESCE(word_similarity(:keyword, ocr_text), 0.0) " +
            "FROM gazette_page WHERE id = :pageId",
            nativeQuery = true)
    Double computeWordSimilarityOnPage(@Param("pageId") UUID pageId,
                                       @Param("keyword") String keyword);

}
