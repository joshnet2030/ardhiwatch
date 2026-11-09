package com.josh.ardhi.repositories;

import com.josh.ardhi.models.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    List<SubscriptionPlan> findAllByOrderByIsActiveDescPriceKesAscCreatedAtDesc();

    Optional<SubscriptionPlan> findFirstByNameIgnoreCase(String name);

    long countByIsActiveTrue();

    long countByIsActiveFalse();

}
