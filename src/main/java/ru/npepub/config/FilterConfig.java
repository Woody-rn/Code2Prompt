package ru.npepub.config;

import java.util.Set;

/**
 * File and directory exclusion settings.
 */
public record FilterConfig(
        Set<String> excludedDirs,
        Set<String> excludedFileNames,
        Set<String> patterns
) {
    public static FilterConfig defaults() {
        return new FilterConfig(
                Set.of(".git/", ".gradle/", ".idea/", "build/", "target/",
                        "node_modules/", "__pycache__/", ".svn/", "out/", "dist/"),
                Set.of(".env", ".env.local", ".env.production",
                        "credentials.json", "secrets.yaml", "secrets.yml",
                        "key.pem", "id_rsa", "id_ed25519"),
                Set.of("*.class", "*.jar", "*.war", "*.lock")
        );
    }
}