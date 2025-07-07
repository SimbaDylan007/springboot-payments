package com.yourcompany.payments.dto;

import java.util.UUID;

/**
 * A Data Transfer Object representing the publicly safe information of a user.
 * This is typically returned by API endpoints like /register or /users/me.
 *
 * @param userUuid The unique public identifier for the user.
 * @param email The user's email address.
 * @param fullName The user's full name.
 * @param isActive A flag indicating if the user's account is active.
 */
public record UserDto(
        UUID userUuid,
        String email,
        String fullName,
        boolean isActive
) {}