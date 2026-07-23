package ru.npepub.di;

import java.lang.reflect.Field;

/**
 * Injects a dependency into a single field of a bean.
 */
interface FieldInjector {

    /**
     * @param bean  the target bean instance
     * @param field the field to inject into
     */
    void inject(Object bean, Field field);
}