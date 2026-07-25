package ru.npepub.validation;

import ru.npepub.di.api.C2PComponent;
import ru.npepub.dto.PrepareRequest;
import ru.npepub.dto.ValidationError;

import java.util.Optional;

@C2PComponent
class LimitValidator implements InputValidator {

    @Override
    public Optional<ValidationError> validate(PrepareRequest request) {
        try {
            int limit = Integer.parseInt(request.limitText());
            if (limit <= 0) {
                return Optional.of(new ValidationError("Лимит должен быть положительным числом"));
            }
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.of(new ValidationError("Некорректный лимит символов"));
        }
    }
}