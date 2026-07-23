package ru.npepub.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Creates bean instances from classes annotated with {@code @C2PComponent}
 * and registers them in the {@link BeanRegistry}.
 */
class BeanFactoryC2P {

    private static final Logger log = LoggerFactory.getLogger(BeanFactoryC2P.class);

    private final BeanRegistry registry;

    BeanFactoryC2P(BeanRegistry registry) {
        this.registry = registry;
    }

    void create(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();

            for (Class<?> iface : clazz.getInterfaces()) {
                registry.register(iface, instance);
            }
            registry.register(clazz, instance);

            log.debug("Created: {} -> {}", clazz.getSimpleName(), instance.getClass().getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
        }
    }
}