package com.josh.ardhi.repositories;

import com.josh.ardhi.models.ClientKeyword;
import com.josh.ardhi.models.KeywordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientKeywordRepository extends JpaRepository<ClientKeyword, UUID> {

    List<ClientKeyword> findAllByClientIdAndIsActiveTrueOrderByCreatedAtDesc(UUID clientId);

    List<ClientKeyword> findAllByClientIdAndIsActiveTrueAndPausedFalseOrderByCreatedAtAsc(UUID clientId);

    long countByClientIdAndIsActiveTrue(UUID clientId);

    long countByClientIdAndIsActiveTrueAndPausedFalse(UUID clientId);

    boolean existsByClientIdAndKeywordIgnoreCaseAndKeywordTypeAndIsActiveTrue(
            UUID clientId, String keyword, KeywordType keywordType);

    boolean existsByClientIdAndKeywordIgnoreCaseAndKeywordTypeAndIsActiveTrueAndIdNot(
            UUID clientId, String keyword, KeywordType keywordType, UUID id);

}
