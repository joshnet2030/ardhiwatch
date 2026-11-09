package com.josh.ardhi.scheduler;

import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.models.GazetteIssueStatus;
import com.josh.ardhi.repositories.GazetteIssueRepository;
import com.josh.ardhi.services.GazetteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class GazetteProcessingScheduler {

    private final GazetteIssueRepository gazetteIssueRepository;
    private final GazetteService gazetteService;

    public GazetteProcessingScheduler(GazetteIssueRepository gazetteIssueRepository,
                                      GazetteService gazetteService) {
        this.gazetteIssueRepository = gazetteIssueRepository;
        this.gazetteService = gazetteService;
    }

    @Scheduled(fixedDelay = 5000L)
    public void processUploadedGazettes() {
        List<GazetteIssue> uploadedIssues = gazetteIssueRepository
                .findAllByStatusAndIsActiveTrueOrderByCreatedAtAsc(GazetteIssueStatus.UPLOADED);

        for (GazetteIssue issue : uploadedIssues) {
            log.info("Processing uploaded Gazette issue {} (Vol. {}, No. {})",
                    issue.getId(), issue.getVolumeNumber(), issue.getIssueNumber());
            try {
                gazetteService.splitAndExtractGazetteIssue(issue.getId());
            } catch (Throwable e) {
                log.error("Error processing Gazette issue {}: {}", issue.getId(), e.getMessage(), e);
            }
        }
    }
}
