package com.josh.ardhi.forms;

import com.josh.ardhi.models.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateClientSubscriptionForm {

    @NotNull(message = "Please select a subscription plan")
    private UUID subscriptionPlanId;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = LocalDate.now();

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial payment cannot be negative")
    private BigDecimal initialPaidAmount = BigDecimal.ZERO;

    private PaymentMethod paymentMethod = PaymentMethod.MPESA;

    private String receiptNumber;

    private String paidFrom;

    private boolean useWalletBalance = true;

}
