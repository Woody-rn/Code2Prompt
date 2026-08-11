package ru.npepub.config;

import ru.npepub.di.api.C2PComponent;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports and imports AppConfig as JSON.
 */
@C2PComponent
public class ConfigJsonExporter {

    /** Serializes config to JSON string. */
    public String toJson(AppConfig c) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"aiModel\": {\n");
        sb.append("    \"name\": \"").append(esc(c.aiModel().name())).append("\",\n");
        sb.append("    \"maxSymbols\": ").append(c.aiModel().maxSymbols()).append(",\n");
        sb.append("    \"safetyMargin\": ").append(c.aiModel().safetyMargin()).append("\n");
        sb.append("  },\n");
        sb.append("  \"paths\": {\n");
        sb.append("    \"outputPath\": \"").append(esc(c.paths().outputPath().toString())).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"filter\": {\n");
        sb.append("    \"excludedDirs\": ").append(toJsonArray(c.filter().excludedDirs())).append(",\n");
        sb.append("    \"excludedFileNames\": ").append(toJsonArray(c.filter().excludedFileNames())).append("\n");
        sb.append("  },\n");
        sb.append("  \"log\": {\n");
        sb.append("    \"level\": \"").append(c.log().level().name()).append("\",\n");
        sb.append("    \"errorEnabled\": ").append(c.log().errorEnabled()).append("\n");
        sb.append("  },\n");
        sb.append("  \"prompt\": {\n");
        sb.append("    \"systemPrompt\": \"").append(esc(c.prompt().systemPrompt())).append("\",\n");
        sb.append("    \"partPrefixTemplate\": \"").append(esc(c.prompt().partPrefixTemplate())).append("\",\n");
        sb.append("    \"finalPartTemplate\": \"").append(esc(c.prompt().finalPartTemplate())).append("\",\n");
        sb.append("    \"fileSeparator\": \"").append(esc(c.prompt().fileSeparator())).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"debugMode\": ").append(c.debugMode()).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /** Deserializes config from JSON string. */
    public AppConfig fromJson(String json) {
        return new AppConfig(
                new AiModelConfig(
                        extractString(json, "name"),
                        extractInt(json, "maxSymbols", 100000),
                        extractDouble(json, "safetyMargin", 0.05)
                ),
                new PathConfig(
                        Path.of(extractString(json, "outputPath")),
                        List.of(), 10
                ),
                new FilterConfig(
                        extractArray(json, "excludedDirs"),
                        extractArray(json, "excludedFileNames")
                ),
                new LogConfig(
                        LogConfig.LogLevel.valueOf(extractString(json, "level", "INFO")),
                        extractBoolean(json, "errorEnabled", true)
                ),
                new PromptConfig(
                        extractString(json, "systemPrompt", ""),
                        extractString(json, "partPrefixTemplate", PromptConfig.defaults().partPrefixTemplate()),
                        extractString(json, "finalPartTemplate", PromptConfig.defaults().finalPartTemplate()),
                        extractString(json, "fileSeparator", PromptConfig.defaults().fileSeparator())
                ),
                extractBoolean(json, "debugMode", false)
        );
    }

    private String extractString(String json, String key) {
        return extractString(json, key, "");
    }

    private String extractString(String json, String key, String defaultValue) {
        String pattern = "\"" + key + "\": \"";
        int start = json.indexOf(pattern);
        if (start == -1) return defaultValue;
        start += pattern.length();

        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && (end == start || json.charAt(end - 1) != '\\')) {
                break;
            }
            end++;
        }
        if (end >= json.length()) return defaultValue;

        return json.substring(start, end)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private int extractInt(String json, String key, int defaultValue) {
        String pattern = "\"" + key + "\": ";
        int start = json.indexOf(pattern);
        if (start == -1) return defaultValue;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (end == start) return defaultValue;
        return Integer.parseInt(json.substring(start, end));
    }

    private double extractDouble(String json, String key, double defaultValue) {
        String pattern = "\"" + key + "\": ";
        int start = json.indexOf(pattern);
        if (start == -1) return defaultValue;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        if (end == start) return defaultValue;
        return Double.parseDouble(json.substring(start, end));
    }

    private boolean extractBoolean(String json, String key, boolean defaultValue) {
        String pattern = "\"" + key + "\": ";
        int start = json.indexOf(pattern);
        if (start == -1) return defaultValue;
        start += pattern.length();
        if (json.startsWith("true", start)) return true;
        if (json.startsWith("false", start)) return false;
        return defaultValue;
    }

    private List<String> extractArray(String json, String key) {
        String pattern = "\"" + key + "\": [";
        int start = json.indexOf(pattern);
        if (start == -1) return List.of();
        start += pattern.length();
        int end = json.indexOf("]", start);
        if (end == -1) return List.of();
        String arrayStr = json.substring(start, end);
        if (arrayStr.isBlank()) return List.of();
        return Arrays.stream(arrayStr.split(","))
                .map(s -> s.trim().replace("\"", ""))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String toJsonArray(List<String> list) {
        if (list.isEmpty()) return "[]";
        return "[" + list.stream()
                .map(s -> "\"" + esc(s) + "\"")
                .collect(Collectors.joining(", ")) + "]";
    }
}