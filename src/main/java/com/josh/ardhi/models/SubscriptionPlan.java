package com.josh.ardhi.models;

import com.josh.ardhi.models.base.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends Auditable {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "price_kes", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceKes;

    @Column(name = "billing_period", nullable = false)
    @Builder.Default
    private String billingPeriod = "Annual";

    @Column(name = "keyword_limit", nullable = false)
    @Builder.Default
    private Integer keywordLimit = 1;

}
