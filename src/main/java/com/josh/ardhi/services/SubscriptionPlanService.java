package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.SubscriptionPlanForm;
import com.josh.ardhi.models.SubscriptionPlan;

import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanService {

    List<SubscriptionPlan> getAllPlans();

    SubscriptionPlan getById(UUID id) throws AppException;

    SubscriptionPlan createPlan(SubscriptionPlanForm form) throws AppException;

    SubscriptionPlan updatePlan(UUID id, SubscriptionPlanForm form) throws AppException;

    SubscriptionPlan updatePlanStatus(UUID id, boolean active, String deactivationMemo) throws AppException;

    long countActivePlans();

    long countRetiredPlans();

}
