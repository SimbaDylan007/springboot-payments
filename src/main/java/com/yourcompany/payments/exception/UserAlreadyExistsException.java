package com.yourcompany.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception thrown when an attempt is made to register a user
 * with an email that already exists in the system.
 *
 * The @ResponseStatus annotation tells Spring to automatically return an
 * HTTP 409 Conflict status code whenever this exception is thrown from a controller.
 */
@ResponseStatus(HttpStatus.CONFLICT) // This automatically sets the HTTP response code
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}