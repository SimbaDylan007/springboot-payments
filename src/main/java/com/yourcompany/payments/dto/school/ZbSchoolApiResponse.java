package com.yourcompany.payments.dto.school;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data // Lombok annotation for getters, setters, etc.
public class ZbSchoolApiResponse {

    @JsonProperty("responseCode")
    private String responseCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("body")
    private Body body;

    @Data
    public static class Body {
        @JsonProperty("accountNumber")
        private String accountNumber;

        @JsonProperty("accountName")
        private String accountName;

        @JsonProperty("currency")
        private String currency;
    }
}