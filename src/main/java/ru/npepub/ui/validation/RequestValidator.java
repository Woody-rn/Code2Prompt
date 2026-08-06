package ru.npepub.ui.validation;

import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;

import java.util.Optional;

/**
 * Validates a PrepareRequest.
 * Returns the first error found, or empty if valid.
 */
public interface RequestValidator {

    /**
     * @return first validation error, or empty if request is valid
     */
    Optional<ValidationError> validate(PrepareRequest request);
}