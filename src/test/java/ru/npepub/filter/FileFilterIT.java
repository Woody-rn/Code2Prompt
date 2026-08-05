package ru.npepub.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npepub.di.ContainerDI;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileFilterIntegrationTest {

    private FileFilter filter;

    @BeforeEach
    void setUp() {
        ContainerDI container = new ContainerDI();
        filter = container.get(FileFilter.class);
    }

    @Test
    void shouldIncludeRegularJavaFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("src/main/java/User.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class User {}");

        assertThat(filter.shouldInclude(file)).isTrue();
    }

    @Test
    void shouldExcludeBuildDirectory(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("build/classes/App.class");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "binary");

        assertThat(filter.shouldInclude(file)).isFalse();
    }

    @Test
    void shouldExcludeGitDirectory(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve(".git/config");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "[core]");

        assertThat(filter.shouldInclude(file)).isFalse();
    }

    @Test
    void shouldExcludeEnvFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve(".env");
        Files.writeString(file, "SECRET=123");

        assertThat(filter.shouldInclude(file)).isFalse();
    }

    @Test
    void shouldExcludeCredentialsJson(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("credentials.json");
        Files.writeString(file, "{}");

        assertThat(filter.shouldInclude(file)).isFalse();
    }

    @Test
    void shouldExcludeBinaryFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("image.jpg");
        byte[] bytes = new byte[]{0x00, 0x01, 0x02};
        Files.write(file, bytes);

        assertThat(filter.shouldInclude(file)).isFalse();
    }

    @Test
    void shouldIncludeYamlFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, "key: value");

        assertThat(filter.shouldInclude(file)).isTrue();
    }
}