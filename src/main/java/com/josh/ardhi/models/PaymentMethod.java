package com.josh.ardhi.models;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    MPESA("M-Pesa"),
    MANUAL("Manual"),
    CARD("Card");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }
}
