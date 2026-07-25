package ru.npepub.validation;

import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;

import java.util.Optional;

/**
 * Validates a single aspect of a PrepareRequest.
 */
interface InputValidator {

    /**
     * @return validation error if invalid, empty if valid
     */
    Optional<ValidationError> validate(PrepareRequest request);
}
