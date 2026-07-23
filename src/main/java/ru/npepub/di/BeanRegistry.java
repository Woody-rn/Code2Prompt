package ru.npepub.di;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores beans indexed by their types (interfaces and class).
 */
class BeanRegistry {

    private final Map<Class<?>, List<Object>> beans = new HashMap<>();

    public void register(Class<?> type, Object instance) {
        beans.computeIfAbsent(type, k -> new ArrayList<>()).add(instance);
    }

    public List<Object> getBeans(Class<?> type) {
        return beans.getOrDefault(type, List.of());
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        List<Object> list = beans.get(type);
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("No bean found for type: " + type.getName());
        }
        return (T) list.getFirst();
    }

    public List<Object> getAllInstances() {
        return beans.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

}