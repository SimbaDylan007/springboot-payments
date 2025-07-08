package com.yourcompany.payments.dto.school;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SchoolType {
    @JsonProperty("university")
    UNIVERSITY,

    @JsonProperty("primary_secondary")
    PRIMARY_SECONDARY
}