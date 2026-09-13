package ru.npepub.ui.state;

import ru.npepub.di.api.C2PComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds the current system prompt and notifies listeners on change.
 * Acts as a single source of truth for prompt state across the UI.
 *
 * <p>All changes — programmatic (AI improvement, template selection)
 * and user-driven (typing) — must go through {@link #set(String)},
 * so that all subscribers (config persistence, task combo sync,
 * server update) are notified uniformly.</p>
 */
@C2PComponent
public class PromptState {

    private String value = "";
    private final List<Consumer<String>> listeners = new ArrayList<>();

    /** Returns the current prompt value. */
    public String get() {
        return value;
    }

    /**
     * Sets a new prompt value and notifies all subscribers.
     * Ignores the update if the value hasn't changed.
     */
    public void set(String newValue) {
        String normalized = newValue != null ? newValue : "";
        if (normalized.equals(this.value)) return;
        this.value = normalized;
        listeners.forEach(listener -> listener.accept(normalized));
    }

    /**
     * Registers a listener to be called on every prompt change.
     */
    public void subscribe(Consumer<String> listener) {
        listeners.add(listener);
    }
}