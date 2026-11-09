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
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends Auditable {

    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private AppUser client;

    @ManyToOne
    @JoinColumn(name = "client_subscription_plan_id")
    private ClientSubscriptionPlan clientSubscriptionPlan;

    @ManyToOne
    @JoinColumn(name = "subscription_plan_id")
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.MPESA;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "paid_from")
    private String paidFrom;

    @Column(name = "reference")
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentTransactionStatus status = PaymentTransactionStatus.COMPLETED;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "admin_notes", columnDefinition = "text")
    private String adminNotes;

}
