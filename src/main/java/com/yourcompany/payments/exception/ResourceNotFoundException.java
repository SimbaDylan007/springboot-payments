package com.yourcompany.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when a requested resource is not found in the system.
 *
 * This exception is typically used in the service layer when a database query
 * for a specific entity by its ID or a unique key returns no result.
 *
 * The @ResponseStatus annotation automatically configures Spring to return an
 * HTTP 404 Not Found status code whenever this exception is thrown and not
 * handled by a more specific handler.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}