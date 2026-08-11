package ru.npepub.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertThat(restored.log().level()).isEqualTo(original.log().level());
        assertThat(restored.log().errorEnabled()).isEqualTo(original.log().errorEnabled());
        assertThat(restored.prompt().systemPrompt()).isEqualTo(original.prompt().systemPrompt());
        assertThat(restored.prompt().partPrefixTemplate()).isEqualTo(original.prompt().partPrefixTemplate());
        assertThat(restored.prompt().finalPartTemplate()).isEqualTo(original.prompt().finalPartTemplate());
        assertThat(restored.prompt().fileSeparator()).isEqualTo(original.prompt().fileSeparator());
        assertThat(restored.debugMode()).isEqualTo(original.debugMode());
    }

    @Test
    void shouldHandleEmptyArrays() {
        AppConfig config = new AppConfig(
                AiModelConfig.defaults(),
                PathConfig.defaults(),
                new FilterConfig(List.of(), List.of()),
                LogConfig.defaults(),
                PromptConfig.defaults(),
                false
        );

        String json = exporter.toJson(config);
        AppConfig restored = exporter.fromJson(json);

        assertThat(restored.filter().excludedDirs()).isEmpty();
        assertThat(restored.filter().excludedFileNames()).isEmpty();
    }

    @Test
    void shouldHandleSpecialCharactersInPrompt() {
        PromptConfig prompt = new PromptConfig(
                "Проверь \"кавычки\" и \nпереносы",
                "Шаблон с \\ и \t",
                "Финал",
                "===="
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
    }
}