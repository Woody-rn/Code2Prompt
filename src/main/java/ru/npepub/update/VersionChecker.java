package ru.npepub.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;

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

            Optional<String> latestVersion = extractVersion(response.body());
            Optional<String> url = extractUrl(response.body());

            if (latestVersion.isEmpty() || url.isEmpty()) {
                log.warn("Failed to parse GitHub API response");
                return new UpdateInfo(null, null, false);
            }

            boolean available = !latestVersion.get().equals(currentVersion.get());
            return new UpdateInfo(latestVersion.get(), url.get(), available);
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

    private Optional<String> extractVersion(String json) {
        int idx = json.indexOf("\"tag_name\"");
        if (idx == -1) return Optional.empty();
        int start = json.indexOf("\"", idx + 11) + 1;
        int end = json.indexOf("\"", start);
        String tag = json.substring(start, end);
        return Optional.of(tag.startsWith("v") ? tag.substring(1) : tag);
    }

    private Optional<String> extractUrl(String json) {
        int idx = json.indexOf("\"html_url\"");
        if (idx == -1) return Optional.empty();
        int start = json.indexOf("\"", idx + 11) + 1;
        int end = json.indexOf("\"", start);
        return Optional.of(json.substring(start, end));
    }
}