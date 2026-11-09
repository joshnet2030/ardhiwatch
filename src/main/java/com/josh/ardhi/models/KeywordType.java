package com.josh.ardhi.models;

import lombok.Getter;

@Getter
public enum KeywordType {
    TITLE("Title / LR Number", "EXACT"),
    NAME("Name", "FUZZY"),
    NATIONAL_ID("National ID", "EXACT"),
    PLOT_NUMBER("Plot Number", "EXACT"),
    OTHER("Other", "EXACT");

    private final String label;
    private final String defaultPrecision;

    KeywordType(String label, String defaultPrecision) {
        this.label = label;
        this.defaultPrecision = defaultPrecision;
    }
}
