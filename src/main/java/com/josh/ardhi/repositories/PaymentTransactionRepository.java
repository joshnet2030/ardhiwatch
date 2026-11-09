package com.josh.ardhi.repositories;

import com.josh.ardhi.models.PaymentTransaction;
import com.josh.ardhi.models.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>, JpaSpecificationExecutor<PaymentTransaction> {

    List<PaymentTransaction> findAllByClientIdAndIsActiveTrueOrderByCreatedAtDesc(UUID clientId);

    List<PaymentTransaction> findAllBySubscriptionPlanIdAndIsActiveTrueOrderByCreatedAtDesc(UUID subscriptionPlanId);

    long countByClientIdAndIsActiveTrue(UUID clientId);

    long countByStatusAndIsActiveTrue(PaymentTransactionStatus status);

    long countByStatusAndIsActiveTrueAndCreatedAtGreaterThanEqual(PaymentTransactionStatus status, LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.isActive = TRUE AND t.client.id = :clientId AND t.status = com.josh.ardhi.models.PaymentTransactionStatus.COMPLETED")
    BigDecimal sumCompletedByClient(@Param("clientId") UUID clientId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.isActive = TRUE AND t.status = :status")
    BigDecimal sumByStatus(@Param("status") PaymentTransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.isActive = TRUE AND t.status = :status AND t.createdAt >= :since")
    BigDecimal sumByStatusSince(@Param("status") PaymentTransactionStatus status, @Param("since") LocalDateTime since);

}
