package ru.npepub.ai;

/**
 * Port for interacting with a local AI assistant.
 */
public interface PromptAssistant {

    /**
     * Improves the given prompt text.
     */
    String improve(String text);

    /**
     * Expands a short phrase into a full prompt.
     */
    String expand(String text);

    /**
     * Generates a short name for a custom template.
     */
    String generateName(String text);

    /**
     * @return true if the assistant is available
     */
    boolean isAvailable();
}