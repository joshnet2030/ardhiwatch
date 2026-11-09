package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "client_subscription_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSubscriptionPlan extends Auditable {

    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private AppUser client;

    @ManyToOne
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ClientSubscriptionStatus status = ClientSubscriptionStatus.PENDING_PAYMENT;

    public boolean hasStarted() {
        return startDate != null && !startDate.isAfter(LocalDate.now());
    }

    public boolean isValid() {
        return isActive()
                && balance != null
                && balance.compareTo(BigDecimal.ZERO) == 0
                && status != ClientSubscriptionStatus.CANCELLED
                && (endDate == null || !endDate.isBefore(LocalDate.now()));
    }

    public long getDaysLeft() {
        if (endDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return Math.max(0, days);
    }

}
