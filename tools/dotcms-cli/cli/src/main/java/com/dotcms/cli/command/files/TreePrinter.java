package com.dotcms.cli.command.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.FilesUtils;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.language.Language;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The {@code TreePrinter} class provides a utility for printing a tree structure of
 * {@link AssetView}s and {@link FolderView}s. The tree can be filtered by language and asset status
 * (live or working).
 *
 * <p>This class uses the singleton pattern, and can be accessed by calling {@link #getInstance()}.
 */
public class TreePrinter {

    /**
     * The {@code TreePrinterHolder} class is used to implement the singleton pattern for the
     * {@link TreePrinter} class.
     */
    private static class TreePrinterHolder {
        private static final TreePrinter instance = new TreePrinter();
    }

    /**
     * Returns the singleton instance of the {@code TreePrinter} class.
     *
     * @return the singleton instance of the {@code TreePrinter} class.
     */
    public static TreePrinter getInstance() {
        return TreePrinterHolder.instance;
    }

    private TreePrinter() {
    }

    /**
     * Prints a filtered tree structure of the specified {@link TreeNode} to the provided
     * {@link StringBuilder}. The tree structure is filtered by language and asset status (live or
     * working).
     *
     * @param sb               the {@link StringBuilder} to append the tree structure to.
     * @param rootNode         the root node of the tree structure.
     * @param showEmptyFolders A boolean indicating whether to include empty folders in the tree. If
     *                         set to true, all folders will be included. If set to false, only
     *                         folders containing assets or having children with assets will be
     *                         included.
     */
    public void filteredFormat(StringBuilder sb,
            TreeNode rootNode,
            final boolean showEmptyFolders,
            final List<Language> languages) {

        // Collect the list of unique statuses and languages
        final var treeNodeInfo = rootNode.collectUniqueStatusesAndLanguages(showEmptyFolders);
        final var uniqueLiveLanguages = treeNodeInfo.liveLanguages();
        final var uniqueWorkingLanguages = treeNodeInfo.workingLanguages();

        if (uniqueLiveLanguages.isEmpty() && uniqueWorkingLanguages.isEmpty()) {
            FilesUtils.FallbackDefaultLanguage(languages, uniqueLiveLanguages);
        }

        // Sort the sets and convert them into lists
        List<String> sortedLiveLanguages = new ArrayList<>(uniqueLiveLanguages);
        Collections.sort(sortedLiveLanguages);

        List<String> sortedWorkingLanguages = new ArrayList<>(uniqueWorkingLanguages);
        Collections.sort(sortedWorkingLanguages);

        // Live tree
        formatByStatus(sb, true, sortedLiveLanguages, rootNode, showEmptyFolders);
        // Working tree
        formatByStatus(sb, false, sortedWorkingLanguages, rootNode, showEmptyFolders);
    }

    /**
     * Formats a StringBuilder object with the assets and their status in the given TreeNode
     * recursively using a short format for each asset in the sortedLanguages list, separated by
     * status.
     *
     * @param sb              The StringBuilder object to format.
     * @param isLive          A boolean indicating whether the status to format is Live or Working.
     * @param sortedLanguages A List of Strings containing the languages to include in the formatted
     *                        StringBuilder.
     * @param rootNode        The root TreeNode to start the formatting from.
     */
    private void formatByStatus(StringBuilder sb, boolean isLive, List<String> sortedLanguages,
            TreeNode rootNode, final boolean showEmptyFolders) {

        if (sortedLanguages.isEmpty()) {
            return;
        }

        // Calculate the parent path for this first node
        String parentPath = calculateRootParentPath(rootNode);
        Path initialPath;
        try {
            initialPath = Paths.get(parentPath);
        } catch (InvalidPathException e) {
            var error = String.format("Invalid folder path [%s] provided", parentPath);
            throw new IllegalArgumentException(error, e);
        }

        var status = FilesUtils.StatusToString(isLive);
        sb.append("\r ").append(status).append('\n');

        Iterator<String> langIterator = sortedLanguages.iterator();
        while (langIterator.hasNext()) {

            String lang = langIterator.next();
            TreeNode filteredRoot = rootNode.cloneAndFilterAssets(isLive, lang, showEmptyFolders);

            // Print the filtered tree using the format method starting from the filtered root itself
            boolean isLastLang = !langIterator.hasNext();
            sb.append("     ").
                    append((isLastLang ? "└── " : "├── ")).
                    append("\uD83C\uDF10 ").
                    append(lang).append('\n');

            // Add the domain and parent folder
            sb.append("     ").
                    append((isLastLang ? "    " : "│   ")).
                    append("└── ").
                    append(rootNode.folder().host()).
                    append('\n');

            if (parentPath.isEmpty() || parentPath.equals("/")) {
                format(sb,
                        "     " + (isLastLang ? "    " : "│   ") + "    ",
                        filteredRoot,
                        "     " + (isLastLang ? "    " : "│   ") + "    ",
                        true, true);
            } else {

                // If the initial path is not the root we need to try to print the folder structure it has
                var parentFolderIdent = "    ";
                for (var i = 0; i < initialPath.getNameCount(); i++) {
                    sb.append("     ").
                            append((isLastLang ? "    " : "│   ")).
                            append(parentFolderIdent).
                            append("└── ").
                            append(String.format("@|bold \uD83D\uDCC2 %s|@", initialPath.getName(i))).
                            append('\n');
                    if (i + 1 < initialPath.getNameCount()) {
                        parentFolderIdent += "    ";
                    }
                }

                format(sb,
                        parentFolderIdent + "     " + (isLastLang ? "" : "│   ") + "        ",
                        filteredRoot,
                        parentFolderIdent + "     " + (isLastLang ? "" : "│   ") + "        ",
                        true, true);
            }
        }
    }

    /**
     * Creates a short representation of the given node and its children as a string, with options
     * for including or excluding asset details and controlling the indentation and prefix for each
     * line.
     *
     * @param sb            The `StringBuilder` to append the string representation to.
     * @param prefix        The string prefix to use for each line in the representation, before any
     *                      symbols.
     * @param node          The `TreeNode` to create a string representation of.
     * @param indent        The string to use for each level of indentation in the representation.
     * @param isLastSibling Whether the `TreeNode` is the last sibling in the current level of the
     *                      representation.
     * @param includeAssets Whether to include asset details in the representation.
     */
    private void format(StringBuilder sb, String prefix, final TreeNode node,
            final String indent, boolean isLastSibling, boolean includeAssets) {

        String filePrefix;
        String nextIndent;
        if (!node.folder().name().equals("/")) {
            sb.append(prefix).
                    append(isLastSibling ? "└── " : "├── ").
                    append(String.format("@|bold \uD83D\uDCC2 %s|@", node.folder().name())).
                    append('\n');

            filePrefix = indent + (isLastSibling ? "    " : "│   ");
            nextIndent = indent + (isLastSibling ? "    " : "│   ");
        } else {
            filePrefix = indent + (isLastSibling ? "" : "│   ");
            nextIndent = indent + (isLastSibling ? "" : "│   ");
        }

        if (includeAssets) {
            // Adds the names of the node's files to the string representation.
            int assetCount = node.assets().size();
            for (int i = 0; i < assetCount; i++) {

                AssetView asset = node.assets().get(i);
                final var fileStr = String.format("%s", asset.name());
                boolean lastAsset = i == assetCount - 1 && node.children().isEmpty();

                sb.append(filePrefix).
                        append(lastAsset ? "└── " : "├── ").
                        append(fileStr).
                        append('\n');
            }
        }

        // Recursively creates string representations for the node's children.
        int childCount = node.children().size();
        for (int i = 0; i < childCount; i++) {
            TreeNode child = node.children().get(i);
            boolean lastSibling = i == childCount - 1;
            format(sb, filePrefix, child, nextIndent, lastSibling, includeAssets);
        }
    }

    /**
     * Calculates the parent path of the given root node.
     *
     * @param rootNode The root node to calculate the parent path from.
     * @return A String containing the root parent path.
     */
    private String calculateRootParentPath(TreeNode rootNode) {

        // Calculating the root folder path
        var folderPath = rootNode.folder().path();
        var folderName = rootNode.folder().name();

        // Determine if the folder path and folder name are empty or null
        var emptyFolderPath = folderPath == null
                || folderPath.isEmpty()
                || folderPath.equals("/");

        var emptyFolderName = folderName == null
                || folderName.isEmpty()
                || folderName.equals("/");

        // Remove firsts and last slash from folder path
        if (!emptyFolderPath) {
            folderPath = folderPath.
                    replaceAll("^/", "").
                    replaceAll("/$", "");
        }

        if (!emptyFolderName) {
            if (folderPath.endsWith(folderName)) {

                int folderIndex = folderPath.lastIndexOf(folderName);
                folderPath = folderPath.substring(0, folderIndex);

                folderPath = folderPath.
                        replaceAll("^/", "").
                        replaceAll("/$", "");
            }
        }

        return folderPath;
    }

}
