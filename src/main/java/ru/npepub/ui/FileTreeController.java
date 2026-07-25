package ru.npepub.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import ru.npepub.model.FileInfo;

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
    public void populate(List<FileInfo> files) {
        TreeItem<String> root = new TreeItem<>("Проект");
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
        fileTree.setRoot(null);
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