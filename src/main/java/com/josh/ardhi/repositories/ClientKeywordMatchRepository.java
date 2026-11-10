package com.josh.ardhi.repositories;

import com.josh.ardhi.models.ClientKeywordMatch;
import com.josh.ardhi.models.ClientKeywordMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientKeywordMatchRepository extends JpaRepository<ClientKeywordMatch, UUID>, JpaSpecificationExecutor<ClientKeywordMatch> {

    Optional<ClientKeywordMatch> findFirstByClientKeywordIdAndGazettePageIdAndIsActiveTrue(UUID clientKeywordId, UUID gazettePageId);

    List<ClientKeywordMatch> findAllByClientIdAndIsActiveTrueOrderByCreatedAtDesc(UUID clientId);

    long countByClientIdAndIsActiveTrue(UUID clientId);

    long countByClientIdAndStatusAndIsActiveTrue(UUID clientId, ClientKeywordMatchStatus status);

    long countByStatusAndIsActiveTrue(ClientKeywordMatchStatus status);

    long countByIsActiveTrue();

    List<ClientKeywordMatch> findTop5ByClientIdAndIdNotAndIsActiveTrueOrderByCreatedAtDesc(UUID clientId, UUID excludeId);

    Optional<ClientKeywordMatch> findFirstByStatusAndCreatedAtBeforeAndIsActiveTrueOrderByCreatedAtDesc(
            ClientKeywordMatchStatus status, LocalDateTime createdAt);

    Optional<ClientKeywordMatch> findFirstByStatusAndCreatedAtAfterAndIsActiveTrueOrderByCreatedAtAsc(
            ClientKeywordMatchStatus status, LocalDateTime createdAt);

}
