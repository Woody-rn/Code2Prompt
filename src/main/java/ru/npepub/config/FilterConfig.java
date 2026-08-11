package ru.npepub.config;

import java.util.List;

/**
 * File and directory exclusion settings.
 */
public record FilterConfig(
        List<String> excludedDirs,
        List<String> excludedFileNames,
        List<String> patterns
) {
    public static FilterConfig defaults() {
        return new FilterConfig(
                List.of(".git/", ".gradle/", ".idea/", "build/", "target/",
                        "node_modules/", "__pycache__/", ".svn/", "out/", "dist/"),
                List.of(".env", ".env.local", ".env.production",
                        "credentials.json", "secrets.yaml", "secrets.yml",
                        "key.pem", "id_rsa", "id_ed25519"),
                List.of("*.class", "*.jar", "*.war", "*.lock")
        );
    }
}