package com.yourcompany.payments.dto.school;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record SchoolValidationResponse(
        @JsonProperty("student_id") String studentId,
        @JsonProperty("student_name") String studentName,
        String grade,
        String term,
        @JsonProperty("fee_amount") BigDecimal feeAmount,
        String currency,
        @JsonProperty("outstanding_balance") BigDecimal outstandingBalance,
        boolean successful,
        String error,
        String schoolName,
        String accountNumber
) {}