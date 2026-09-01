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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileAggregatorImplTest {

    private FileAggregator aggregator;

    @BeforeEach
    void setUp() {
        ContainerDI container = new ContainerDI();
        aggregator = container.get(FileAggregator.class);
    }

    // ========== Базовые тесты (уже есть) ==========

    @Test
    void shouldReturnSingleChunkWhenAllFilesFit() {
        FileInfo file1 = fileInfo("User.java", "class User {}", 13);
        FileInfo file2 = fileInfo("Order.java", "class Order {}", 15);

        List<Chunk> chunks = aggregator.aggregate(List.of(file1, file2), 1000, false);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().files()).containsExactly(file1, file2);
    }

    @Test
    void shouldSplitIntoMultipleChunksWhenLimitExceeded() {
        FileInfo file1 = fileInfo("a.java", "a", 1);
        FileInfo file2 = fileInfo("b.java", "b", 1);
        FileInfo file3 = fileInfo("c.java", "c", 1);

        List<Chunk> chunks = aggregator.aggregate(List.of(file1, file2, file3), 180, false);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).files()).containsExactly(file1, file2);
        assertThat(chunks.get(1).files()).containsExactly(file3);
    }

    @Test
    void shouldReturnEmptyListForEmptyInput() {
        List<Chunk> chunks = aggregator.aggregate(List.of(), 1000, false);
        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldAssignCorrectChunkIndexes() {
        FileInfo f1 = fileInfo("a.java", "a", 1);
        FileInfo f2 = fileInfo("b.java", "b", 1);

        List<Chunk> chunks = aggregator.aggregate(List.of(f1, f2), 90, false);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).index()).isEqualTo(1);
        assertThat(chunks.get(1).index()).isEqualTo(2);
    }

    @Test
    void shouldSplitLargeFileIntoParts() {
        String bigContent = "x".repeat(1000);
        FileInfo bigFile = fileInfo("Big.java", bigContent, bigContent.length());

        List<Chunk> chunks = aggregator.aggregate(List.of(bigFile), 300, false);

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

        List<Chunk> chunks = aggregator.aggregate(List.of(small, big), 500, false);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getFirst().files().stream()
                .anyMatch(f -> f.relativePath().toString().equals("small.java"))).isTrue();
    }

    @Test
    void shouldCreateOneChunkPerFileWhenOneFilePerChunkEnabled() {
        FileInfo file1 = fileInfo("a.java", "aaa", 3);
        FileInfo file2 = fileInfo("b.java", "bbb", 3);

        List<Chunk> chunks = aggregator.aggregate(List.of(file1, file2), 1000, true);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).files()).containsExactly(file1);
        assertThat(chunks.get(1).files()).containsExactly(file2);
    }

    @Test
    void shouldSplitLargeFileWhenOneFilePerChunkEnabled() {
        String bigContent = "x".repeat(1000);
        FileInfo bigFile = fileInfo("Big.java", bigContent, bigContent.length());

        List<Chunk> chunks = aggregator.aggregate(List.of(bigFile), 300, true);

        assertThat(chunks).isNotEmpty();
        for (Chunk chunk : chunks) {
            assertThat(chunk.files()).hasSize(1);
            assertThat(chunk.files().getFirst().isSplit()).isTrue();
        }
    }

    // ========== Новые тесты ==========

    @Test
    void shouldCloseChunkWhenFillThresholdReached() {
        // Лимит 1000, порог 95% = 950
        FileInfo file1 = fileInfo("a.java", "x".repeat(800), 800);
        FileInfo file2 = fileInfo("b.java", "y".repeat(200), 200);

        List<Chunk> chunks = aggregator.aggregate(List.of(file1, file2), 1000, false);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).files()).containsExactly(file1);
        assertThat(chunks.get(1).files()).containsExactly(file2);
    }

    @Test
    void shouldKeepSmallFilesTogetherUntilThreshold() {
        FileInfo file1 = fileInfo("a.java", "aaa", 3);
        FileInfo file2 = fileInfo("b.java", "bbb", 3);
        FileInfo file3 = fileInfo("c.java", "ccc", 3);

        List<Chunk> chunks = aggregator.aggregate(List.of(file1, file2, file3), 1000, false);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().files()).containsExactly(file1, file2, file3);
    }

    @Test
    void shouldThrowExceptionWhenLimitTooSmallForAnyFile() {
        FileInfo file = fileInfo("User.java", "class User {}", 13);

        assertThatThrownBy(() -> aggregator.aggregate(List.of(file), 50, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Symbol limit too small");
    }

    @Test
    void shouldSplitFileIntoPartsNotExceedingLimit() {
        String bigContent = "x".repeat(1000);
        FileInfo bigFile = fileInfo("Big.java", bigContent, bigContent.length());

        List<Chunk> chunks = aggregator.aggregate(List.of(bigFile), 300, false);

        for (Chunk chunk : chunks) {
            assertThat(chunk.totalSize()).isLessThanOrEqualTo(300);
            assertThat(chunk.files()).hasSize(1);
            assertThat(chunk.files().getFirst().isSplit()).isTrue();
        }
    }

    @Test
    void shouldCalculateCorrectNumberOfParts() {
        String content = "x".repeat(1000);
        FileInfo file = fileInfo("Big.java", content, 1000);
        int limit = 300;

        // partLimit = 300 - 80 - 100 = 120
        // totalParts = ceil(1000 / 120) = 9
        List<Chunk> chunks = aggregator.aggregate(List.of(file), limit, false);

        assertThat(chunks).hasSize(9);
    }

    @Test
    void shouldNotSplitFileExactlyAtLimit() {
        int limit = 200;
        int contentSize = limit - 80 - "Small.java".length();
        String content = "x".repeat(contentSize);
        FileInfo file = fileInfo("Small.java", content, contentSize);

        List<Chunk> chunks = aggregator.aggregate(List.of(file), limit, true);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().files().getFirst().isSplit()).isFalse();
    }

    @Test
    void shouldSplitFileJustAboveLimit() {
        int limit = 200;
        int contentSize = limit - 80 - "Small.java".length() + 1;
        String content = "x".repeat(contentSize);
        FileInfo file = fileInfo("Small.java", content, contentSize);

        List<Chunk> chunks = aggregator.aggregate(List.of(file), limit, true);

        assertThat(chunks).hasSizeGreaterThan(1);  // больше одной части
        assertThat(chunks.getFirst().files().getFirst().isSplit()).isTrue();

        for (Chunk chunk : chunks) {
            assertThat(chunk.totalSize()).isLessThanOrEqualTo(limit);
        }
    }

    private FileInfo fileInfo(String path, String content, int size) {
        return new FileInfo(Path.of(path), content, size, false, 0, 0);
    }
}