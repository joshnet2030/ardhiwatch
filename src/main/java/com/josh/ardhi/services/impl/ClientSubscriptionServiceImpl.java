package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.CreateClientSubscriptionForm;
import com.josh.ardhi.forms.RecordPaymentForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.ClientSubscriptionPlan;
import com.josh.ardhi.models.ClientSubscriptionStatus;
import com.josh.ardhi.models.PaymentMethod;
import com.josh.ardhi.models.PaymentTransaction;
import com.josh.ardhi.models.PaymentTransactionStatus;
import com.josh.ardhi.models.SubscriptionPlan;
import com.josh.ardhi.repositories.ClientSubscriptionPlanRepository;
import com.josh.ardhi.repositories.PaymentTransactionRepository;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.ClientSubscriptionService;
import com.josh.ardhi.services.SubscriptionPlanService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientSubscriptionServiceImpl implements ClientSubscriptionService {

    private final ClientSubscriptionPlanRepository clientSubscriptionPlanRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AppUserService appUserService;
    private final SubscriptionPlanService subscriptionPlanService;

    public ClientSubscriptionServiceImpl(ClientSubscriptionPlanRepository clientSubscriptionPlanRepository,
                                         PaymentTransactionRepository paymentTransactionRepository,
                                         AppUserService appUserService,
                                         SubscriptionPlanService subscriptionPlanService) {
        this.clientSubscriptionPlanRepository = clientSubscriptionPlanRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.appUserService = appUserService;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @Override
    public List<ClientSubscriptionPlan> getClientSubscriptions(UUID clientId) {
        List<ClientSubscriptionPlan> list = clientSubscriptionPlanRepository
                .findAllByClientIdAndIsActiveTrueOrderByStartDateDescCreatedAtDesc(clientId);
        LocalDate today = LocalDate.now();
        for (ClientSubscriptionPlan sub : list) {
            if (sub.getStatus() == ClientSubscriptionStatus.ACTIVE
                    && sub.getEndDate() != null
                    && sub.getEndDate().isBefore(today)) {
                sub.setStatus(ClientSubscriptionStatus.EXPIRED);
                clientSubscriptionPlanRepository.save(sub);
            }
        }
        return list;
    }

    @Override
    public Optional<ClientSubscriptionPlan> getCurrentSubscriptionForClient(UUID clientId) {
        List<ClientSubscriptionPlan> subs = getClientSubscriptions(clientId);
        Optional<ClientSubscriptionPlan> validActive = subs.stream()
                .filter(ClientSubscriptionPlan::isValid)
                .max(Comparator.comparing(ClientSubscriptionPlan::getEndDate));
        if (validActive.isPresent()) {
            return validActive;
        }
        return subs.stream()
                .filter(s -> s.getStatus() != ClientSubscriptionStatus.CANCELLED)
                .findFirst();
    }

    @Override
    public long countClientSubscriptions(UUID clientId) {
        return clientSubscriptionPlanRepository.countByClientIdAndIsActiveTrue(clientId);
    }

    @Override
    public ClientSubscriptionPlan getSubscriptionById(UUID id) throws AppException {
        return clientSubscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new AppException("Client subscription not found."));
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientSubscriptionPlan createSubscription(UUID clientId, CreateClientSubscriptionForm form) throws AppException {
        AppUser client = appUserService.getById(clientId);
        if (form.getSubscriptionPlanId() == null) {
            throw new AppException("Please select a subscription plan.");
        }
        SubscriptionPlan plan = subscriptionPlanService.getById(form.getSubscriptionPlanId());
        LocalDate startDate = form.getStartDate() != null ? form.getStartDate() : LocalDate.now();
        LocalDate endDate = calculateEndDate(startDate, plan.getBillingPeriod());

        List<ClientSubscriptionPlan> existingSubscriptions = getClientSubscriptions(clientId);
        for (ClientSubscriptionPlan existing : existingSubscriptions) {
            if (!existing.isActive()
                    || existing.getStatus() == ClientSubscriptionStatus.CANCELLED
                    || existing.getStatus() == ClientSubscriptionStatus.EXPIRED
                    || existing.getStartDate() == null
                    || existing.getEndDate() == null) {
                continue;
            }
            if (startDate.isBefore(existing.getEndDate()) && endDate.isAfter(existing.getStartDate())) {
                LocalDate nextAvailable = findNextAvailableStartDate(clientId);
                throw new AppException("Selected dates (" + startDate + " to " + endDate
                        + ") overlap with an existing subscription ("
                        + existing.getSubscriptionPlan().getName() + ": "
                        + existing.getStartDate() + " to " + existing.getEndDate()
                        + "). Next available start date is " + nextAvailable + ".");
            }
        }

        BigDecimal planAmount = plan.getPriceKes();
        ClientSubscriptionPlan clientSub = ClientSubscriptionPlan.builder()
                .client(client)
                .subscriptionPlan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .amount(planAmount)
                .paidAmount(BigDecimal.ZERO)
                .balance(planAmount)
                .status(ClientSubscriptionStatus.PENDING_PAYMENT)
                .build();

        clientSub = clientSubscriptionPlanRepository.saveAndFlush(clientSub);

        if (form.isUseWalletBalance() && client.getWalletBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal walletAvailable = client.getWalletBalance();
            allocateFundsToClientSubscriptions(client, clientSub, plan, walletAvailable);
            appUserService.deductFromWalletBalance(clientId, walletAvailable);
            clientSub = getSubscriptionById(clientSub.getId());
        }

        if (form.getInitialPaidAmount() != null && form.getInitialPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            RecordPaymentForm paymentForm = new RecordPaymentForm();
            paymentForm.setClientId(clientId);
            paymentForm.setClientSubscriptionPlanId(clientSub.getId());
            paymentForm.setSubscriptionPlanId(plan.getId());
            paymentForm.setAmount(form.getInitialPaidAmount());
            paymentForm.setPaymentMethod(form.getPaymentMethod() != null ? form.getPaymentMethod() : PaymentMethod.MPESA);
            paymentForm.setReceiptNumber(form.getReceiptNumber());
            paymentForm.setPaidFrom(StringUtils.hasText(form.getPaidFrom()) ? form.getPaidFrom() : client.getPhone());
            paymentForm.setStatus(PaymentTransactionStatus.COMPLETED);
            recordPayment(paymentForm);
        }

        return getSubscriptionById(clientSub.getId());
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientSubscriptionPlan cancelSubscription(UUID clientId, UUID subscriptionId, String reason) throws AppException {
        ClientSubscriptionPlan sub = getSubscriptionById(subscriptionId);
        if (!sub.getClient().getId().equals(clientId)) {
            throw new AppException("Subscription does not belong to this client.");
        }
        if (sub.getStatus() == ClientSubscriptionStatus.CANCELLED) {
            throw new AppException("Subscription is already cancelled.");
        }

        boolean alreadyStarted = sub.hasStarted();
        BigDecimal paidAmount = sub.getPaidAmount() != null ? sub.getPaidAmount() : BigDecimal.ZERO;

        if (!alreadyStarted && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            appUserService.addToWalletBalance(clientId, paidAmount);
            sub.setPaidAmount(BigDecimal.ZERO);
            sub.setBalance(sub.getAmount());
            String memo = StringUtils.hasText(reason) ? reason.trim() : "Cancelled before start date";
            sub.setDeactivationMemo(memo + " (Moved KES " + String.format("%,.2f", paidAmount) + " to wallet)");
        } else if (alreadyStarted) {
            String memo = StringUtils.hasText(reason) ? reason.trim() : "Cancelled after start date";
            sub.setDeactivationMemo(memo + " (No refund — subscription already started)");
        } else {
            sub.setDeactivationMemo(StringUtils.hasText(reason) ? reason.trim() : "Cancelled before start date");
        }

        sub.setStatus(ClientSubscriptionStatus.CANCELLED);
        return clientSubscriptionPlanRepository.save(sub);
    }

    @Override
    public List<PaymentTransaction> getClientTransactions(UUID clientId) {
        return paymentTransactionRepository.findAllByClientIdAndIsActiveTrueOrderByCreatedAtDesc(clientId);
    }

    @Override
    public long countClientTransactions(UUID clientId) {
        return paymentTransactionRepository.countByClientIdAndIsActiveTrue(clientId);
    }

    @Override
    public BigDecimal getTotalPaidByClient(UUID clientId) {
        BigDecimal sum = paymentTransactionRepository.sumCompletedByClient(clientId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public PaymentTransaction getTransactionById(UUID transactionId) throws AppException {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException("Payment transaction not found."));
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public PaymentTransaction recordPayment(RecordPaymentForm form) throws AppException {
        if (form.getClientId() == null) {
            throw new AppException("Please select a client.");
        }
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Payment amount must be greater than zero.");
        }

        AppUser client = appUserService.getById(form.getClientId());
        ClientSubscriptionPlan targetSub = null;
        SubscriptionPlan targetPlan = null;

        if (form.getClientSubscriptionPlanId() != null) {
            targetSub = getSubscriptionById(form.getClientSubscriptionPlanId());
            targetPlan = targetSub.getSubscriptionPlan();
        } else if (form.getSubscriptionPlanId() != null) {
            targetPlan = subscriptionPlanService.getById(form.getSubscriptionPlanId());
        } else {
            List<ClientSubscriptionPlan> unpaid = clientSubscriptionPlanRepository
                    .findAllByClientIdAndIsActiveTrueAndBalanceGreaterThanOrderByStartDateAscCreatedAtAsc(client.getId(), BigDecimal.ZERO)
                    .stream()
                    .filter(s -> s.getStatus() != ClientSubscriptionStatus.CANCELLED)
                    .toList();
            if (!unpaid.isEmpty()) {
                targetSub = unpaid.get(0);
                targetPlan = targetSub.getSubscriptionPlan();
            } else {
                List<ClientSubscriptionPlan> existing = clientSubscriptionPlanRepository
                        .findAllByClientIdAndIsActiveTrueOrderByStartDateDescCreatedAtDesc(client.getId());
                if (!existing.isEmpty()) {
                    targetPlan = existing.get(0).getSubscriptionPlan();
                } else {
                    List<SubscriptionPlan> allPlans = subscriptionPlanService.getAllPlans();
                    if (allPlans.isEmpty()) {
                        throw new AppException("No subscription plans exist. Please create a Subscription Plan first.");
                    }
                    targetPlan = allPlans.get(0);
                }
            }
        }

        PaymentTransactionStatus txStatus = form.getStatus() != null ? form.getStatus() : PaymentTransactionStatus.COMPLETED;
        String reference = "DKZ-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        if (txStatus == PaymentTransactionStatus.COMPLETED) {
            targetSub = allocateFundsToClientSubscriptions(client, targetSub, targetPlan, form.getAmount());
        } else if (targetSub == null && targetPlan != null) {
            LocalDate startDate = findNextAvailableStartDate(client.getId());
            LocalDate endDate = calculateEndDate(startDate, targetPlan.getBillingPeriod());
            targetSub = clientSubscriptionPlanRepository.saveAndFlush(ClientSubscriptionPlan.builder()
                    .client(client)
                    .subscriptionPlan(targetPlan)
                    .startDate(startDate)
                    .endDate(endDate)
                    .amount(targetPlan.getPriceKes())
                    .paidAmount(BigDecimal.ZERO)
                    .balance(targetPlan.getPriceKes())
                    .status(ClientSubscriptionStatus.PENDING_PAYMENT)
                    .build());
        }

        PaymentTransaction tx = PaymentTransaction.builder()
                .client(client)
                .clientSubscriptionPlan(targetSub)
                .subscriptionPlan(targetPlan != null ? targetPlan : (targetSub != null ? targetSub.getSubscriptionPlan() : null))
                .amount(form.getAmount())
                .paymentMethod(form.getPaymentMethod() != null ? form.getPaymentMethod() : PaymentMethod.MPESA)
                .receiptNumber(StringUtils.hasText(form.getReceiptNumber()) ? form.getReceiptNumber().trim().toUpperCase() : null)
                .paidFrom(StringUtils.hasText(form.getPaidFrom()) ? form.getPaidFrom().trim() : client.getPhone())
                .reference(reference)
                .status(txStatus)
                .completedAt(txStatus == PaymentTransactionStatus.COMPLETED ? LocalDateTime.now() : null)
                .adminNotes(StringUtils.hasText(form.getAdminNotes()) ? form.getAdminNotes().trim() : null)
                .build();

        return paymentTransactionRepository.save(tx);
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public PaymentTransaction reconcileTransaction(UUID transactionId, String receiptNumber) throws AppException {
        PaymentTransaction tx = getTransactionById(transactionId);
        if (tx.getStatus() == PaymentTransactionStatus.COMPLETED) {
            throw new AppException("Transaction is already marked as completed.");
        }

        SubscriptionPlan plan = tx.getSubscriptionPlan();
        if (plan == null && tx.getClientSubscriptionPlan() != null) {
            plan = tx.getClientSubscriptionPlan().getSubscriptionPlan();
        }
        if (plan == null) {
            List<SubscriptionPlan> plans = subscriptionPlanService.getAllPlans();
            if (plans.isEmpty()) {
                throw new AppException("Cannot reconcile transaction without a subscription plan.");
            }
            plan = plans.get(0);
        }

        ClientSubscriptionPlan primarySub = allocateFundsToClientSubscriptions(
                tx.getClient(),
                tx.getClientSubscriptionPlan(),
                plan,
                tx.getAmount()
        );

        tx.setClientSubscriptionPlan(primarySub);
        tx.setSubscriptionPlan(plan);
        tx.setStatus(PaymentTransactionStatus.COMPLETED);
        tx.setCompletedAt(LocalDateTime.now());
        if (StringUtils.hasText(receiptNumber)) {
            tx.setReceiptNumber(receiptNumber.trim().toUpperCase());
        }
        return paymentTransactionRepository.save(tx);
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public PaymentTransaction refundTransaction(UUID transactionId, String reason) throws AppException {
        PaymentTransaction tx = getTransactionById(transactionId);
        if (tx.getStatus() != PaymentTransactionStatus.COMPLETED) {
            throw new AppException("Only completed payments can be refunded.");
        }

        ClientSubscriptionPlan sub = tx.getClientSubscriptionPlan();
        if (sub != null) {
            BigDecimal newPaid = sub.getPaidAmount().subtract(tx.getAmount());
            if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
                newPaid = BigDecimal.ZERO;
            }
            BigDecimal newBalance = sub.getAmount().subtract(newPaid);
            sub.setPaidAmount(newPaid);
            sub.setBalance(newBalance);
            if (newPaid.compareTo(BigDecimal.ZERO) == 0) {
                sub.setStatus(ClientSubscriptionStatus.PENDING_PAYMENT);
            } else if (newBalance.compareTo(BigDecimal.ZERO) > 0) {
                sub.setStatus(ClientSubscriptionStatus.PARTIALLY_PAID);
            }
            clientSubscriptionPlanRepository.save(sub);
        }

        tx.setStatus(PaymentTransactionStatus.REFUNDED);
        String note = StringUtils.hasText(reason) ? "Refunded: " + reason.trim() : "Refunded by admin";
        if (StringUtils.hasText(tx.getAdminNotes())) {
            tx.setAdminNotes(tx.getAdminNotes() + "\n" + note);
        } else {
            tx.setAdminNotes(note);
        }
        return paymentTransactionRepository.save(tx);
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public PaymentTransaction updateTransactionNotes(UUID transactionId, String adminNotes) throws AppException {
        PaymentTransaction tx = getTransactionById(transactionId);
        tx.setAdminNotes(StringUtils.hasText(adminNotes) ? adminNotes.trim() : null);
        return paymentTransactionRepository.save(tx);
    }

    private ClientSubscriptionPlan allocateFundsToClientSubscriptions(AppUser client,
                                                                      ClientSubscriptionPlan preferredSub,
                                                                      SubscriptionPlan fallbackPlan,
                                                                      BigDecimal paymentAmount) {
        BigDecimal remaining = paymentAmount;
        ClientSubscriptionPlan firstAffected = null;
        SubscriptionPlan activePlanTemplate = fallbackPlan;

        // 1. Apply to preferred subscription first if it has a positive balance
        if (preferredSub != null
                && preferredSub.isActive()
                && preferredSub.getStatus() != ClientSubscriptionStatus.CANCELLED
                && preferredSub.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            activePlanTemplate = preferredSub.getSubscriptionPlan();
            BigDecimal applied = remaining.min(preferredSub.getBalance());
            applyPaymentToSubscription(preferredSub, applied);
            remaining = remaining.subtract(applied);
            firstAffected = preferredSub;
        }

        // 2. Apply any remaining amount to other existing unpaid/partially-paid subscriptions in chronological order
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            List<ClientSubscriptionPlan> unpaidSubs = clientSubscriptionPlanRepository
                    .findAllByClientIdAndIsActiveTrueAndBalanceGreaterThanOrderByStartDateAscCreatedAtAsc(client.getId(), BigDecimal.ZERO);

            for (ClientSubscriptionPlan sub : unpaidSubs) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (sub.getStatus() == ClientSubscriptionStatus.CANCELLED) {
                    continue;
                }
                if (preferredSub != null && sub.getId().equals(preferredSub.getId())) {
                    continue;
                }
                activePlanTemplate = sub.getSubscriptionPlan();
                BigDecimal applied = remaining.min(sub.getBalance());
                applyPaymentToSubscription(sub, applied);
                remaining = remaining.subtract(applied);
                if (firstAffected == null) {
                    firstAffected = sub;
                }
            }
        }

        // 3. Overpayment rollover: while remaining > 0, create a new ClientSubscriptionPlan starting where the last one ends
        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            LocalDate nextStart = findNextAvailableStartDate(client.getId());
            LocalDate nextEnd = calculateEndDate(nextStart, activePlanTemplate.getBillingPeriod());
            BigDecimal planPrice = activePlanTemplate.getPriceKes();

            ClientSubscriptionPlan rolloverSub = ClientSubscriptionPlan.builder()
                    .client(client)
                    .subscriptionPlan(activePlanTemplate)
                    .startDate(nextStart)
                    .endDate(nextEnd)
                    .amount(planPrice)
                    .paidAmount(BigDecimal.ZERO)
                    .balance(planPrice)
                    .status(ClientSubscriptionStatus.PENDING_PAYMENT)
                    .build();

            BigDecimal applied = remaining.min(planPrice);
            applyPaymentToSubscription(rolloverSub, applied);
            remaining = remaining.subtract(applied);

            if (firstAffected == null) {
                firstAffected = rolloverSub;
            }
        }

        return firstAffected != null ? firstAffected : preferredSub;
    }

    private void applyPaymentToSubscription(ClientSubscriptionPlan sub, BigDecimal appliedAmount) {
        BigDecimal updatedPaid = sub.getPaidAmount().add(appliedAmount);
        BigDecimal updatedBalance = sub.getAmount().subtract(updatedPaid);
        if (updatedBalance.compareTo(BigDecimal.ZERO) <= 0) {
            updatedBalance = BigDecimal.ZERO;
            sub.setStatus(ClientSubscriptionStatus.ACTIVE);
        } else if (updatedPaid.compareTo(BigDecimal.ZERO) > 0) {
            sub.setStatus(ClientSubscriptionStatus.PARTIALLY_PAID);
        } else {
            sub.setStatus(ClientSubscriptionStatus.PENDING_PAYMENT);
        }
        sub.setPaidAmount(updatedPaid);
        sub.setBalance(updatedBalance);
        clientSubscriptionPlanRepository.saveAndFlush(sub);
    }

    @Override
    public LocalDate getNextAvailableStartDate(UUID clientId) {
        return findNextAvailableStartDate(clientId);
    }

    private LocalDate findNextAvailableStartDate(UUID clientId) {
        List<ClientSubscriptionPlan> existing = clientSubscriptionPlanRepository
                .findAllByClientIdAndIsActiveTrueOrderByStartDateDescCreatedAtDesc(clientId);
        LocalDate latestEnd = existing.stream()
                .filter(s -> s.getStatus() != ClientSubscriptionStatus.CANCELLED && s.getEndDate() != null)
                .map(ClientSubscriptionPlan::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        LocalDate today = LocalDate.now();
        if (latestEnd != null && !latestEnd.isBefore(today)) {
            return latestEnd;
        }
        return today;
    }

    private LocalDate calculateEndDate(LocalDate startDate, String billingPeriod) {
        if (billingPeriod == null) {
            return startDate.plusYears(1);
        }
        return switch (billingPeriod.trim().toLowerCase()) {
            case "monthly" -> startDate.plusMonths(1);
            case "quarterly" -> startDate.plusMonths(3);
            default -> startDate.plusYears(1);
        };
    }

    @Override
    public Page<PaymentTransaction> searchTransactions(String q,
                                                       String status,
                                                       String method,
                                                       LocalDate from,
                                                       LocalDate to,
                                                       int page,
                                                       int size) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? size : 10;

        Specification<PaymentTransaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Join<PaymentTransaction, AppUser> clientJoin = root.join("client", JoinType.LEFT);
                Predicate receiptMatch = cb.like(cb.lower(cb.coalesce(root.get("receiptNumber"), "")), pattern);
                Predicate refMatch = cb.like(cb.lower(cb.coalesce(root.get("reference"), "")), pattern);
                Predicate clientNameMatch = cb.like(cb.lower(cb.coalesce(clientJoin.get("fullName"), "")), pattern);
                Predicate clientEmailMatch = cb.like(cb.lower(cb.coalesce(clientJoin.get("email"), "")), pattern);
                predicates.add(cb.or(receiptMatch, refMatch, clientNameMatch, clientEmailMatch));
            }

            if (StringUtils.hasText(status)) {
                try {
                    PaymentTransactionStatus parsedStatus = PaymentTransactionStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), parsedStatus));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (StringUtils.hasText(method)) {
                try {
                    PaymentMethod parsedMethod = PaymentMethod.valueOf(method.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("paymentMethod"), parsedMethod));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentTransactionRepository.findAll(spec, pageRequest);
    }

    @Override
    public BigDecimal getCollectedThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return paymentTransactionRepository.sumByStatusSince(PaymentTransactionStatus.COMPLETED, startOfMonth);
    }

    @Override
    public BigDecimal getPendingTotal() {
        return paymentTransactionRepository.sumByStatus(PaymentTransactionStatus.PENDING);
    }

    @Override
    public long getPendingCount() {
        return paymentTransactionRepository.countByStatusAndIsActiveTrue(PaymentTransactionStatus.PENDING);
    }

    @Override
    public long getFailedThisMonthCount() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return paymentTransactionRepository.countByStatusAndIsActiveTrueAndCreatedAtGreaterThanEqual(
                PaymentTransactionStatus.FAILED, startOfMonth);
    }

    @Override
    public BigDecimal getRefundedThisMonth() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return paymentTransactionRepository.sumByStatusSince(PaymentTransactionStatus.REFUNDED, startOfMonth);
    }
}
