package com.dotcms.cli.command.files;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.file.File;
import picocli.CommandLine;

public abstract class AbstractFilesCommand {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOption;

    /**
     * Appends a short-format string representation of the given tree node to the specified
     * StringBuilder. The short format includes only the name of the node's folder and the names of
     * its files, without any additional information such as sizes or modification times.
     *
     * @param sb     The StringBuilder to append the string representation to.
     * @param prefix The prefix to add to the start of each line of the string representation.
     * @param node   The tree node to create a string representation for.
     * @param root   Whether the given node is the root node of the tree.
     */
    void shortFormat(StringBuilder sb, String prefix, final TreeNode node, final boolean root) {

        final String folderStr;
        if (root) {
            folderStr = String.format("\r@|bold,yellow \uD83D\uDCC2 %s|@",
                    node.folder().name());
        } else {
            folderStr = String.format("@|bold,yellow \uD83D\uDCC2 %s|@",
                    node.folder().name());
        }
        sb.append(prefix).append(folderStr).append('\n');

        String newPrefix = prefix + "    ";

        // Adds the names of the node's files to the string representation.
        for (File file : node.files()) {
            final var fileStr = String.format("@|green \uD83D\uDCC4|@ %s", file.fileName());
            sb.append(newPrefix).append(fileStr).append('\n');
        }

        // Recursively creates string representations for the node's children.
        for (TreeNode child : node.children()) {
            shortFormat(sb, newPrefix, child, false);
        }
    }

}
