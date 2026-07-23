package ru.npepub.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.di.api.C2PComponent;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Scans the classpath for classes annotated with @C2PComponent.
 */
class ClassPathScanner {

    private static final Logger log = LoggerFactory.getLogger(ClassPathScanner.class);

    public List<Class<?>> scan(String basePackage) {
        log.info("Scanning package: {}", basePackage);
        try {
            String path = basePackage.replace('.', '/');
            Enumeration<URL> resources = ClassPathScanner.class.getClassLoader().getResources(path);
            List<Class<?>> componentClasses = new ArrayList<>();

            while (resources.hasMoreElements()) {
                URL packageUrl = resources.nextElement();
                log.debug("Found package at: {}", packageUrl);
                if ("file".equals(packageUrl.getProtocol())) {
                    File directory = new File(packageUrl.toURI());
                    componentClasses.addAll(scanDirectory(directory, basePackage));
                }
            }
            return componentClasses;
        } catch (Exception e) {
            throw new RuntimeException("Component scan failed", e);
        }
    }

    private List<Class<?>> scanDirectory(File directory, String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = basePackage + "." + file.getName();
                classes.addAll(scanDirectory(file, subPackage));
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
}