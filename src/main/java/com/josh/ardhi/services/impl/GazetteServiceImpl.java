package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.GazetteUploadForm;
import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.models.GazetteIssueStatus;
import com.josh.ardhi.models.GazettePage;
import com.josh.ardhi.models.GazettePageStatus;
import com.josh.ardhi.models.SystemFile;
import com.josh.ardhi.repositories.GazetteIssueRepository;
import com.josh.ardhi.repositories.GazettePageRepository;
import com.josh.ardhi.services.GazetteService;
import com.josh.ardhi.services.SystemFileService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GazetteServiceImpl implements GazetteService {

    private final GazetteIssueRepository gazetteIssueRepository;
    private final GazettePageRepository gazettePageRepository;
    private final SystemFileService systemFileService;

    public GazetteServiceImpl(GazetteIssueRepository gazetteIssueRepository,
                              GazettePageRepository gazettePageRepository,
                              SystemFileService systemFileService) {
        this.gazetteIssueRepository = gazetteIssueRepository;
        this.gazettePageRepository = gazettePageRepository;
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
    public void splitAndExtractGazetteIssue(UUID gazetteIssueId) throws AppException {
        GazetteIssue issue = gazetteIssueRepository.findById(gazetteIssueId)
                .orElseThrow(() -> new AppException("Gazette issue not found: " + gazetteIssueId));

        if (issue.getPdfSystemFile() == null || issue.getPdfSystemFile().getFilePath() == null) {
            markIssueFailed(issue, "Gazette issue has no associated PDF file");
            throw new AppException("Gazette issue has no associated PDF file");
        }

        File sourcePdfFile = new File(issue.getPdfSystemFile().getFilePath());
        if (!sourcePdfFile.exists() || !sourcePdfFile.isFile()) {
            markIssueFailed(issue, "PDF file does not exist on disk: " + sourcePdfFile.getAbsolutePath());
            throw new AppException("PDF file does not exist on disk: " + sourcePdfFile.getAbsolutePath());
        }

        updateIssueStatus(issue, GazetteIssueStatus.SPLITTING);

        try (PDDocument sourceDocument = Loader.loadPDF(sourcePdfFile)) {
            Splitter splitter = new Splitter();
            List<PDDocument> splitPages = splitter.split(sourceDocument);

            updateIssueStatus(issue, GazetteIssueStatus.OCR_PROCESSING);

            PDFTextStripper textStripper = new PDFTextStripper();

            try {
                int pageNumber = 1;
                for (PDDocument pageDocument : splitPages) {
                    processSinglePage(issue, pageDocument, pageNumber, textStripper);
                    pageNumber++;
                }
            } finally {
                for (PDDocument pageDocument : splitPages) {
                    try {
                        pageDocument.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            updateIssueStatus(issue, GazetteIssueStatus.OCR_DONE);
            log.info("Finished splitting and extracting {} page(s) for Gazette Vol. {}, No. {}",
                    splitPages.size(), issue.getVolumeNumber(), issue.getIssueNumber());
        } catch (Exception e) {
            markIssueFailed(issue, e.getMessage());
            throw new AppException("Failed to split and extract gazette issue: " + e.getMessage());
        }
    }

    private void processSinglePage(GazetteIssue issue,
                                   PDDocument pageDocument,
                                   int pageNumber,
                                   PDFTextStripper textStripper) throws AppException {
        GazettePage page = gazettePageRepository
                .findFirstByGazetteIssueIdAndPageNumberAndIsActiveTrue(issue.getId(), pageNumber)
                .orElseGet(() -> GazettePage.builder()
                        .gazetteIssue(issue)
                        .pageNumber(pageNumber)
                        .status(GazettePageStatus.OCR_PROCESSING)
                        .build());

        try {
            byte[] pagePdfBytes;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                pageDocument.save(outputStream);
                pagePdfBytes = outputStream.toByteArray();
            }

            String pageFileName = String.format("gazette-vol-%d-no-%d-page-%d.pdf",
                    issue.getVolumeNumber(), issue.getIssueNumber(), pageNumber);

            SystemFile pageSystemFile = systemFileService.storeBytes(pagePdfBytes, pageFileName, "application/pdf");
            page.setPdfSystemFile(pageSystemFile);

            String rawText = textStripper.getText(pageDocument);
            String sanitizedText = rawText != null ? rawText.replace("\u0000", "").trim() : "";

            page.setOcrText(sanitizedText);
            page.setStatus(GazettePageStatus.OCR_DONE);
            page.setFailureMessage(null);
            gazettePageRepository.save(page);
        } catch (Exception e) {
            log.error("Failed to extract page {} of Gazette issue {}: {}", pageNumber, issue.getId(), e.getMessage());
            page.setStatus(GazettePageStatus.OCR_FAILED);
            page.setFailureMessage(e.getMessage());
            gazettePageRepository.save(page);
        }
    }

    private void updateIssueStatus(GazetteIssue issue, GazetteIssueStatus status) {
        issue.setStatus(status);
        gazetteIssueRepository.saveAndFlush(issue);
    }

    private void markIssueFailed(GazetteIssue issue, String reason) {
        log.error("Marking Gazette issue {} as FAILED: {}", issue.getId(), reason);
        issue.setStatus(GazetteIssueStatus.FAILED);
        gazetteIssueRepository.saveAndFlush(issue);
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

    @Override
    public List<GazettePage> getPagesForIssue(UUID gazetteIssueId) {
        return gazettePageRepository.findAllByGazetteIssueIdAndIsActiveTrueOrderByPageNumberAsc(gazetteIssueId);
    }
}
