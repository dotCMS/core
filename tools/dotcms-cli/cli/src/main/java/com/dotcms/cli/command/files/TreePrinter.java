package com.dotcms.cli.command.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.model.asset.AssetView;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.language.Language;

import java.util.*;

/**
 * The {@code TreePrinter} class provides a utility for printing a tree structure of
 * {@link AssetView}s and {@link FolderView}s. The tree can be filtered by language and asset status
 * (live or working).
 *
 * <p>This class uses the singleton pattern, and can be accessed by calling {@link #getInstance()}.
 */
public class TreePrinter {

    private static final String STATUS_LIVE = "live";
    private static final String STATUS_WORKING = "working";

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
     * Helper method to generate a short string representation of a tree. The string representation
     * includes the names of the folder and all files under it in the format
     * {@code <filename> [<language>] (<status>)}.
     *
     * @param sb   the StringBuilder to which the string representation of the tree is appended
     * @param node the root of the tree that needs to be printed
     */
    public void shortFormat(StringBuilder sb, final TreeNode node) {
        shortFormat(sb, "", node, true, "    ", false);
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
    private void shortFormat(StringBuilder sb, String prefix, final TreeNode node,
            final boolean root, final String indent, boolean isLastSibling) {

        var folderNameStr = String.format("@|bold %s|@", node.folder().name());
        if (!root) {
            sb.append(prefix).append(isLastSibling ? "└── " : "├── ").append(folderNameStr)
                    .append('\n');
        } else {

            if (node.folder().name().equals("/")) {
                folderNameStr = String.format("@|bold %s|@", node.folder().host());
                sb.append("\r").append(folderNameStr).append('\n');
            } else {
                sb.append("\r").append(folderNameStr).append('\n');
            }
        }

        String filePrefix = indent + (root ? "    " : (isLastSibling ? "    " : "│   "));
        String nextIndent = indent + (root ? "    " : (isLastSibling ? "    " : "│   "));

        // Adds the names of the node's files to the string representation.
        int assetCount = node.assets().size();
        for (int i = 0; i < assetCount; i++) {
            AssetView asset = node.assets().get(i);
            final var fileStr = String.format("%s [%s] (%s)",
                    asset.name(),
                    asset.lang(),
                    asset.live() ? STATUS_LIVE : STATUS_WORKING);
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
        Set<String> uniqueLiveLanguages = new HashSet<>();
        Set<String> uniqueWorkingLanguages = new HashSet<>();

        collectUniqueStatusesAndLanguages(rootNode, uniqueLiveLanguages, uniqueWorkingLanguages);

        if (uniqueLiveLanguages.isEmpty() && uniqueWorkingLanguages.isEmpty()) {
            fallbackDefaultLanguage(languages, uniqueLiveLanguages);
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

        var status = isLive ? STATUS_LIVE : STATUS_WORKING;
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

            if (rootNode.folder().path().equals("/")) {
                format(sb,
                        "     " + (isLastLang ? "    " : "│   ") + "    ",
                        filteredRoot,
                        "     " + (isLastLang ? "    " : "│   ") + "    ",
                        true, true);
            } else {

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

        if (node.assets() != null) {
            for (AssetView asset : node.assets()) {
                if (asset.live()) {
                    uniqueLiveLanguages.add(asset.lang());
                } else {
                    uniqueWorkingLanguages.add(asset.lang());
                }
            }
        }

        for (TreeNode child : node.children()) {
            collectUniqueStatusesAndLanguages(child, uniqueLiveLanguages,
                    uniqueWorkingLanguages);
        }
    }

    /**
     * Fallbacks to the default language in case of no languages found scanning the assets.
     *
     * @throws RuntimeException if no default language is found in the list of languages
     */
    private void fallbackDefaultLanguage(
            final List<Language> languages, Set<String> uniqueLiveLanguages) {

        // Get the default language from the list of languages
        var defaultLanguage = languages.stream()
                .filter(language -> {
                    if (language.defaultLanguage().isPresent()) {
                        return language.defaultLanguage().get();
                    }

                    return false;
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No default language found"));

        uniqueLiveLanguages.add(defaultLanguage.isoCode());
    }

}
