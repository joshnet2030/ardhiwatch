package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.GazetteUploadForm;
import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.models.GazettePage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GazetteService {

    GazetteIssue uploadGazette(GazetteUploadForm form) throws AppException;

    void splitAndExtractGazetteIssue(UUID gazetteIssueId) throws AppException;

    List<GazetteIssue> getAllActiveIssues();

    List<GazetteIssue> getRecentIssues();

    Optional<GazetteIssue> findById(UUID id);

    List<GazettePage> getPagesForIssue(UUID gazetteIssueId);

}
