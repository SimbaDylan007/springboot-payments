package com.yourcompany.payments.converters;

import com.yourcompany.payments.dto.school.SchoolType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Converter to convert String request parameters
 * into the SchoolType enum, handling case-insensitivity or specific mappings.
 */
@Component // Mark as a Spring component for auto-detection and registration
public class StringToSchoolTypeConverter implements Converter<String, SchoolType> {

    @Override
    public SchoolType convert(String source) {
        if (source == null || source.isBlank()) {
            // Depending on your requirements, you might return null,
            // or throw an IllegalArgumentException if an empty/null source is invalid.
            return null;
        }

        // Try to convert by uppercasing the source to match enum constants directly
        try {
            return SchoolType.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If the direct uppercase conversion fails, try specific mappings (e.g., if you
            // expect "univ" and your enum is UNIVERSITY)
            if ("university".equalsIgnoreCase(source)) {
                return SchoolType.UNIVERSITY;
            } else if ("primary_secondary".equalsIgnoreCase(source)) {
                return SchoolType.PRIMARY_SECONDARY;
            }
            // If still no match, throw an error
            throw new IllegalArgumentException("Invalid schoolType value: '" + source + "'. Expected 'university' or 'primary_secondary'.");
        }
    }
}