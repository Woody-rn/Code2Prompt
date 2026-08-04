package ru.npepub.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.npepub.di.ContainerDI;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.OutputWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutputWriterImplTest {

    private OutputWriter writer;

    @BeforeEach
    void setUp() {
        ContainerDI container = new ContainerDI();
        writer = container.get(OutputWriter.class);
    }

    @Test
    void shouldWriteSingleChunkToFile(@TempDir Path outputDir) {
        FileInfo file = fileInfo("User.java", "class User {}");
        Chunk chunk = new Chunk(1, List.of(file), file.size());

        List<Path> paths = writer.write(List.of(chunk), outputDir);

        assertThat(paths).hasSize(1);
        assertThat(paths.getFirst().getFileName().toString()).isEqualTo("code2prompt_part1.txt");
        assertThat(paths.getFirst()).exists();
    }

    @Test
    void shouldWriteMultipleChunksToSeparateFiles(@TempDir Path outputDir) {
        FileInfo file1 = fileInfo("a.java", "aaa");
        FileInfo file2 = fileInfo("b.java", "bbb");
        Chunk chunk1 = new Chunk(1, List.of(file1), file1.size());
        Chunk chunk2 = new Chunk(2, List.of(file2), file2.size());

        List<Path> paths = writer.write(List.of(chunk1, chunk2), outputDir);

        assertThat(paths).hasSize(2);
        assertThat(paths.get(0).getFileName().toString()).isEqualTo("code2prompt_part1.txt");
        assertThat(paths.get(1).getFileName().toString()).isEqualTo("code2prompt_part2.txt");
        assertThat(paths.get(0)).exists();
        assertThat(paths.get(1)).exists();
    }

    @Test
    void shouldIncludeSeparatorAndFilePathInOutput(@TempDir Path outputDir) throws Exception {
        FileInfo file = fileInfo("src/main/User.java", "class User {}");
        Chunk chunk = new Chunk(1, List.of(file), file.size());

        writer.write(List.of(chunk), outputDir);

        String content = Files.readString(outputDir.resolve("code2prompt_part1.txt"));
        String normalizedContent = content.replace("\\", "/");

        assertThat(normalizedContent).contains("File: src/main/User.java");
        assertThat(normalizedContent).contains("class User {}");
        assertThat(normalizedContent).contains("====");
    }

    @Test
    void shouldCreateOutputDirectoryIfNotExists(@TempDir Path baseDir) {
        Path outputDir = baseDir.resolve("new/nested/output");

        FileInfo file = fileInfo("test.java", "test");
        Chunk chunk = new Chunk(1, List.of(file), file.size());

        writer.write(List.of(chunk), outputDir);

        assertThat(outputDir).exists();
        assertThat(outputDir.resolve("code2prompt_part1.txt")).exists();
    }

    @Test
    void shouldReturnEmptyListForEmptyChunks(@TempDir Path outputDir) {
        List<Path> paths = writer.write(List.of(), outputDir);

        assertThat(paths).isEmpty();
    }

    private FileInfo fileInfo(String path, String content) {
        return new FileInfo(Path.of(path), content, content.length(), false, 0, 0);
    }
}