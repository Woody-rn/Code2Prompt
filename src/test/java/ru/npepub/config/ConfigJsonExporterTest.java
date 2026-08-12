package ru.npepub.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
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
        assertThat(restored.filter().excludedDirs()).containsExactlyElementsOf(original.filter().excludedDirs());
        assertThat(restored.filter().excludedFileNames()).containsExactlyElementsOf(original.filter().excludedFileNames());
        assertThat(restored.filter().patterns()).containsExactlyElementsOf(original.filter().patterns());
        assertThat(restored.log().level()).isEqualTo(original.log().level());
        assertThat(restored.log().errorEnabled()).isEqualTo(original.log().errorEnabled());
        assertThat(restored.prompt().systemPrompt()).isEqualTo(original.prompt().systemPrompt());
        assertThat(restored.prompt().partPrefixTemplate()).isEqualTo(original.prompt().partPrefixTemplate());
        assertThat(restored.prompt().finalPartTemplate()).isEqualTo(original.prompt().finalPartTemplate());
        assertThat(restored.prompt().fileSeparator()).isEqualTo(original.prompt().fileSeparator());
        assertThat(restored.prompt().customTemplates()).containsAllEntriesOf(original.prompt().customTemplates());
        assertThat(restored.debugMode()).isEqualTo(original.debugMode());
    }

    @Test
    void shouldHandleEmptyArrays() {
        AppConfig config = new AppConfig(
                AiModelConfig.defaults(),
                PathConfig.defaults(),
                new FilterConfig(List.of(), List.of(), List.of()),
                LogConfig.defaults(),
                PromptConfig.defaults(),
                false
        );

        String json = exporter.toJson(config);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.filter().excludedDirs()).isEmpty();
        assertThat(restored.filter().excludedFileNames()).isEmpty();
        assertThat(restored.filter().patterns()).isEmpty();
    }

    @Test
    void shouldHandleSpecialCharactersInPrompt() {
        Map<String, String> templates = new LinkedHashMap<>();
        templates.put("Шаблон 1", "Текст 1");
        templates.put("Шаблон \"два\"", "Текст с \nпереносом");

        PromptConfig prompt = new PromptConfig(
                "Проверь \"кавычки\" и \nпереносы",
                "Шаблон с \\ и \t",
                "Финал",
                "====",
                templates
        );
        AppConfig config = new AppConfig(
                AiModelConfig.defaults(),
                PathConfig.defaults(),
                FilterConfig.defaults(),
                LogConfig.defaults(),
                prompt,
                false
        );

        String json = exporter.toJson(config);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.prompt().systemPrompt()).isEqualTo("Проверь \"кавычки\" и \nпереносы");
        assertThat(restored.prompt().partPrefixTemplate()).isEqualTo("Шаблон с \\ и \t");
        assertThat(restored.prompt().customTemplates()).containsAllEntriesOf(templates);
    }

    @Test
    void shouldHandleEmptyCustomTemplates() {
        PromptConfig prompt = new PromptConfig("", "", "", "", new LinkedHashMap<>());
        AppConfig config = new AppConfig(
                AiModelConfig.defaults(),
                PathConfig.defaults(),
                FilterConfig.defaults(),
                LogConfig.defaults(),
                prompt,
                false
        );

        String json = exporter.toJson(config);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.prompt().customTemplates()).isEmpty();
    }

    @Test
    void shouldHandleMultipleCustomTemplates() {
        Map<String, String> templates = new LinkedHashMap<>();
        templates.put("Шаблон 1", "Текст 1");
        templates.put("Шаблон с / и \\", "Текст 2");
        templates.put("Шаблон с переносом", "Текст с \nпереносом");

        PromptConfig prompt = new PromptConfig("", "", "", "", templates);
        AppConfig config = new AppConfig(
                AiModelConfig.defaults(),
                PathConfig.defaults(),
                FilterConfig.defaults(),
                LogConfig.defaults(),
                prompt,
                false
        );

        String json = exporter.toJson(config);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.prompt().customTemplates()).containsAllEntriesOf(templates);
    }

    @Test
    void shouldHandleWhitespaceInMap() {
        String json = "{ \"prompt\": { \"customTemplates\": { \"a\": \"x\", \"b\": \"y\", \"c\": \"z\" } } }";
        AppConfig config = exporter.fromJson(json);
        assertThat(config.prompt().customTemplates()).containsAllEntriesOf(Map.of("a", "x", "b", "y", "c", "z"));
    }
}