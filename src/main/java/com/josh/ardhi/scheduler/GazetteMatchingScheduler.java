package com.josh.ardhi.scheduler;

import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.models.GazetteIssueStatus;
import com.josh.ardhi.repositories.GazetteIssueRepository;
import com.josh.ardhi.services.ClientKeywordMatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class GazetteMatchingScheduler {

    private final GazetteIssueRepository gazetteIssueRepository;
    private final ClientKeywordMatchService clientKeywordMatchService;

    public GazetteMatchingScheduler(GazetteIssueRepository gazetteIssueRepository,
                                    ClientKeywordMatchService clientKeywordMatchService) {
        this.gazetteIssueRepository = gazetteIssueRepository;
        this.clientKeywordMatchService = clientKeywordMatchService;
    }

    @Scheduled(fixedDelay = 5000L)
    public void matchOcrDoneGazettes() {
        List<GazetteIssue> ocrDoneIssues = gazetteIssueRepository
                .findAllByStatusAndIsActiveTrueOrderByCreatedAtAsc(GazetteIssueStatus.OCR_DONE);

        for (GazetteIssue issue : ocrDoneIssues) {
            log.info("Running keyword matching for Gazette issue {} (Vol. {}, No. {})",
                    issue.getId(), issue.getVolumeNumber(), issue.getIssueNumber());
            try {
                clientKeywordMatchService.matchGazetteIssue(issue.getId());
            } catch (Throwable e) {
                log.error("Error matching keywords for Gazette issue {}: {}", issue.getId(), e.getMessage(), e);
            }
        }
    }
}
