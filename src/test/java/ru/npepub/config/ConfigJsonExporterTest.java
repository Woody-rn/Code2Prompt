package ru.npepub.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.npepub.ai.AssistantConfig;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigJsonExporterTest {

    private ConfigJsonExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new ConfigJsonExporter();
    }

    @Test
    void shouldSerializeAndDeserialize() {
        AppConfig original = AppConfig.defaults();

        String json = exporter.toJson(original);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.aiModel().name()).isEqualTo(original.aiModel().name());
        assertThat(restored.aiModel().maxSymbols()).isEqualTo(original.aiModel().maxSymbols());
        assertThat(restored.aiModel().safetyMargin()).isEqualTo(original.aiModel().safetyMargin());
        assertThat(restored.paths().outputPath()).isEqualTo(original.paths().outputPath());
        assertThat(restored.filter().excludedDirs()).containsExactlyInAnyOrderElementsOf(original.filter().excludedDirs());
        assertThat(restored.filter().excludedFileNames()).containsExactlyInAnyOrderElementsOf(original.filter().excludedFileNames());
        assertThat(restored.filter().patterns()).containsExactlyInAnyOrderElementsOf(original.filter().patterns());
        assertThat(restored.log().level()).isEqualTo(original.log().level());
        assertThat(restored.log().errorEnabled()).isEqualTo(original.log().errorEnabled());
        assertThat(restored.prompt().systemPrompt()).isEqualTo(original.prompt().systemPrompt());
        assertThat(restored.prompt().partPrefixTemplate()).isEqualTo(original.prompt().partPrefixTemplate());
        assertThat(restored.prompt().finalPartTemplate()).isEqualTo(original.prompt().finalPartTemplate());
        assertThat(restored.prompt().fileSeparator()).isEqualTo(original.prompt().fileSeparator());
        assertThat(restored.prompt().customTemplates()).containsAllEntriesOf(original.prompt().customTemplates());
        assertThat(restored.assistant().endpoint()).isEqualTo(original.assistant().endpoint());
        assertThat(restored.assistant().model()).isEqualTo(original.assistant().model());
        assertThat(restored.assistant().enabled()).isEqualTo(original.assistant().enabled());
        assertThat(restored.oneFilePerChunk()).isEqualTo(original.oneFilePerChunk());
        assertThat(restored.debugMode()).isEqualTo(original.debugMode());
    }

    @Test
    void shouldHandleEmptyCustomTemplates() {
        PromptConfig prompt = new PromptConfig("", "", "", "", new LinkedHashMap<>());
        AppConfig config = new AppConfig(
                ModelLimitConfig.defaults(),
                PathConfig.defaults(),
                FilterConfig.defaults(),
                LogConfig.defaults(),
                prompt,
                AssistantConfig.defaults(),
                false,
                false
        );

        String json = exporter.toJson(config);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.prompt().customTemplates()).isEmpty();
    }

    @Test
    void shouldHandleCustomTemplatesWithSpecialCharacters() {
        Map<String, String> templates = new LinkedHashMap<>();
        templates.put("Шаблон 1", "Текст с \"кавычками\" и \nпереносом");
        templates.put("Шаблон 2", "Текст с \\ слэшем");

        PromptConfig prompt = new PromptConfig("", "", "", "", templates);
        AppConfig config = new AppConfig(
                ModelLimitConfig.defaults(),
                PathConfig.defaults(),
                FilterConfig.defaults(),
                LogConfig.defaults(),
                prompt,
                AssistantConfig.defaults(),
                false,
                false
        );

        String json = exporter.toJson(config);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.prompt().customTemplates()).containsAllEntriesOf(templates);
    }
}