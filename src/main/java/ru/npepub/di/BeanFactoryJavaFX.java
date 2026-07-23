package ru.npepub.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Creates JavaFX controller instances and injects their {@code @C2PInject} fields.
 * Controllers are not registered as beans — created on demand by FXMLLoader.
 */
class BeanFactoryJavaFX {

    private static final Logger log = LoggerFactory.getLogger(BeanFactoryJavaFX.class);

    private final BeanRegistry registry;
    private final DependencyInjector injector;

    BeanFactoryJavaFX(BeanRegistry registry, DependencyInjector injector) {
        this.registry = registry;
        this.injector = injector;
    }

    @SuppressWarnings("unchecked")
    <T> T create(Class<T> type) {
        try {
            T existing = (T) registry.getBeans(type).stream().findFirst().orElse(null);
            if (existing != null) return existing;

            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            injector.inject(instance);
            log.debug("Created controller: {}", type.getSimpleName());
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create controller: " + type.getName(), e);
        }
    }
}