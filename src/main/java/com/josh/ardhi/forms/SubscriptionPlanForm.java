package com.josh.ardhi.forms;

import com.josh.ardhi.models.SubscriptionPlan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPlanForm {

    private UUID id;

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Price (KES) is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be 0 or greater")
    private BigDecimal priceKes;

    @NotBlank(message = "Billing period is required")
    private String billingPeriod = "Annual";

    @NotNull(message = "Keyword limit is required")
    @Min(value = 1, message = "Keyword limit must be at least 1")
    private Integer keywordLimit = 1;

    private boolean active = true;

    public static SubscriptionPlanForm fromEntity(SubscriptionPlan plan) {
        SubscriptionPlanForm form = new SubscriptionPlanForm();
        form.setId(plan.getId());
        form.setName(plan.getName());
        form.setDescription(plan.getDescription());
        form.setPriceKes(plan.getPriceKes());
        form.setBillingPeriod(plan.getBillingPeriod());
        form.setKeywordLimit(plan.getKeywordLimit());
        form.setActive(plan.isActive());
        return form;
    }

}
