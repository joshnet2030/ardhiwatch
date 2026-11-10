package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.AppSetting;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.ClientKeyword;
import com.josh.ardhi.models.ClientKeywordMatch;
import com.josh.ardhi.models.ClientKeywordMatchStatus;
import com.josh.ardhi.models.ClientNotification;
import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.models.GazetteIssueStatus;
import com.josh.ardhi.models.GazettePage;
import com.josh.ardhi.models.KeywordType;
import com.josh.ardhi.models.NotificationChannel;
import com.josh.ardhi.models.NotificationStatus;
import com.josh.ardhi.models.SystemFile;
import com.josh.ardhi.repositories.ClientKeywordMatchRepository;
import com.josh.ardhi.repositories.ClientKeywordRepository;
import com.josh.ardhi.repositories.ClientNotificationRepository;
import com.josh.ardhi.repositories.ClientSubscriptionPlanRepository;
import com.josh.ardhi.repositories.GazetteIssueRepository;
import com.josh.ardhi.repositories.GazettePageRepository;
import com.josh.ardhi.services.AppSettingService;
import com.josh.ardhi.services.ClientKeywordMatchService;
import com.josh.ardhi.services.SystemFileService;
import com.josh.ardhi.utils.PdfHighlighter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ClientKeywordMatchServiceImpl implements ClientKeywordMatchService {

    private final GazetteIssueRepository gazetteIssueRepository;
    private final GazettePageRepository gazettePageRepository;
    private final ClientSubscriptionPlanRepository clientSubscriptionPlanRepository;
    private final ClientKeywordRepository clientKeywordRepository;
    private final ClientKeywordMatchRepository clientKeywordMatchRepository;
    private final ClientNotificationRepository clientNotificationRepository;
    private final AppSettingService appSettingService;
    private final SystemFileService systemFileService;

    public ClientKeywordMatchServiceImpl(GazetteIssueRepository gazetteIssueRepository,
                                         GazettePageRepository gazettePageRepository,
                                         ClientSubscriptionPlanRepository clientSubscriptionPlanRepository,
                                         ClientKeywordRepository clientKeywordRepository,
                                         ClientKeywordMatchRepository clientKeywordMatchRepository,
                                         ClientNotificationRepository clientNotificationRepository,
                                         AppSettingService appSettingService,
                                         SystemFileService systemFileService) {
        this.gazetteIssueRepository = gazetteIssueRepository;
        this.gazettePageRepository = gazettePageRepository;
        this.clientSubscriptionPlanRepository = clientSubscriptionPlanRepository;
        this.clientKeywordRepository = clientKeywordRepository;
        this.clientKeywordMatchRepository = clientKeywordMatchRepository;
        this.clientNotificationRepository = clientNotificationRepository;
        this.appSettingService = appSettingService;
        this.systemFileService = systemFileService;
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public void matchGazetteIssue(UUID gazetteIssueId) throws AppException {
        GazetteIssue issue = gazetteIssueRepository.findById(gazetteIssueId)
                .orElseThrow(() -> new AppException("Gazette issue not found: " + gazetteIssueId));

        issue.setStatus(GazetteIssueStatus.MATCHING);
        gazetteIssueRepository.saveAndFlush(issue);

        try {
            AppSetting settings = appSettingService.getActiveSettings();
            int fuzzyThresholdPct = settings.getFuzzyThreshold() != null ? settings.getFuzzyThreshold() : 80;
            fuzzyThresholdPct = Math.max(10, Math.min(100, fuzzyThresholdPct));
            double minSimilarity = fuzzyThresholdPct / 100.0;

            boolean manualReview = settings.isManualReview();
            boolean autoApproveExact = settings.isAutoApproveExact();

            List<AppUser> subscribedClients = clientSubscriptionPlanRepository
                    .findClientsWithActiveValidSubscriptions(LocalDate.now());

            int matchesCreated = 0;

            for (AppUser client : subscribedClients) {
                List<ClientKeyword> keywords = clientKeywordRepository
                        .findAllByClientIdAndIsActiveTrueAndPausedFalseOrderByCreatedAtAsc(client.getId());

                for (ClientKeyword kw : keywords) {
                    if (!StringUtils.hasText(kw.getKeyword())) {
                        continue;
                    }
                    String keywordText = kw.getKeyword().trim();
                    Set<UUID> matchedPageIds = new HashSet<>();

                    // 1. Exact search first
                    List<GazettePage> exactPages = gazettePageRepository
                            .findExactMatchesInIssue(issue.getId(), keywordText);

                    for (GazettePage page : exactPages) {
                        matchedPageIds.add(page.getId());
                        ClientKeywordMatchStatus initialStatus = (!manualReview || autoApproveExact)
                                ? ClientKeywordMatchStatus.APPROVED
                                : ClientKeywordMatchStatus.PENDING_REVIEW;

                        String matchedFragment = extractExactMatchText(page.getOcrText(), keywordText);
                        String snippet = extractSnippet(page.getOcrText(), matchedFragment);

                        ClientKeywordMatch created = saveMatchAndNotificationsIfNew(
                                client, kw, issue, page, keywordText, matchedFragment, snippet,
                                "EXACT", 100, initialStatus
                        );
                        if (created != null) {
                            matchesCreated++;
                        }
                    }

                    // 2. Fuzzy search second (using pg_trgm word_similarity >= fuzzyThreshold / 100.0)
                    List<GazettePage> fuzzyPages = gazettePageRepository
                            .findFuzzyMatchesInIssue(issue.getId(), keywordText, minSimilarity);

                    for (GazettePage page : fuzzyPages) {
                        if (matchedPageIds.contains(page.getId())) {
                            continue;
                        }
                        Double rawSim = gazettePageRepository.computeWordSimilarityOnPage(page.getId(), keywordText);
                        int score = rawSim != null ? (int) Math.round(rawSim * 100.0) : fuzzyThresholdPct;
                        score = Math.max(fuzzyThresholdPct, Math.min(99, score));

                        ClientKeywordMatchStatus initialStatus = !manualReview
                                ? ClientKeywordMatchStatus.APPROVED
                                : ClientKeywordMatchStatus.PENDING_REVIEW;

                        String matchedFragment = extractBestFuzzyFragment(page.getOcrText(), keywordText);
                        String snippet = extractSnippet(page.getOcrText(), matchedFragment);

                        ClientKeywordMatch created = saveMatchAndNotificationsIfNew(
                                client, kw, issue, page, keywordText, matchedFragment, snippet,
                                "FUZZY", score, initialStatus
                        );
                        if (created != null) {
                            matchesCreated++;
                        }
                    }
                }
            }

            issue.setStatus(GazetteIssueStatus.MATCHED);
            gazetteIssueRepository.saveAndFlush(issue);
            log.info("Completed keyword matching for Gazette Vol. {}, No. {} — {} match(es) created across {} subscribed client(s)",
                    issue.getVolumeNumber(), issue.getIssueNumber(), matchesCreated, subscribedClients.size());
        } catch (Exception e) {
            log.error("Keyword matching failed for Gazette issue {}: {}", issue.getId(), e.getMessage(), e);
            issue.setStatus(GazetteIssueStatus.FAILED);
            gazetteIssueRepository.saveAndFlush(issue);
            throw new AppException("Keyword matching failed: " + e.getMessage());
        }
    }

    private ClientKeywordMatch saveMatchAndNotificationsIfNew(AppUser client,
                                                              ClientKeyword kw,
                                                              GazetteIssue issue,
                                                              GazettePage page,
                                                              String keywordText,
                                                              String matchedText,
                                                              String snippetText,
                                                              String precision,
                                                              int similarityScore,
                                                              ClientKeywordMatchStatus initialStatus) {
        Optional<ClientKeywordMatch> existing = clientKeywordMatchRepository
                .findFirstByClientKeywordIdAndGazettePageIdAndIsActiveTrue(kw.getId(), page.getId());
        if (existing.isPresent()) {
            return null;
        }

        SystemFile highlightedPdfFile = generateHighlightedPagePdf(issue, page, matchedText, keywordText);

        ClientKeywordMatch match = ClientKeywordMatch.builder()
                .client(client)
                .clientKeyword(kw)
                .gazetteIssue(issue)
                .gazettePage(page)
                .highlightedPdfSystemFile(highlightedPdfFile)
                .keywordText(keywordText)
                .matchedText(matchedText)
                .snippetText(snippetText)
                .matchPrecision(precision)
                .similarityScore(similarityScore)
                .status(initialStatus)
                .reviewedAt(initialStatus == ClientKeywordMatchStatus.APPROVED ? LocalDateTime.now() : null)
                .build();

        match = clientKeywordMatchRepository.saveAndFlush(match);

        createPendingNotificationsForMatch(match);
        return match;
    }

    private SystemFile generateHighlightedPagePdf(GazetteIssue issue,
                                                  GazettePage page,
                                                  String matchedText,
                                                  String keywordText) {
        if (page == null || page.getPdfSystemFile() == null || !StringUtils.hasText(page.getPdfSystemFile().getFilePath())) {
            return null;
        }

        File sourcePdf = new File(page.getPdfSystemFile().getFilePath());
        if (!sourcePdf.exists() || !sourcePdf.isFile()) {
            return null;
        }

        try {
            Set<String> wordsSet = new LinkedHashSet<>();
            if (StringUtils.hasText(matchedText)) {
                wordsSet.add(matchedText.trim());
            }
            if (StringUtils.hasText(keywordText)) {
                wordsSet.add(keywordText.trim());
            }
            if (StringUtils.hasText(matchedText)) {
                for (String token : matchedText.trim().split("\\s+")) {
                    if (token.length() >= 3) {
                        wordsSet.add(token);
                    }
                }
            }

            PdfHighlighter highlighter = new PdfHighlighter();
            byte[] highlightedBytes = highlighter.highlightToBytes(
                    sourcePdf,
                    wordsSet.toArray(new String[0])
            );

            String fileName = String.format("gazette-vol-%d-no-%d-page-%d-highlighted-v2.pdf",
                    issue != null && issue.getVolumeNumber() != null ? issue.getVolumeNumber() : 0,
                    issue != null && issue.getIssueNumber() != null ? issue.getIssueNumber() : 0,
                    page.getPageNumber() != null ? page.getPageNumber() : 1);

            return systemFileService.storeBytes(highlightedBytes, fileName, "application/pdf");
        } catch (Exception e) {
            log.warn("Could not generate highlighted PDF for page {}: {}", page.getId(), e.getMessage());
            return null;
        }
    }

    private void createPendingNotificationsForMatch(ClientKeywordMatch match) {
        AppUser client = match.getClient();
        GazetteIssue issue = match.getGazetteIssue();
        GazettePage page = match.getGazettePage();

        String issueLabel = "Vol. " + issue.getVolumeNumber() + ", No. " + issue.getIssueNumber();
        String subject = "Possible Gazette notice: " + match.getKeywordText();

        if (StringUtils.hasText(client.getEmail())) {
            String emailBody = "Hello " + client.getFullName() + ",\n\n"
                    + "We found a notice in the Kenya Gazette (" + issueLabel + ", page " + page.getPageNumber()
                    + ") that may mention your watched keyword \"" + match.getKeywordText()
                    + "\" (Matched text: \"" + match.getMatchedText() + "\").\n\n"
                    + "Context excerpt:\n" + (match.getSnippetText() != null ? match.getSnippetText() : match.getMatchedText())
                    + "\n\nPlease review the Gazette page carefully.";

            ClientNotification emailNotification = ClientNotification.builder()
                    .client(client)
                    .clientKeywordMatch(match)
                    .channel(NotificationChannel.EMAIL)
                    .recipient(client.getEmail().trim())
                    .subject(subject)
                    .messageBody(emailBody)
                    .details("Page " + page.getPageNumber() + " (" + issueLabel + ") — " + match.getMatchPrecision() + " match (" + match.getSimilarityScore() + "%)")
                    .status(NotificationStatus.PENDING)
                    .attemptCount(0)
                    .build();

            clientNotificationRepository.save(emailNotification);
        }

        if (StringUtils.hasText(client.getPhone())) {
            String smsBody = "Dokezo Alert: Kenya Gazette " + issueLabel + " (Page " + page.getPageNumber()
                    + ") mentions \"" + match.getMatchedText() + "\" matching your keyword \""
                    + match.getKeywordText() + "\".";

            ClientNotification smsNotification = ClientNotification.builder()
                    .client(client)
                    .clientKeywordMatch(match)
                    .channel(NotificationChannel.SMS)
                    .recipient(client.getPhone().trim())
                    .subject(subject)
                    .messageBody(smsBody)
                    .details("SMS alert for Page " + page.getPageNumber() + " (" + issueLabel + ")")
                    .status(NotificationStatus.PENDING)
                    .attemptCount(0)
                    .build();

            clientNotificationRepository.save(smsNotification);
        }
    }

    private String extractExactMatchText(String ocrText, String keyword) {
        if (!StringUtils.hasText(ocrText) || !StringUtils.hasText(keyword)) {
            return keyword;
        }
        String lowerOcr = ocrText.toLowerCase();
        String lowerKw = keyword.toLowerCase();
        int idx = lowerOcr.indexOf(lowerKw);
        if (idx >= 0) {
            return ocrText.substring(idx, Math.min(ocrText.length(), idx + keyword.length()))
                    .replaceAll("\\s+", " ").trim();
        }
        return keyword;
    }

    private String extractBestFuzzyFragment(String ocrText, String keyword) {
        if (!StringUtils.hasText(ocrText) || !StringUtils.hasText(keyword)) {
            return keyword;
        }
        String normalizedOcr = ocrText.replaceAll("\\s+", " ").trim();
        String[] kwTokens = keyword.trim().split("\\s+");
        String[] ocrTokens = normalizedOcr.split(" ");
        int windowSize = Math.max(1, kwTokens.length);

        if (ocrTokens.length <= windowSize) {
            return normalizedOcr;
        }

        String lowerKw = keyword.toLowerCase().trim();
        String bestCandidate = keyword;
        double bestScore = -1.0;

        for (int i = 0; i <= ocrTokens.length - windowSize; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < windowSize; j++) {
                if (j > 0) sb.append(' ');
                sb.append(ocrTokens[i + j]);
            }
            String candidate = sb.toString();
            String cleaned = candidate.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "");
            if (cleaned.isEmpty()) {
                continue;
            }
            double sim = computeDiceTrigramSimilarity(lowerKw, cleaned.toLowerCase());
            if (sim > bestScore) {
                bestScore = sim;
                bestCandidate = cleaned;
            }
        }
        return bestCandidate;
    }

    private double computeDiceTrigramSimilarity(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        Set<String> trigramsA = buildTrigrams(a);
        Set<String> trigramsB = buildTrigrams(b);
        if (trigramsA.isEmpty() || trigramsB.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String t : trigramsA) {
            if (trigramsB.contains(t)) {
                intersection++;
            }
        }
        return (2.0 * intersection) / (trigramsA.size() + trigramsB.size());
    }

    private Set<String> buildTrigrams(String s) {
        String padded = "  " + s.toLowerCase() + " ";
        Set<String> set = new HashSet<>();
        for (int i = 0; i <= padded.length() - 3; i++) {
            set.add(padded.substring(i, i + 3));
        }
        return set;
    }

    private String extractSnippet(String ocrText, String matchedFragment) {
        if (!StringUtils.hasText(ocrText)) {
            return matchedFragment;
        }
        String normalized = ocrText.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(matchedFragment)) {
            return normalized.substring(0, Math.min(220, normalized.length()));
        }
        int idx = normalized.toLowerCase().indexOf(matchedFragment.toLowerCase());
        if (idx < 0) {
            return normalized.substring(0, Math.min(220, normalized.length()));
        }
        int start = Math.max(0, idx - 90);
        int end = Math.min(normalized.length(), idx + matchedFragment.length() + 90);
        String prefix = start > 0 ? "..." : "";
        String suffix = end < normalized.length() ? "..." : "";
        return prefix + normalized.substring(start, end).trim() + suffix;
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientKeywordMatch getMatchById(UUID id) throws AppException {
        ClientKeywordMatch match = clientKeywordMatchRepository.findById(id)
                .orElseThrow(() -> new AppException("Match not found."));
        boolean needsHighlightFile = match.getHighlightedPdfSystemFile() == null
                || match.getHighlightedPdfSystemFile().getOriginalFileName() == null
                || !match.getHighlightedPdfSystemFile().getOriginalFileName().endsWith("-v2.pdf");
        if (needsHighlightFile && match.getGazettePage() != null) {
            SystemFile generated = generateHighlightedPagePdf(
                    match.getGazetteIssue(),
                    match.getGazettePage(),
                    match.getMatchedText(),
                    match.getKeywordText()
            );
            if (generated != null) {
                match.setHighlightedPdfSystemFile(generated);
                match = clientKeywordMatchRepository.save(match);
            }
        }
        return match;
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientKeywordMatch approveMatch(UUID matchId) throws AppException {
        ClientKeywordMatch match = getMatchById(matchId);
        match.setStatus(ClientKeywordMatchStatus.APPROVED);
        match.setReviewedAt(LocalDateTime.now());
        match.setRejectionReason(null);
        return clientKeywordMatchRepository.save(match);
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientKeywordMatch rejectMatch(UUID matchId, String reason, String notes) throws AppException {
        ClientKeywordMatch match = getMatchById(matchId);
        match.setStatus(ClientKeywordMatchStatus.REJECTED);
        match.setReviewedAt(LocalDateTime.now());
        match.setRejectionReason(StringUtils.hasText(reason) ? reason.trim() : "not_same_person");
        match.setReviewNotes(StringUtils.hasText(notes) ? notes.trim() : null);
        return clientKeywordMatchRepository.save(match);
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public void bulkReviewMatches(List<UUID> matchIds, String action) throws AppException {
        if (matchIds == null || matchIds.isEmpty()) {
            throw new AppException("Please select at least one match.");
        }
        boolean approve = "approve".equalsIgnoreCase(action);
        for (UUID id : matchIds) {
            if (approve) {
                approveMatch(id);
            } else {
                rejectMatch(id, "bulk_rejected", "Rejected via bulk action");
            }
        }
    }

    @Override
    public Page<ClientKeywordMatch> searchMatches(String status,
                                                  UUID issueId,
                                                  String clientQuery,
                                                  String keywordType,
                                                  String matchMode,
                                                  int page,
                                                  int size) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? size : 10;

        Specification<ClientKeywordMatch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
                try {
                    ClientKeywordMatchStatus parsedStatus = ClientKeywordMatchStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), parsedStatus));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (issueId != null) {
                predicates.add(cb.equal(root.get("gazetteIssue").get("id"), issueId));
            }

            if (StringUtils.hasText(clientQuery)) {
                String pattern = "%" + clientQuery.trim().toLowerCase() + "%";
                Join<ClientKeywordMatch, AppUser> clientJoin = root.join("client", JoinType.LEFT);
                Predicate namePred = cb.like(cb.lower(cb.coalesce(clientJoin.get("fullName"), "")), pattern);
                Predicate emailPred = cb.like(cb.lower(cb.coalesce(clientJoin.get("email"), "")), pattern);
                Predicate phonePred = cb.like(cb.lower(cb.coalesce(clientJoin.get("phone"), "")), pattern);
                predicates.add(cb.or(namePred, emailPred, phonePred));
            }

            if (StringUtils.hasText(keywordType)) {
                try {
                    KeywordType parsedType = KeywordType.valueOf(keywordType.trim().toUpperCase());
                    Join<ClientKeywordMatch, ClientKeyword> kwJoin = root.join("clientKeyword", JoinType.LEFT);
                    predicates.add(cb.equal(kwJoin.get("keywordType"), parsedType));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (StringUtils.hasText(matchMode)) {
                predicates.add(cb.equal(cb.upper(root.get("matchPrecision")), matchMode.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return clientKeywordMatchRepository.findAll(spec, pageRequest);
    }

    @Override
    public Page<ClientKeywordMatch> searchClientMatches(UUID clientId,
                                                        String status,
                                                        UUID keywordId,
                                                        int page,
                                                        int size) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? size : 10;

        Specification<ClientKeywordMatch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.equal(root.get("client").get("id"), clientId));

            if (StringUtils.hasText(status)) {
                try {
                    ClientKeywordMatchStatus parsedStatus = ClientKeywordMatchStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), parsedStatus));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (keywordId != null) {
                predicates.add(cb.equal(root.get("clientKeyword").get("id"), keywordId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return clientKeywordMatchRepository.findAll(spec, pageRequest);
    }

    @Override
    public Page<ClientNotification> searchClientNotifications(UUID clientId,
                                                              String channel,
                                                              String status,
                                                              int page,
                                                              int size) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? size : 10;

        Specification<ClientNotification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.equal(root.get("client").get("id"), clientId));

            if (StringUtils.hasText(channel)) {
                try {
                    NotificationChannel parsedChannel = NotificationChannel.valueOf(channel.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("channel"), parsedChannel));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (StringUtils.hasText(status)) {
                try {
                    NotificationStatus parsedStatus = NotificationStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), parsedStatus));
                } catch (IllegalArgumentException ignored) {
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return clientNotificationRepository.findAll(spec, pageRequest);
    }

    @Override
    public List<ClientNotification> getNotificationsForMatch(UUID matchId) {
        return clientNotificationRepository.findAllByClientKeywordMatchIdAndIsActiveTrueOrderByCreatedAtDesc(matchId);
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientNotification resendNotification(UUID notificationId) throws AppException {
        ClientNotification notification = clientNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException("Notification not found."));
        notification.setStatus(NotificationStatus.PENDING);
        notification.setFailureMessage(null);
        return clientNotificationRepository.save(notification);
    }

    @Override
    public long countAllMatches() {
        return clientKeywordMatchRepository.countByIsActiveTrue();
    }

    @Override
    public long countMatchesByStatus(ClientKeywordMatchStatus status) {
        return clientKeywordMatchRepository.countByStatusAndIsActiveTrue(status);
    }

    @Override
    public long countClientMatches(UUID clientId) {
        return clientKeywordMatchRepository.countByClientIdAndIsActiveTrue(clientId);
    }

    @Override
    public long countClientMatchesByStatus(UUID clientId, ClientKeywordMatchStatus status) {
        return clientKeywordMatchRepository.countByClientIdAndStatusAndIsActiveTrue(clientId, status);
    }

    @Override
    public long countClientNotifications(UUID clientId) {
        return clientNotificationRepository.countByClientIdAndIsActiveTrue(clientId);
    }

    @Override
    public long countClientNotificationsByChannel(UUID clientId, NotificationChannel channel) {
        return clientNotificationRepository.countByClientIdAndChannelAndIsActiveTrue(clientId, channel);
    }

    @Override
    public long countClientNotificationsByStatus(UUID clientId, NotificationStatus status) {
        return clientNotificationRepository.countByClientIdAndStatusAndIsActiveTrue(clientId, status);
    }

    @Override
    public Optional<ClientKeywordMatch> findPreviousPendingMatch(ClientKeywordMatch current) {
        if (current == null || current.getCreatedAt() == null) {
            return Optional.empty();
        }
        return clientKeywordMatchRepository.findFirstByStatusAndCreatedAtBeforeAndIsActiveTrueOrderByCreatedAtDesc(
                ClientKeywordMatchStatus.PENDING_REVIEW, current.getCreatedAt());
    }

    @Override
    public Optional<ClientKeywordMatch> findNextPendingMatch(ClientKeywordMatch current) {
        if (current == null || current.getCreatedAt() == null) {
            return Optional.empty();
        }
        return clientKeywordMatchRepository.findFirstByStatusAndCreatedAtAfterAndIsActiveTrueOrderByCreatedAtAsc(
                ClientKeywordMatchStatus.PENDING_REVIEW, current.getCreatedAt());
    }

    @Override
    public List<ClientKeywordMatch> findOtherMatchesForClient(UUID clientId, UUID excludeMatchId) {
        return clientKeywordMatchRepository.findTop5ByClientIdAndIdNotAndIsActiveTrueOrderByCreatedAtDesc(
                clientId, excludeMatchId);
    }
}
