package ru.npepub.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;
import ru.npepub.di.api.C2PPrimary;

import java.util.List;

/**
 * Micro-DI container for Code2Prompt.
 * <p>
 * Scans the classpath for classes annotated with {@link C2PComponent},
 * instantiates them, and injects fields marked with {@link C2PInject}.
 * Supports {@code List<T>} injection and {@link C2PPrimary} for multiple implementations.
 * <p>
 * <b>Limitations (by design, KISS):</b>
 * <ul>
 *   <li>Field injection only — constructor and setter injection are not supported.</li>
 *   <li>No-arg constructor required on every component.</li>
 *   <li>Singleton scope only.</li>
 *   <li>Classpath scanning only — may fail with custom classloaders or JPMS modules.</li>
 *   <li>Multiple implementations of the same interface are allowed.
 *       Use {@code @C2PPrimary} to mark the default, or inject as {@code List<T>}.</li>
 *   <li>When injecting {@code List<T>}, the target bean itself is excluded
 *       to prevent self-injection and infinite recursion.</li>
 *   <li>No lifecycle callbacks.</li>
 *   <li>No AOP, proxies, transactions.</li>
 *   <li>No circular dependency detection.</li>
 *   <li>Not thread-safe during initialization.</li>
 * </ul>
 */
public class ContainerDI {

    private static final Logger log = LoggerFactory.getLogger(ContainerDI.class);

    private final BeanRegistry registry;
    private final BeanFactoryJavaFX beanFactoryJavaFX;

    public ContainerDI() {
        log.info("Initializing DI container");

        this.registry = new BeanRegistry();
        BeanFactoryC2P beanFactoryC2P = new BeanFactoryC2P(registry);
        DependencyInjector injector = new DependencyInjector(registry);
        this.beanFactoryJavaFX = new BeanFactoryJavaFX(registry, injector);
        ClassPathScanner scanner = new ClassPathScanner();

        List<Class<?>> classes = scanner.scan("ru.npepub");
        for (Class<?> clazz : classes) {
            beanFactoryC2P.create(clazz);
        }
        injector.injectAll();
        registry.register(ContainerDI.class, this);
    }

    /**
     * Returns a bean by its type.
     */
    public <T> T get(Class<T> type) {
        return registry.get(type);
    }

    /**
     * Creates a JavaFX controller and injects its dependencies.
     */
    public <T> T createController(Class<T> type) {
        return beanFactoryJavaFX.create(type);
    }
}