package com.yourcompany.payments.exception;

public class PaymentFailedException extends RuntimeException {

    private final boolean isServiceUnavailable;

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
        // Determine if this is a temporary service availability issue (e.g., timeout, connection error)
        // or a permanent client-side error (e.g., invalid account).
        // This is a simple check; a more robust implementation might inspect the cause's type.
        String causeMessage = cause.getMessage().toLowerCase();
        this.isServiceUnavailable = causeMessage.contains("timeout") || causeMessage.contains("connection");
    }

    public boolean isServiceUnavailable() {
        return isServiceUnavailable;
    }
}