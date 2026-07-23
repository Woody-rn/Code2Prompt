package ru.npepub.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PPrimary;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Injects a single bean into a field.
 * Requires {@code @C2PPrimary} if multiple candidates exist.
 */
class SingleFieldInjector implements FieldInjector {

    private static final Logger log = LoggerFactory.getLogger(SingleFieldInjector.class);

    private final BeanRegistry registry;

    SingleFieldInjector(BeanRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void inject(Object bean, Field field) {
        List<Object> candidates = registry.getBeans(field.getType());
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "No bean found for injection: " + field.getType().getName() +
                            " in " + bean.getClass().getName()
            );
        }

        Object selected = resolve(candidates, field, bean);
        setField(bean, field, selected);
    }

    private Object resolve(List<Object> candidates, Field field, Object bean) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        List<Object> primaries = candidates.stream()
                .filter(c -> c.getClass().isAnnotationPresent(C2PPrimary.class))
                .toList();

        if (primaries.isEmpty()) {
            throw new IllegalStateException(
                    "Multiple beans found for " + field.getType().getName() +
                            " in " + bean.getClass().getName() +
                            ". Mark one with @C2PPrimary."
            );
        }
        if (primaries.size() > 1) {
            throw new IllegalStateException(
                    "Multiple @C2PPrimary beans found for " + field.getType().getName()
            );
        }
        return primaries.get(0);
    }

    private void setField(Object bean, Field field, Object value) {
        field.setAccessible(true);
        try {
            field.set(bean, value);
            log.debug("Injected: {} -> {}", value.getClass().getSimpleName(), field.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject: " + field.getName(), e);
        }
    }
}