package ru.npepub.ui.validation;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;

import java.util.List;
import java.util.Optional;

/**
 * Composite validator that runs all InputValidators and returns the first error.
 */
@C2PComponent
class RequestValidatorImpl implements RequestValidator {

    @C2PInject
    private List<InputValidator> validators;

    @Override
    public Optional<ValidationError> validate(PrepareRequest request) {
        for (InputValidator validator : validators) {
            Optional<ValidationError> error = validator.validate(request);
            if (error.isPresent()) return error;
        }
        return Optional.empty();
    }
}