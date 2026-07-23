package ru.npepub.di;

import ru.npepub.di.api.C2PInject;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Orchestrates injection of dependencies into beans.
 */
class DependencyInjector {

    private final FieldInjector singleInjector;
    private final FieldInjector listInjector;
    private final BeanRegistry registry;

    DependencyInjector(BeanRegistry registry) {
        this.registry = registry;
        this.singleInjector = new SingleFieldInjector(registry);
        this.listInjector = new ListFieldInjector(registry);
    }

    /**
     * Injects dependencies into all registered beans.
     */
    void injectAll() {
        for (Object bean : registry.getAllInstances()) {
            inject(bean);
        }
    }

    /**
     * Injects dependencies into all annotated fields of a single bean.
     */
    void inject(Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(C2PInject.class)) {
                FieldInjector injector = List.class.isAssignableFrom(field.getType())
                        ? listInjector
                        : singleInjector;
                injector.inject(bean, field);
            }
        }
    }
}