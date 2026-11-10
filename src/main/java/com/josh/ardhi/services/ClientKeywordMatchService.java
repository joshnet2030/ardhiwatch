package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.models.ClientKeywordMatch;
import com.josh.ardhi.models.ClientKeywordMatchStatus;
import com.josh.ardhi.models.ClientNotification;
import com.josh.ardhi.models.NotificationChannel;
import com.josh.ardhi.models.NotificationStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientKeywordMatchService {

    void matchGazetteIssue(UUID gazetteIssueId) throws AppException;

    ClientKeywordMatch getMatchById(UUID id) throws AppException;

    ClientKeywordMatch approveMatch(UUID matchId) throws AppException;

    ClientKeywordMatch rejectMatch(UUID matchId, String reason, String notes) throws AppException;

    void bulkReviewMatches(List<UUID> matchIds, String action) throws AppException;

    Page<ClientKeywordMatch> searchMatches(String status,
                                           UUID issueId,
                                           String clientQuery,
                                           String keywordType,
                                           String matchMode,
                                           int page,
                                           int size);

    Page<ClientKeywordMatch> searchClientMatches(UUID clientId,
                                                 String status,
                                                 UUID keywordId,
                                                 int page,
                                                 int size);

    Page<ClientNotification> searchClientNotifications(UUID clientId,
                                                       String channel,
                                                       String status,
                                                       int page,
                                                       int size);

    List<ClientNotification> getNotificationsForMatch(UUID matchId);

    ClientNotification resendNotification(UUID notificationId) throws AppException;

    long countAllMatches();

    long countMatchesByStatus(ClientKeywordMatchStatus status);

    long countClientMatches(UUID clientId);

    long countClientMatchesByStatus(UUID clientId, ClientKeywordMatchStatus status);

    long countClientNotifications(UUID clientId);

    long countClientNotificationsByChannel(UUID clientId, NotificationChannel channel);

    long countClientNotificationsByStatus(UUID clientId, NotificationStatus status);

    Optional<ClientKeywordMatch> findPreviousPendingMatch(ClientKeywordMatch current);

    Optional<ClientKeywordMatch> findNextPendingMatch(ClientKeywordMatch current);

    List<ClientKeywordMatch> findOtherMatchesForClient(UUID clientId, UUID excludeMatchId);

}
