package ru.npepub.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npepub.di.ContainerDI;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileScanner;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileScannerImplTest {

    private FileScanner scanner;

    @BeforeEach
    void setUp() {
        ContainerDI container = new ContainerDI();
        scanner = container.get(FileScanner.class);
        System.out.println(scanner);
    }

    @Test
    void shouldScanAllFilesRecursively(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("User.java"), "class User {}");
        Files.writeString(tempDir.resolve("app.yaml"), "port: 8080");
        Files.createDirectories(tempDir.resolve("deep/nested"));
        Files.writeString(tempDir.resolve("deep/nested/Util.java"), "class Util {}");

        List<FileInfo> files = scanner.scan(tempDir);

        assertThat(files).hasSize(3);

        List<String> relativePaths = files.stream()
                .map(f -> f.relativePath().toString().replace("\\", "/"))
                .toList();

        assertThat(relativePaths).containsExactlyInAnyOrder(
                "User.java",
                "app.yaml",
                "deep/nested/Util.java"
        );
    }

    @Test
    void shouldReturnRelativePaths(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("Main.java"), "class Main {}");

        List<FileInfo> files = scanner.scan(tempDir);

        assertThat(files).hasSize(1);
        FileInfo file = files.get(0);
        assertThat(file.relativePath().toString()).isEqualTo("Main.java");
        assertThat(file.content()).isEqualTo("class Main {}");
        assertThat(file.size()).isEqualTo("class Main {}".length());
    }

    @Test
    void shouldReturnEmptyListForEmptyDirectory(@TempDir Path tempDir) {
        List<FileInfo> files = scanner.scan(tempDir);
        assertThat(files).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenDirectoryDoesNotExist() {
        Path nonExistent = Path.of("non_existent_directory");

        assertThatThrownBy(() -> scanner.scan(nonExistent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a directory");
    }

    @Test
    void shouldThrowExceptionWhenPathIsFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        assertThatThrownBy(() -> scanner.scan(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a directory");
    }
}