package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.SubscriptionPlanForm;
import com.josh.ardhi.models.SubscriptionPlan;
import com.josh.ardhi.repositories.SubscriptionPlanRepository;
import com.josh.ardhi.services.SubscriptionPlanService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanServiceImpl(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Override
    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAllByOrderByIsActiveDescPriceKesAscCreatedAtDesc();
    }

    @Override
    public SubscriptionPlan getById(UUID id) throws AppException {
        return subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new AppException("Subscription plan not found."));
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public SubscriptionPlan createPlan(SubscriptionPlanForm form) throws AppException {
        if (form == null || !StringUtils.hasText(form.getName())) {
            throw new AppException("Plan name is required.");
        }

        String trimmedName = form.getName().trim();
        Optional<SubscriptionPlan> existing = subscriptionPlanRepository.findFirstByNameIgnoreCase(trimmedName);
        if (existing.isPresent()) {
            throw new AppException("A subscription plan named '" + trimmedName + "' already exists.");
        }

        try {
            SubscriptionPlan plan = SubscriptionPlan.builder()
                    .name(trimmedName)
                    .description(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null)
                    .priceKes(form.getPriceKes())
                    .billingPeriod(form.getBillingPeriod().trim())
                    .keywordLimit(form.getKeywordLimit())
                    .build();
            plan.setActive(form.isActive());
            return subscriptionPlanRepository.save(plan);
        } catch (Exception e) {
            throw new AppException("Failed to create subscription plan: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public SubscriptionPlan updatePlan(UUID id, SubscriptionPlanForm form) throws AppException {
        SubscriptionPlan plan = getById(id);

        if (form == null || !StringUtils.hasText(form.getName())) {
            throw new AppException("Plan name is required.");
        }

        String trimmedName = form.getName().trim();
        Optional<SubscriptionPlan> existing = subscriptionPlanRepository.findFirstByNameIgnoreCase(trimmedName);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new AppException("Another subscription plan named '" + trimmedName + "' already exists.");
        }

        try {
            plan.setName(trimmedName);
            plan.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
            plan.setPriceKes(form.getPriceKes());
            plan.setBillingPeriod(form.getBillingPeriod().trim());
            plan.setKeywordLimit(form.getKeywordLimit());
            return subscriptionPlanRepository.save(plan);
        } catch (Exception e) {
            throw new AppException("Failed to update subscription plan: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public SubscriptionPlan updatePlanStatus(UUID id, boolean active, String deactivationMemo) throws AppException {
        SubscriptionPlan plan = getById(id);
        try {
            plan.setActive(active);
            if (!active) {
                plan.setDeactivationMemo(StringUtils.hasText(deactivationMemo)
                        ? deactivationMemo.trim()
                        : "Retired from sale by admin");
            } else {
                plan.setDeactivationMemo(null);
            }
            return subscriptionPlanRepository.save(plan);
        } catch (Exception e) {
            throw new AppException("Failed to update plan status: " + e.getMessage());
        }
    }

    @Override
    public long countActivePlans() {
        return subscriptionPlanRepository.countByIsActiveTrue();
    }

    @Override
    public long countRetiredPlans() {
        return subscriptionPlanRepository.countByIsActiveFalse();
    }
}
