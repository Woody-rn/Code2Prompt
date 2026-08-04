package ru.npepub.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import ru.npepub.model.FileInfo;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * Controller for the file tree panel.
 * Displays scanned files in a tree structure by their relative paths.
 */
public class FileTreeController {

    @FXML private TreeView<String> fileTree;

    /**
     * Fills the tree with the given file infos.
     */
    public void populate(List<FileInfo> files, String projectName) {
        String rootName = "Project - " + (projectName != null ? projectName : "");
        TreeItem<String> root = new TreeItem<>(rootName);
        root.setExpanded(true);

        for (FileInfo file : files) {
            addToTree(root, file.relativePath().toString());
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
        TreeItem<String> root = fileTree.getRoot();
        if (root == null) return;

        StringBuilder sb = new StringBuilder();
        buildTreeString(root, sb, 0);

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(sb.toString()), null);
    }

    private void buildTreeString(TreeItem<String> item, StringBuilder sb, int depth) {
        if (item == null || item.getValue() == null) return;
        sb.append("  ".repeat(depth))
                .append(item.isLeaf() ? "📄 " : "📁 ")
                .append(item.getValue())
                .append("\n");
        for (TreeItem<String> child : item.getChildren()) {
            buildTreeString(child, sb, depth + 1);
        }
    }

    private void expandAll(TreeItem<?> item) {
        if (item == null || item.isLeaf()) return;
        item.setExpanded(true);
        for (TreeItem<?> child : item.getChildren()) {
            expandAll(child);
        }
    }

    private void collapseAll(TreeItem<?> item) {
        if (item == null || item.isLeaf()) return;
        item.setExpanded(false);
        for (TreeItem<?> child : item.getChildren()) {
            collapseAll(child);
        }
    }

    private void addToTree(TreeItem<String> root, String path) {
        String[] parts = path.replace('\\', '/').split("/");
        TreeItem<String> current = root;

        for (String part : parts) {
            current = findOrCreateChild(current, part);
        }
    }

    private TreeItem<String> findOrCreateChild(TreeItem<String> parent, String name) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (child.getValue().equals(name)) return child;
        }
        TreeItem<String> newChild = new TreeItem<>(name);
        parent.getChildren().add(newChild);
        return newChild;
    }
}