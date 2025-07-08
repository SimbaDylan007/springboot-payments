package com.yourcompany.payments.dto.school;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder // Using Builder pattern for easy construction
public class SchoolValidationResponse {
    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("student_name")
    private String studentName;

    private String grade;
    private String term;

    @JsonProperty("fee_amount")
    private double feeAmount;

    private String currency;

    @JsonProperty("outstanding_balance")
    private double outstandingBalance;

    private boolean successful;
    private String error;

    @JsonProperty("schoolName")
    private String schoolName;

    @JsonProperty("accountNumber")
    private String accountNumber;
}