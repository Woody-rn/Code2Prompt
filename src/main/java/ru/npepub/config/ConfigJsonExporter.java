package ru.npepub.config;

import ru.npepub.di.api.C2PComponent;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

/**
 * Exports and imports AppConfig as JSON using Jackson.
 */
@C2PComponent
public class ConfigJsonExporter {

    ObjectMapper mapper = new ObjectMapper()
            .rebuild()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /** Serializes config to JSON string. */
    public String toJson(AppConfig config) {
        try {
            return mapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize config", e);
        }
    }

    /** Deserializes config from JSON string. */
    public AppConfig fromJson(String json) {
        try {
            return mapper.readValue(json, AppConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize config", e);
        }
    }
}