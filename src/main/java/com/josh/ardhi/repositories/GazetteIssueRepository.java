package com.josh.ardhi.repositories;

import com.josh.ardhi.models.GazetteIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GazetteIssueRepository extends JpaRepository<GazetteIssue, UUID> {

    List<GazetteIssue> findAllByIsActiveTrueOrderByPublicationDateDescCreatedAtDesc();

    List<GazetteIssue> findTop5ByIsActiveTrueOrderByPublicationDateDescCreatedAtDesc();

}
