package ru.npepub.validation;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;

import java.util.Optional;

@C2PComponent
class OutputPathValidator implements InputValidator {

    @Override
    public Optional<ValidationError> validate(PrepareRequest request) {
        if (request.outputPath() == null || request.outputPath().isBlank()) {
            return Optional.of(new ValidationError("Укажите папку вывода"));
        }
        return Optional.empty();
    }
}