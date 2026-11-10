package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.CreateClientSubscriptionForm;
import com.josh.ardhi.forms.RecordPaymentForm;
import com.josh.ardhi.models.ClientSubscriptionPlan;
import com.josh.ardhi.models.PaymentTransaction;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientSubscriptionService {

    List<ClientSubscriptionPlan> getClientSubscriptions(UUID clientId);

    Optional<ClientSubscriptionPlan> getCurrentSubscriptionForClient(UUID clientId);

    long countClientSubscriptions(UUID clientId);

    long countActiveValidSubscriptions();

    ClientSubscriptionPlan getSubscriptionById(UUID id) throws AppException;

    ClientSubscriptionPlan createSubscription(UUID clientId, CreateClientSubscriptionForm form) throws AppException;

    LocalDate getNextAvailableStartDate(UUID clientId);

    ClientSubscriptionPlan cancelSubscription(UUID clientId, UUID subscriptionId, String reason) throws AppException;

    List<PaymentTransaction> getClientTransactions(UUID clientId);

    long countClientTransactions(UUID clientId);

    BigDecimal getTotalPaidByClient(UUID clientId);

    PaymentTransaction getTransactionById(UUID transactionId) throws AppException;

    PaymentTransaction recordPayment(RecordPaymentForm form) throws AppException;

    PaymentTransaction reconcileTransaction(UUID transactionId, String receiptNumber) throws AppException;

    PaymentTransaction refundTransaction(UUID transactionId, String reason) throws AppException;

    PaymentTransaction updateTransactionNotes(UUID transactionId, String adminNotes) throws AppException;

    Page<PaymentTransaction> searchTransactions(String q,
                                                String status,
                                                String method,
                                                LocalDate from,
                                                LocalDate to,
                                                int page,
                                                int size);

    BigDecimal getCollectedThisMonth();

    BigDecimal getPendingTotal();

    long getPendingCount();

    long getFailedThisMonthCount();

    BigDecimal getRefundedThisMonth();

}
