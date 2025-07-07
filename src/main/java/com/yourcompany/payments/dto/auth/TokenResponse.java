package com.yourcompany.payments.dto.auth;

public record TokenResponse(
        String accessToken,
        String tokenType
) {}