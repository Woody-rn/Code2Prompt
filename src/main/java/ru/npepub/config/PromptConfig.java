package ru.npepub.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prompt formatting templates.
 */
public record PromptConfig(
        String systemPrompt,
        String partPrefixTemplate,
        String finalPartTemplate,
        String fileSeparator,
        Map<String, String> customTemplates
) {
    public static PromptConfig defaults() {
        return new PromptConfig(
                "",
                "[ЧАСТЬ {part}/{total}] НЕ ОТВЕЧАЙ. Только подтверди: \"Принята часть {part}/{total}\".\n\n",
                "[ЧАСТЬ {part}/{total}] Это последняя часть. Можешь отвечать.\n\n",
                "=".repeat(40),
                new LinkedHashMap<>()
        );
    }
}