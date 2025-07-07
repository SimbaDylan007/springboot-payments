package com.yourcompany.payments.dto.fml;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FmlValidationResponse(
        @JsonProperty("customerAccount") String policyNumber,
        @JsonProperty("policy_holder") String policyHolder,
        @JsonProperty("policy_status") String policyStatus,
        boolean successful,
        String error
) {}