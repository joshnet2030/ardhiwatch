package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.GazetteUploadForm;
import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.models.GazetteIssueStatus;
import com.josh.ardhi.models.SystemFile;
import com.josh.ardhi.repositories.GazetteIssueRepository;
import com.josh.ardhi.services.GazetteService;
import com.josh.ardhi.services.SystemFileService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GazetteServiceImpl implements GazetteService {

    private final GazetteIssueRepository gazetteIssueRepository;
    private final SystemFileService systemFileService;

    public GazetteServiceImpl(GazetteIssueRepository gazetteIssueRepository,
                              SystemFileService systemFileService) {
        this.gazetteIssueRepository = gazetteIssueRepository;
        this.systemFileService = systemFileService;
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public GazetteIssue uploadGazette(GazetteUploadForm form) throws AppException {
        SystemFile pdfSystemFile = systemFileService.store(form.getPdfFile());

        GazetteIssue issue = GazetteIssue.builder()
                .volumeNumber(form.getVolumeNumber())
                .issueNumber(form.getIssueNumber())
                .publicationDate(form.getPublicationDate())
                .status(GazetteIssueStatus.UPLOADED)
                .pdfSystemFile(pdfSystemFile)
                .build();

        return gazetteIssueRepository.save(issue);
    }

    @Override
    public List<GazetteIssue> getAllActiveIssues() {
        return gazetteIssueRepository.findAllByIsActiveTrueOrderByPublicationDateDescCreatedAtDesc();
    }

    @Override
    public List<GazetteIssue> getRecentIssues() {
        return gazetteIssueRepository.findTop5ByIsActiveTrueOrderByPublicationDateDescCreatedAtDesc();
    }

    @Override
    public Optional<GazetteIssue> findById(UUID id) {
        return gazetteIssueRepository.findById(id);
    }
}
