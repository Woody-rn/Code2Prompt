package ru.npepub.config;

import ru.npepub.di.api.C2PComponent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exports and imports AppConfig as JSON.
 */
@C2PComponent
public class ConfigJsonExporter {

    /**
     * Serializes config to JSON string.
     */
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
        sb.append("    \"excludedFileNames\": ").append(toJsonArray(c.filter().excludedFileNames())).append(",\n");
        sb.append("    \"patterns\": ").append(toJsonArray(c.filter().patterns())).append("\n");
        sb.append("  },\n");
        sb.append("  \"log\": {\n");
        sb.append("    \"level\": \"").append(c.log().level().name()).append("\",\n");
        sb.append("    \"errorEnabled\": ").append(c.log().errorEnabled()).append("\n");
        sb.append("  },\n");
        sb.append("  \"prompt\": {\n");
        sb.append("    \"systemPrompt\": \"").append(esc(c.prompt().systemPrompt())).append("\",\n");
        sb.append("    \"partPrefixTemplate\": \"").append(esc(c.prompt().partPrefixTemplate())).append("\",\n");
        sb.append("    \"finalPartTemplate\": \"").append(esc(c.prompt().finalPartTemplate())).append("\",\n");
        sb.append("    \"fileSeparator\": \"").append(esc(c.prompt().fileSeparator())).append("\",\n");
        sb.append("    \"customTemplates\": ").append(toJsonMap(c.prompt().customTemplates())).append("\n");
        sb.append("  },\n");
        sb.append("  \"debugMode\": ").append(c.debugMode()).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Deserializes config from JSON string.
     */
    public AppConfig fromJson(String json) {
        return new AppConfig(
                new ModelLimitConfig(
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
                        extractArray(json, "excludedFileNames"),
                        extractArray(json, "patterns")
                ),
                new LogConfig(
                        LogConfig.LogLevel.valueOf(extractString(json, "level", "INFO")),
                        extractBoolean(json, "errorEnabled", true)
                ),
                new PromptConfig(
                        extractString(json, "systemPrompt", ""),
                        extractString(json, "partPrefixTemplate", PromptConfig.defaults().partPrefixTemplate()),
                        extractString(json, "finalPartTemplate", PromptConfig.defaults().finalPartTemplate()),
                        extractString(json, "fileSeparator", PromptConfig.defaults().fileSeparator()),
                        extractMap(json, "customTemplates")
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

        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < arrayStr.length()) {
            if (arrayStr.charAt(i) == '"') {
                result.add(parseJsonString(arrayStr, i));
                i += parseJsonStringLength(arrayStr, i);
            } else {
                i++;
            }
        }
        return result;
    }

    private Map<String, String> extractMap(String json, String key) {
        String pattern = "\"" + key + "\": {";
        int start = json.indexOf(pattern);
        if (start == -1) return new LinkedHashMap<>();
        start += pattern.length();
        int end = json.indexOf("}", start);
        if (end == -1) return new LinkedHashMap<>();
        String mapStr = json.substring(start, end);

        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        while (i < mapStr.length()) {
            if (mapStr.charAt(i) == '"') {
                String mapKey = parseJsonString(mapStr, i);
                i += parseJsonStringLength(mapStr, i);
                i = skipWhitespace(mapStr, i);
                if (i < mapStr.length() && mapStr.charAt(i) == ':') i++;
                i = skipWhitespace(mapStr, i);
                if (i < mapStr.length() && mapStr.charAt(i) == '"') {
                    String mapValue = parseJsonString(mapStr, i);
                    i += parseJsonStringLength(mapStr, i);
                    map.put(mapKey, mapValue);
                }
                i = skipWhitespace(mapStr, i);
                if (i < mapStr.length() && mapStr.charAt(i) == ',') i++;
            } else {
                i++;
            }
        }
        return map;
    }

    private String parseJsonString(String s, int start) {
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < s.length()) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':
                        sb.append('"');
                        i += 2;
                        break;
                    case '\\':
                        sb.append('\\');
                        i += 2;
                        break;
                    case 'n':
                        sb.append('\n');
                        i += 2;
                        break;
                    case 'r':
                        sb.append('\r');
                        i += 2;
                        break;
                    case 't':
                        sb.append('\t');
                        i += 2;
                        break;
                    default:
                        sb.append(s.charAt(i));
                        i++;
                        break;
                }
            } else if (s.charAt(i) == '"') {
                break;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private int parseJsonStringLength(String s, int start) {
        int i = start + 1;
        while (i < s.length()) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                i += 2;
            } else if (s.charAt(i) == '"') {
                return i - start + 1;
            } else {
                i++;
            }
        }
        return i - start;
    }

    private int skipWhitespace(String s, int i) {
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\n' || s.charAt(i) == '\r' || s.charAt(i) == '\t')) {
            i++;
        }
        return i;
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

    private String toJsonMap(Map<String, String> map) {
        if (map.isEmpty()) return "{}";
        return "{" + map.entrySet().stream()
                .map(e -> "\"" + esc(e.getKey()) + "\": \"" + esc(e.getValue()) + "\"")
                .collect(Collectors.joining(", ")) + "}";
    }
}