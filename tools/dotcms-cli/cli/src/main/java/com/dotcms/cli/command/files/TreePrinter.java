package com.dotcms.cli.command.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.model.asset.Asset;
import com.dotcms.model.asset.AssetsFolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The {@code TreePrinter} class provides a utility for printing a tree structure of {@link Asset}s
 * and {@link AssetsFolder}s. The tree can be filtered by language and asset status (live or
 * working).
 *
 * <p>This class uses the singleton pattern, and can be accessed by calling {@link #getInstance()}.
 */
public class TreePrinter {

    /**
     * The {@code TreePrinterHolder} class is used to implement the singleton pattern for the
     * {@link TreePrinter} class.
     */
    private static class TreePrinterHolder {

        private static TreePrinter instance = new TreePrinter();
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
     * Recursive helper method to generate a short string representation of a tree. The string
     * representation includes the names of the folder and all files under it in the format
     * {@code <filename> [<language>] (<status>)}. The string representation for each node is
     * prepended with a {@code prefix} string, and the {@code indent} string is used for
     * indentation. If {@code isLastSibling} is true, then the string representation will not
     * include a vertical bar. This method appends the string representation to the provided
     * {@code StringBuilder}.
     *
     * @param sb            the StringBuilder to which the string representation of the tree is
     *                      appended
     * @param prefix        the prefix string that is prepended to the string representation for
     *                      each node
     * @param node          the root of the tree that needs to be printed
     * @param root          true if the current node is the root of the tree, false otherwise
     * @param indent        the string used for indentation
     * @param isLastSibling true if the current node is the last sibling, false otherwise
     */
    public void shortFormat(StringBuilder sb, String prefix, final TreeNode node,
            final boolean root,
            final String indent, boolean isLastSibling) {

        var folderNameStr = String.format("@|bold \uD83D\uDDC0 %s|@", node.folder().name());
        if (!root) {
            sb.append(prefix).append(isLastSibling ? "└── " : "├── ").append(folderNameStr)
                    .append('\n');
        } else {
            sb.append("\r").append(folderNameStr).append('\n');
        }

        String filePrefix = indent + (root ? "    " : (isLastSibling ? "    " : "│   "));
        String nextIndent = indent + (root ? "    " : (isLastSibling ? "    " : "│   "));

        // Adds the names of the node's files to the string representation.
        int assetCount = node.assets().size();
        for (int i = 0; i < assetCount; i++) {
            Asset asset = node.assets().get(i);
            final var fileStr = String.format("%s [%s] (%s)",
                    asset.name(),
                    asset.lang(),
                    asset.live() ? "Live" : "Working");
            boolean lastAsset = i == assetCount - 1 && node.children().isEmpty();
            sb.append(filePrefix).append(lastAsset ? "└── " : "├── ").append(fileStr).append('\n');
        }

        // Recursively creates string representations for the node's children.
        int childCount = node.children().size();
        for (int i = 0; i < childCount; i++) {
            TreeNode child = node.children().get(i);
            boolean lastSibling = i == childCount - 1;
            shortFormat(sb, filePrefix, child, false, nextIndent, lastSibling);
        }
    }

    /**
     * Prints a filtered tree structure of the specified {@link TreeNode} to the provided
     * {@link StringBuilder}. The tree structure is filtered by language and asset status (live or
     * working).
     *
     * @param rootNode the root node of the tree structure.
     * @param sb       the {@link StringBuilder} to append the tree structure to.
     */
    public void filteredFormat(TreeNode rootNode, StringBuilder sb) {

        // Collect the list of unique statuses and languages
        Set<String> uniqueLiveLanguages = new HashSet<>();
        Set<String> uniqueWorkingLanguages = new HashSet<>();

        collectUniqueStatusesAndLanguages(rootNode, uniqueLiveLanguages, uniqueWorkingLanguages);

        // Sort the sets and convert them into lists
        List<String> sortedLiveLanguages = new ArrayList<>(uniqueLiveLanguages);
        Collections.sort(sortedLiveLanguages);

        List<String> sortedWorkingLanguages = new ArrayList<>(uniqueWorkingLanguages);
        Collections.sort(sortedWorkingLanguages);

        // Live tree
        formatByStatus(sb, true, sortedLiveLanguages, rootNode);
        // Working tree
        formatByStatus(sb, false, sortedWorkingLanguages, rootNode);
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
            TreeNode rootNode) {

        if (sortedLanguages.isEmpty()) {
            return;
        }

        var status = isLive ? "Live" : "Working";
        sb.append("\r ").append(status).append('\n');

        Iterator<String> langIterator = sortedLanguages.iterator();
        while (langIterator.hasNext()) {

            String lang = langIterator.next();
            TreeNode filteredRoot = rootNode.cloneAndFilterAssets(isLive, lang);

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
                    append(rootNode.folder().site()).
                    append('\n');
            sb.append("     ").
                    append((isLastLang ? "    " : "│   ")).
                    append("    └── ").
                    append(rootNode.folder().path()).
                    append('\n');

            format(sb,
                    "     " + (isLastLang ? "    " : "│   ") + "        ",
                    filteredRoot,
                    "     " + (isLastLang ? "    " : "│   ") + "        ",
                    true, true);
        }
    }

    /**
     * Traverses the given TreeNode recursively and collects the unique live and working languages
     * from its assets and its children's assets.
     *
     * @param node                   The root TreeNode to start the traversal from.
     * @param uniqueLiveLanguages    A Set to collect unique live languages found in the assets of
     *                               the TreeNode and its children.
     * @param uniqueWorkingLanguages A Set to collect unique working languages found in the assets
     *                               of the TreeNode and its children.
     */
    private void collectUniqueStatusesAndLanguages(
            TreeNode node, Set<String> uniqueLiveLanguages, Set<String> uniqueWorkingLanguages) {

        for (Asset asset : node.assets()) {
            if (asset.live()) {
                uniqueLiveLanguages.add(asset.lang());
            } else {
                uniqueWorkingLanguages.add(asset.lang());
            }
        }

        for (TreeNode child : node.children()) {
            collectUniqueStatusesAndLanguages(child, uniqueLiveLanguages, uniqueWorkingLanguages);
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

        var folderNameStr = String.format("@|bold \uD83D\uDCC2 %s|@", node.folder().name());

        boolean shouldPrintFolder = !node.assets().isEmpty() || hasAssetsInSubtree(node);

        if (shouldPrintFolder) {
            sb.append(prefix).append(isLastSibling ? "└── " : "├── ").append(folderNameStr)
                    .append('\n');
        }

        String filePrefix = indent + (isLastSibling ? "    " : "│   ");
        String nextIndent = indent + (isLastSibling ? "    " : "│   ");

        if (includeAssets) {
            // Adds the names of the node's files to the string representation.
            int assetCount = node.assets().size();
            for (int i = 0; i < assetCount; i++) {

                Asset asset = node.assets().get(i);
                /*final var fileStr = String.format("%s [%s] (%s)", asset.name(), asset.lang(),
                        asset.live());*/
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

    private boolean hasAssetsInSubtree(TreeNode node) {

        return true;
        /*if (!node.assets().isEmpty()) {
            return true;
        }

        for (TreeNode child : node.children()) {
            if (hasAssetsInSubtree(child)) {
                return true;
            }
        }

        return false;*/
    }


}
