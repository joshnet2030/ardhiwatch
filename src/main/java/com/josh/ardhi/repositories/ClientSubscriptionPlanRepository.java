package com.josh.ardhi.repositories;

import com.josh.ardhi.models.ClientSubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClientSubscriptionPlanRepository extends JpaRepository<ClientSubscriptionPlan, UUID> {

    List<ClientSubscriptionPlan> findAllByClientIdAndIsActiveTrueOrderByStartDateDescCreatedAtDesc(UUID clientId);

    List<ClientSubscriptionPlan> findAllByClientIdAndIsActiveTrueAndBalanceGreaterThanOrderByStartDateAscCreatedAtAsc(UUID clientId, BigDecimal balance);

    long countByClientIdAndIsActiveTrue(UUID clientId);

    List<ClientSubscriptionPlan> findAllBySubscriptionPlanIdAndIsActiveTrueOrderByStartDateDesc(UUID subscriptionPlanId);

}
