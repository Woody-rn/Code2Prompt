package ru.npepub.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * Micro-DI container for Code2Prompt.
 * <p>
 * Scans the classpath for classes annotated with {@link C2PComponent},
 * instantiates them, and injects fields marked with {@link C2PInject}.
 * <p>
 * <b>Limitations (by design, KISS):</b>
 * <ul>
 *   <li>Only one implementation per interface — a second bean silently overwrites the first.</li>
 *   <li>Field injection only — constructor and setter injection are not supported.</li>
 *   <li>No-arg constructor required on every component.</li>
 *   <li>Singleton scope only — no prototype, request, or session scopes.</li>
 *   <li>Classpath scanning only — may fail with custom classloaders or JPMS modules.</li>
 *   <li>No lifecycle callbacks ({@code @PostConstruct} / {@code @PreDestroy}).</li>
 *   <li>No AOP, proxies, transactions, caching, or interceptors.</li>
 *   <li>No circular dependency detection — if A injects B and B injects A,
 *       one field stays {@code null}.</li>
 *   <li>Not thread-safe during initialization — call the constructor once
 *       from a single thread.</li>
 * </ul>
 * The container fits in ~100 lines, has zero external dependencies, and is
 * perfectly adequate for a desktop utility.
 */
public class ContainerDI {

    private static final Logger log = LoggerFactory.getLogger(ContainerDI.class);

    private final Map<Class<?>, Object> beans = new HashMap<>();

    public ContainerDI() {
        scanAndRegister("ru.npepub");
        injectDependencies();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        T bean = (T) beans.get(type);
        if (bean == null) {
            throw new IllegalStateException("No bean found for type: " + type.getName());
        }
        return bean;
    }

    private void scanAndRegister(String basePackage) {
        log.info("Scanning package: {}", basePackage);

        try {
            URL packageUrl = getPackageUrl(basePackage);

            File directory = new File(packageUrl.toURI());

            List<Class<?>> componentClasses = findComponentClasses(directory, basePackage);

            for (Class<?> clazz : componentClasses) {
                register(clazz);
            }

            log.info("Registered {} beans", beans.size());
        } catch (Exception e) {
            throw new RuntimeException("Component scan failed", e);
        }
    }

    private URL getPackageUrl(String basePackage) {
        String path = basePackage.replace('.', '/');
        URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(path);
        if (packageUrl == null) {
            throw new RuntimeException("Package not found: " + basePackage);
        }
        return packageUrl;
    }

    private List<Class<?>> findComponentClasses(File directory, String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = basePackage + "." + file.getName();
                classes.addAll(findComponentClasses(file, subPackage));
            } else if (file.getName().endsWith(".class")) {
                String className = basePackage + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(C2PComponent.class)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to load class: {}", className, e);
                }
            }
        }
        return classes;
    }

    private void register(Class<?> clazz) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();

            for (Class<?> iface : clazz.getInterfaces()) {
                beans.put(iface, instance);
            }
            beans.put(clazz, instance);

            log.debug("Registered: {} -> {}", clazz.getSimpleName(), instance.getClass().getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + clazz.getName(), e);
        }
    }

    private void injectDependencies() {
        for (Object bean : beans.values()) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(C2PInject.class)) {
                    Object dependency = beans.get(field.getType());
                    if (dependency == null) {
                        throw new IllegalStateException(
                                "No bean found for injection: " + field.getType().getName() +
                                        " in " + bean.getClass().getName()
                        );
                    }
                    field.setAccessible(true);
                    try {
                        field.set(bean, dependency);
                        log.debug("Injected: {} -> {}", dependency.getClass().getSimpleName(), field.getName());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to inject: " + field.getName(), e);
                    }
                }
            }
        }
    }
}