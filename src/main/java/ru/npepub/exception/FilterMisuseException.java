package ru.npepub.exception;

/**
 * Thrown when a filter is called with an argument that violates its contract.
 *
 * <p>Example: {@code FileFilter.shouldInclude} receives a filesystem root
 * (e.g. {@code "/"} or {@code "C:\"}) instead of a file path. This is a bug
 * in the caller, not a runtime data error — hence a {@link RuntimeException}.</p>
 */
public class FilterMisuseException extends RuntimeException {

    public FilterMisuseException(String message) {
        super(message);
    }

    public FilterMisuseException(String message, Throwable cause) {
        super(message, cause);
    }
}