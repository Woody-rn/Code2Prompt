package ru.npepub.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.npepub.di.ContainerDI;
import ru.npepub.model.Chunk;
import ru.npepub.model.FileInfo;
import ru.npepub.service.FileAggregator;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;;

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
        assertThat(chunks.get(0).files()).containsExactly(file1, file2);
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
    void shouldKeepLargeFileAloneInChunk() {
        FileInfo small = fileInfo("small.java", "x", 1);
        FileInfo large = fileInfo("large.java", "x".repeat(100), 1000);

        List<Chunk> chunks = aggregator.aggregate(List.of(small, large), 500);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).files()).containsExactly(small);
        assertThat(chunks.get(1).files()).containsExactly(large);
        assertThat(chunks.get(1).totalSize()).isGreaterThan(500);
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

        List<Chunk> chunks = aggregator.aggregate(List.of(f1, f2), 1);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).index()).isEqualTo(1);
        assertThat(chunks.get(1).index()).isEqualTo(2);
    }

    @Test
    void shouldNeverSplitSingleFile() {
        FileInfo singleFile = fileInfo("huge.java", "x".repeat(1000), 1000);

        List<Chunk> chunks = aggregator.aggregate(List.of(singleFile), 500);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).files()).containsExactly(singleFile);
    }

    @Test
    void shouldKeepOversizedFileInOwnChunk() {
        FileInfo huge = fileInfo("huge.java", "x".repeat(1000), 1000);

        List<Chunk> chunks = aggregator.aggregate(List.of(huge), 500);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).files()).containsExactly(huge);
        assertThat(chunks.get(0).totalSize()).isGreaterThan(500);
    }

    @Test
    void shouldNotMixOversizedFileWithOthers() {
        FileInfo small = fileInfo("small.java", "x", 1);
        FileInfo huge = fileInfo("huge.java", "x".repeat(1000), 1000);
        FileInfo another = fileInfo("another.java", "y", 1);

        List<Chunk> chunks = aggregator.aggregate(List.of(small, huge, another), 500);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).files()).containsExactly(small);
        assertThat(chunks.get(1).files()).containsExactly(huge);
        assertThat(chunks.get(2).files()).containsExactly(another);
    }

    private FileInfo fileInfo(String path, String content, int size) {
        return new FileInfo(Path.of(path), content, size);
    }
}