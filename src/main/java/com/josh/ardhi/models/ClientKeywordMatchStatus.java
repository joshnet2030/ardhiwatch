package com.josh.ardhi.models;

import lombok.Getter;

@Getter
public enum ClientKeywordMatchStatus {
    PENDING_REVIEW("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    ClientKeywordMatchStatus(String label) {
        this.label = label;
    }
}
