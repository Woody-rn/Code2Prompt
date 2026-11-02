package ru.npepub.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.npepub.config.FilterConfig;
import ru.npepub.di.ContainerDI;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExcludedDirFilterTest {

    private DirectoryFilter filter;

    @BeforeEach
    void setUp() {
        System.setProperty("user.home", System.getProperty("java.io.tmpdir") + "/c2p-test");
        ContainerDI container = new ContainerDI();
        filter = container.get(DirectoryFilter.class);

        if (filter instanceof ConfigurableFilter c) {
            c.reloadConfig(FilterConfig.defaults());
        }
    }

    @Test
    void shouldSkipBuildDirectory() {
        assertThat(filter.shouldEnter(Path.of("project/build"))).isFalse();
    }

    @Test
    void shouldSkipBuildDirectoryAtAnyDepth() {
        assertThat(filter.shouldEnter(Path.of("project/module-a/build"))).isFalse();
        assertThat(filter.shouldEnter(Path.of("project/module-b/build"))).isFalse();
    }

    @Test
    void shouldSkipGitDirectory() {
        assertThat(filter.shouldEnter(Path.of("project/.git"))).isFalse();
    }

    @Test
    void shouldSkipGradleAndIdeaDirectories() {
        assertThat(filter.shouldEnter(Path.of("project/.gradle"))).isFalse();
        assertThat(filter.shouldEnter(Path.of("project/.idea"))).isFalse();
    }

    @Test
    void shouldSkipTargetAndOutDirectories() {
        assertThat(filter.shouldEnter(Path.of("project/target"))).isFalse();
        assertThat(filter.shouldEnter(Path.of("project/out"))).isFalse();
        assertThat(filter.shouldEnter(Path.of("project/dist"))).isFalse();
    }

    @Test
    void shouldEnterSrcDirectory() {
        assertThat(filter.shouldEnter(Path.of("project/src"))).isTrue();
    }

    @Test
    void shouldEnterResourcesDirectory() {
        assertThat(filter.shouldEnter(Path.of("project/src/main/resources"))).isTrue();
    }

    @Test
    void shouldNotFilterRootLikePath() {
        // Path with no name component (filesystem root) — must not crash.
        assertThat(filter.shouldEnter(Path.of("/"))).isTrue();
    }
}