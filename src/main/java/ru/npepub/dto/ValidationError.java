package ru.npepub.dto;

/**
 * A validation error with a user-facing description.
 */
public record ValidationError(String description) {
}