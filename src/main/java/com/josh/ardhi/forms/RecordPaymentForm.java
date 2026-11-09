package com.josh.ardhi.forms;

import com.josh.ardhi.models.PaymentMethod;
import com.josh.ardhi.models.PaymentTransactionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RecordPaymentForm {

    private UUID clientId;

    private UUID clientSubscriptionPlanId;

    private UUID subscriptionPlanId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod = PaymentMethod.MPESA;

    private String receiptNumber;

    private String paidFrom;

    @NotNull(message = "Status is required")
    private PaymentTransactionStatus status = PaymentTransactionStatus.COMPLETED;

    private String adminNotes;

}
