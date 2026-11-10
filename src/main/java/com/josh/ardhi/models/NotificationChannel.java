package com.josh.ardhi.models;

import lombok.Getter;

@Getter
public enum NotificationChannel {
    EMAIL("Email"),
    SMS("SMS");

    private final String label;

    NotificationChannel(String label) {
        this.label = label;
    }
}
