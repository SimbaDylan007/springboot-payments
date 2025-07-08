package com.yourcompany.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for business logic failures during biller validation.
 * <p>
 * This exception should be thrown when a validation check with an external
 * biller service fails due to business rules, not technical errors.
 * Examples:
 * - The account number is not found.
 * - The policy status is inactive and cannot be paid.
 * - The payment amount submitted by the client does not match the amount due.
 * <p>
 * The {@link ResponseStatus} annotation tells Spring to automatically return an
 * HTTP 400 (Bad Request) status code whenever this exception is thrown from a controller
 * and not otherwise handled. This is appropriate as these are considered client-side
 * or request-data errors.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BillerValidationException extends RuntimeException {

    /**
     * Constructs a new BillerValidationException with the specified detail message.
     *
     * @param message the detail message.
     */
    public BillerValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new BillerValidationException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval by the getCause() method).
     */
    public BillerValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}