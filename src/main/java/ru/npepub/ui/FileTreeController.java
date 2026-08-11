package ru.npepub.ui;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.npepub.model.FileInfo;
import ru.npepub.ui.model.FileNode;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the file tree panel.
 * Displays scanned files in a tree structure by their relative paths.
 */
public class FileTreeController {

    private static final Logger log = LoggerFactory.getLogger(FileTreeController.class);

    @FXML
    private TreeView<FileNode> fileTree;

    @Setter
    private Consumer<String> statusConsumer;

    /**
     * Initializes multi-selection mode for the file tree.
     */
    @FXML
    private void initialize() {
        fileTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileTree.setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.C).match(event)) {
                onCopySelectedFiles();
            }
        });
    }

    /**
     * Fills the tree with the given file infos.
     */
    public void populate(List<FileInfo> files, String projectName, Path rootDir) {
        if (rootDir == null) {
            rootDir = Path.of("");
        }

        String rootName = "Project - " + (projectName != null ? projectName : "");
        TreeItem<FileNode> root = new TreeItem<>(new FileNode(rootName, rootDir, true));
        root.setExpanded(true);

        for (FileInfo file : files) {
            addToTree(root, file.relativePath(), rootDir);
        }

        fileTree.setRoot(root);
    }

    /**
     * Clears the tree.
     */
    public void clear() {
        if (fileTree != null) {
            fileTree.setRoot(null);
        }
    }

    /**
     * Expands all folders in the tree.
     */
    @FXML
    private void onExpandAll() {
        if (fileTree != null && fileTree.getRoot() != null) {
            expandAll(fileTree.getRoot());
        }
    }

    /**
     * Collapses all folders in the tree.
     */
    @FXML
    private void onCollapseAll() {
        if (fileTree != null && fileTree.getRoot() != null) {
            collapseAll(fileTree.getRoot());
        }
    }

    /**
     * Copies the file tree structure to clipboard.
     */
    @FXML
    private void onCopyTree() {
        TreeItem<FileNode> root = fileTree.getRoot();
        if (root == null) return;

        StringBuilder sb = new StringBuilder();
        buildTreeString(root, sb, 0);

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(sb.toString()), null);
    }

    /**
     * Copies the contents of all selected files to clipboard.
     * Directories are silently skipped.
     */
    @FXML
    private void onCopySelectedFiles() {
        ObservableList<TreeItem<FileNode>> selected = fileTree.getSelectionModel().getSelectedItems();

        if (selected.isEmpty()) {
            setStatus("Выберите файлы для копирования");
            return;
        }

        StringBuilder result = new StringBuilder();
        int copied = 0;
        int skipped = 0;

        for (TreeItem<FileNode> item : selected) {
            FileNode node = item.getValue();
            if (node.isDirectory()) {
                skipped++;
                continue;
            }

            try {
                String content = Files.readString(node.path());
                result.append(content);
                copied++;
            } catch (IOException e) {
                log.error("Failed to read: {}", node.path(), e);
            }
        }

        if (copied == 0) {
            setStatus("Нет файлов для копирования");
            return;
        }

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(result.toString()), null);

        String message = "Скопировано " + copied + " файлов";
        if (skipped > 0) {
            message = "Папки пропущены, " + message.toLowerCase();
        }
        setStatus(message);
    }

    private void buildTreeString(TreeItem<FileNode> item, StringBuilder sb, int depth) {
        if (item == null || item.getValue() == null) return;
        sb.append("  ".repeat(depth))
                .append(item.getValue().isDirectory() ? "📁 " : "📄 ")
                .append(item.getValue().name())
                .append("\n");
        for (TreeItem<FileNode> child : item.getChildren()) {
            buildTreeString(child, sb, depth + 1);
        }
    }

    private void expandAll(TreeItem<FileNode> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<FileNode> child : item.getChildren()) {
            expandAll(child);
        }
    }

    private void collapseAll(TreeItem<FileNode> item) {
        if (item == null) return;
        item.setExpanded(false);
        for (TreeItem<FileNode> child : item.getChildren()) {
            collapseAll(child);
        }
    }

    private void addToTree(TreeItem<FileNode> root, Path relativePath, Path rootDir) {
        String[] parts = relativePath.toString().replace('\\', '/').split("/");
        TreeItem<FileNode> current = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean isFile = (i == parts.length - 1);
            Path fullPath;
            if (isFile) {
                fullPath = rootDir.resolve(relativePath);
            } else {
                StringBuilder pathBuilder = new StringBuilder();
                for (int j = 0; j <= i; j++) {
                    if (j > 0) pathBuilder.append("/");
                    pathBuilder.append(parts[j]);
                }
                fullPath = rootDir.resolve(pathBuilder.toString());
            }
            current = findOrCreateChild(current, part, fullPath, !isFile);
        }
    }

    private TreeItem<FileNode> findOrCreateChild(TreeItem<FileNode> parent, String name, Path fullPath, boolean isDirectory) {
        for (TreeItem<FileNode> child : parent.getChildren()) {
            if (child.getValue().name().equals(name)) {
                return child;
            }
        }
        FileNode node = new FileNode(name, fullPath, isDirectory);
        TreeItem<FileNode> newChild = new TreeItem<>(node);
        parent.getChildren().add(newChild);
        return newChild;
    }

    private void setStatus(String text) {
        if (statusConsumer != null) {
            statusConsumer.accept(text);
        }
    }
}