package ru.npepub.ui.validation;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;

import java.util.Optional;

@C2PComponent
class SourcePathValidator implements InputValidator {

    @Override
    public Optional<ValidationError> validate(PrepareRequest request) {
        if (request.sourcePath() == null || request.sourcePath().isBlank()) {
            return Optional.of(new ValidationError("Укажите папку-источник"));
        }
        return Optional.empty();
    }
}