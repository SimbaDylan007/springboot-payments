package com.yourcompany.payments.dto.nyaradzo;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PolicyStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    LAPSED("LAPSED"),
    PAID_UP("PAID UP"), // Handles the space in the value
    UNKNOWN("Unknown");

    private final String value;

    PolicyStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}