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

}
