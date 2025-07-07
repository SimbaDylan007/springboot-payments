package com.yourcompany.payments.dto.zetdc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZetdcValidationResponse(
        @JsonProperty("customerAccount") String accountNumber,
        String customerName,
        String address,
        String currency,
        boolean successful,
        String error
) {}