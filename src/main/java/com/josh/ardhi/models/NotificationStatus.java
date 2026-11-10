package com.josh.ardhi.models;

import lombok.Getter;

@Getter
public enum NotificationStatus {
    PENDING("Pending"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    FAILED("Failed");

    private final String label;

    NotificationStatus(String label) {
        this.label = label;
    }
}
