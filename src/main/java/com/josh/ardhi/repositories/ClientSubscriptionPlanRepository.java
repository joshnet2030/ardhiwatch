package com.josh.ardhi.repositories;

import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.ClientSubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClientSubscriptionPlanRepository extends JpaRepository<ClientSubscriptionPlan, UUID> {

    List<ClientSubscriptionPlan> findAllByClientIdAndIsActiveTrueOrderByStartDateDescCreatedAtDesc(UUID clientId);

    List<ClientSubscriptionPlan> findAllByClientIdAndIsActiveTrueAndBalanceGreaterThanOrderByStartDateAscCreatedAtAsc(UUID clientId, BigDecimal balance);

    long countByClientIdAndIsActiveTrue(UUID clientId);

    List<ClientSubscriptionPlan> findAllBySubscriptionPlanIdAndIsActiveTrueOrderByStartDateDesc(UUID subscriptionPlanId);

    @Query("SELECT DISTINCT cs.client FROM ClientSubscriptionPlan cs " +
            "WHERE cs.isActive = true " +
            "AND cs.client.isActive = true " +
            "AND cs.balance = 0 " +
            "AND cs.status <> com.josh.ardhi.models.ClientSubscriptionStatus.CANCELLED " +
            "AND cs.startDate <= :today " +
            "AND cs.endDate >= :today")
    List<AppUser> findClientsWithActiveValidSubscriptions(@Param("today") LocalDate today);

    @Query("SELECT COUNT(cs) FROM ClientSubscriptionPlan cs " +
            "WHERE cs.isActive = true " +
            "AND cs.client.isActive = true " +
            "AND cs.balance = 0 " +
            "AND cs.status <> com.josh.ardhi.models.ClientSubscriptionStatus.CANCELLED " +
            "AND cs.startDate <= :today " +
            "AND cs.endDate >= :today")
    long countActiveValidSubscriptions(@Param("today") LocalDate today);

}
