package ru.npepub.ui.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.config.*;
import ru.npepub.di.api.C2PComponent;
import ru.npepub.di.api.C2PInject;

import java.util.*;

/**
 * Manages task templates: built-in and custom.
 */
@C2PComponent
public class TaskTemplateManager {

    private static final Logger log = LoggerFactory.getLogger(TaskTemplateManager.class);

    private static final List<String> BUILT_IN_TASKS = List.of(
            "task.find_bugs", "task.refactor", "task.documentation",
            "task.explain", "task.tests", "task.custom"
    );

    @C2PInject
    private ConfigPort configPort;

    /** Returns built-in task display names. */
    public List<String> getBuiltInTaskNames(ResourceBundle messages) {
        return BUILT_IN_TASKS.stream()
                .map(messages::getString)
                .toList();
    }

    /** Returns prompt text for a task name, or null if not found. */
    public String getPromptForTask(String taskName, ResourceBundle messages) {
        if (taskName == null) return null;
        if (taskName.equals(messages.getString("task.custom"))) return "";
        String key = getBuiltInPromptKey(taskName, messages);
        if (key != null) return messages.getString(key);
        return getCustomTemplates().get(taskName);
    }

    /** Returns custom templates map (mutable copy). */
    public Map<String, String> getCustomTemplates() {
        return new LinkedHashMap<>(configPort.load().prompt().customTemplates());
    }

    /** Saves a new custom template. If name exists, updates it. */
    public void saveOrUpdateTemplate(String name, String text) {
        AppConfig config = configPort.load();
        Map<String, String> templates = new LinkedHashMap<>(config.prompt().customTemplates());
        templates.put(name, text);
        configPort.save(withTemplates(config, templates));
        log.debug("Template saved: {}", name);
    }

    /** Deletes a custom template by name. */
    public void deleteTemplate(String name) {
        AppConfig config = configPort.load();
        Map<String, String> templates = new LinkedHashMap<>(config.prompt().customTemplates());
        templates.remove(name);
        configPort.save(withTemplates(config, templates));
        log.debug("Template deleted: {}", name);
    }

    /** Returns true if the task name is a built-in task. */
    public boolean isBuiltIn(String taskName, ResourceBundle messages) {
        return getBuiltInPromptKey(taskName, messages) != null;
    }

    private String getBuiltInPromptKey(String taskName, ResourceBundle messages) {
        for (String key : BUILT_IN_TASKS) {
            if (messages.getString(key).equals(taskName)) {
                return key.replace("task.", "prompt.");
            }
        }
        return null;
    }

    private AppConfig withTemplates(AppConfig config, Map<String, String> templates) {
        return new AppConfig(
                config.aiModel(), config.paths(), config.filter(), config.log(),
                new PromptConfig(config.prompt().systemPrompt(),
                        config.prompt().partPrefixTemplate(),
                        config.prompt().finalPartTemplate(),
                        config.prompt().fileSeparator(),
                        templates),
                config.debugMode()
        );
    }
}