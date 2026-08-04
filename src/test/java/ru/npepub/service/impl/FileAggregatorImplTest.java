package ru.npepub.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.npepub.di.ContainerDI;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileAggregatorImplTest {

    private FileAggregator aggregator;

    @BeforeEach
    void setUp() {
        ContainerDI container = new ContainerDI();
        aggregator = container.get(FileAggregator.class);
    }

    @Test
    void shouldReturnSingleChunkWhenAllFilesFit() {
        FileInfo file1 = fileInfo("User.java", "class User {}", 13);
        FileInfo file2 = fileInfo("Order.java", "class Order {}", 15);

        List<Chunk> chunks = aggregator.aggregate(List.of(file1, file2), 1000);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().files()).containsExactly(file1, file2);
    }

    @Test
    void shouldSplitIntoMultipleChunksWhenLimitExceeded() {
        FileInfo file1 = fileInfo("a.java", "a", 1);
        FileInfo file2 = fileInfo("b.java", "b", 1);
        FileInfo file3 = fileInfo("c.java", "c", 1);

        List<Chunk> chunks = aggregator.aggregate(List.of(file1, file2, file3), 180);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).files()).containsExactly(file1, file2);
        assertThat(chunks.get(1).files()).containsExactly(file3);
    }

    @Test
    void shouldReturnEmptyListForEmptyInput() {
        List<Chunk> chunks = aggregator.aggregate(List.of(), 1000);
        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldAssignCorrectChunkIndexes() {
        FileInfo f1 = fileInfo("a.java", "a", 1);
        FileInfo f2 = fileInfo("b.java", "b", 1);

        // Каждый файл с заголовком ~87 символов, лимит 90 — только один влезает
        List<Chunk> chunks = aggregator.aggregate(List.of(f1, f2), 90);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).index()).isEqualTo(1);
        assertThat(chunks.get(1).index()).isEqualTo(2);
    }

    @Test
    void shouldSplitLargeFileIntoParts() {
        String bigContent = "x".repeat(1000);
        FileInfo bigFile = fileInfo("Big.java", bigContent, bigContent.length());

        List<Chunk> chunks = aggregator.aggregate(List.of(bigFile), 300);

        assertThat(chunks).isNotEmpty();
        for (Chunk chunk : chunks) {
            for (FileInfo file : chunk.files()) {
                assertThat(file.isSplit()).isTrue();
                assertThat(file.relativePath().toString()).isEqualTo("Big.java");
            }
        }
    }

    @Test
    void shouldNotMixSplitFileWithOtherFiles() {
        FileInfo small = fileInfo("small.java", "x", 1);
        FileInfo big = fileInfo("big.java", "y".repeat(1000), 1000);

        List<Chunk> chunks = aggregator.aggregate(List.of(small, big), 500);

        assertThat(chunks).isNotEmpty();
        // small должен быть в первом чанке, big разбит в следующих
        assertThat(chunks.getFirst().files().stream().anyMatch(f -> f.relativePath().toString().equals("small.java"))).isTrue();
    }

    private FileInfo fileInfo(String path, String content, int size) {
        return new FileInfo(Path.of(path), content, size, false, 0, 0);
    }
}