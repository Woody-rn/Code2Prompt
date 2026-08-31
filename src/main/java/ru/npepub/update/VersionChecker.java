package ru.npepub.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

/**
 * Checks GitHub for newer releases.
 */
@C2PComponent
public class VersionChecker {

    private static final Logger log = LoggerFactory.getLogger(VersionChecker.class);

    private final ObjectMapper mapper = new ObjectMapper();

    public record UpdateInfo(String version, String url, boolean updateAvailable) {}

    /** Checks GitHub for the latest release. */
    public UpdateInfo check() {
        Optional<String> currentVersion = getCurrentVersion();
        Optional<String> apiUrl = getGithubApiUrl();

        if (currentVersion.isEmpty() || apiUrl.isEmpty()) {
            log.warn("Version or API URL not found in version.properties");
            return new UpdateInfo(null, null, false);
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl.get()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("GitHub API returned status {}", response.statusCode());
                return new UpdateInfo(null, null, false);
            }

            JsonNode node = mapper.readTree(response.body());
            String latestVersion = node.get("tag_name").asString();
            String url = node.get("html_url").asString();

            latestVersion = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;

            boolean available = compareVersions(latestVersion, currentVersion.get()) > 0;
            return new UpdateInfo(latestVersion, url, available);
        } catch (IOException | InterruptedException e) {
            log.warn("Failed to check for updates: {}", e.getMessage());
            return new UpdateInfo(null, null, false);
        }
    }

    private Optional<String> getCurrentVersion() {
        return readProperty("app.version");
    }

    private Optional<String> getGithubApiUrl() {
        return readProperty("github.api.url");
    }

    private Optional<String> readProperty(String key) {
        try (InputStream in = getClass().getResourceAsStream("/version.properties")) {
            if (in == null) return Optional.empty();
            Properties props = new Properties();
            props.load(in);
            return Optional.ofNullable(props.getProperty(key));
        } catch (IOException e) {
            log.warn("Failed to read version.properties", e);
            return Optional.empty();
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int max = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < max; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}