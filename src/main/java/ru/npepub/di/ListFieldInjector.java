package ru.npepub.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Injects a List of all beans of a given type.
 */
class ListFieldInjector implements FieldInjector {

    private static final Logger log = LoggerFactory.getLogger(ListFieldInjector.class);

    private final BeanRegistry registry;

    ListFieldInjector(BeanRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void inject(Object bean, Field field) {
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        Class<?> elementType = (Class<?>) genericType.getActualTypeArguments()[0];

        List<Object> list = registry.getAllInstances().stream()
                .filter(b -> elementType.isAssignableFrom(b.getClass()))
                .distinct()
                .collect(Collectors.toList());

        field.setAccessible(true);
        try {
            field.set(bean, list);
            log.debug("Injected List<{}>: {} beans", elementType.getSimpleName(), list.size());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject list: " + field.getName(), e);
        }
    }
}