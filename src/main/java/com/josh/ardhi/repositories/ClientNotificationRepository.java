package com.josh.ardhi.repositories;

import com.josh.ardhi.models.ClientNotification;
import com.josh.ardhi.models.NotificationChannel;
import com.josh.ardhi.models.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientNotificationRepository extends JpaRepository<ClientNotification, UUID>, JpaSpecificationExecutor<ClientNotification> {

    List<ClientNotification> findAllByClientIdAndIsActiveTrueOrderByCreatedAtDesc(UUID clientId);

    List<ClientNotification> findAllByClientKeywordMatchIdAndIsActiveTrueOrderByCreatedAtDesc(UUID matchId);

    long countByClientIdAndIsActiveTrue(UUID clientId);

    long countByClientIdAndChannelAndIsActiveTrue(UUID clientId, NotificationChannel channel);

    long countByClientIdAndStatusAndIsActiveTrue(UUID clientId, NotificationStatus status);

    long countByClientKeywordMatchIdAndIsActiveTrue(UUID matchId);

    List<ClientNotification> findAllByStatusAndIsActiveTrueOrderByCreatedAtAsc(NotificationStatus status);

}
