package com.dotcms.api.client.pull.file;

import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_EMPTY_FOLDERS;
import static com.dotcms.api.client.pull.file.OptionConstants.PRESERVE;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.pull.CustomPullHandler;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodeInfo;
import com.dotcms.cli.command.files.TreePrinter;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.language.Language;
import com.dotcms.model.pull.PullOptions;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

@Dependent
public class FilePullHandler implements CustomPullHandler<FileTraverseResult> {

    @Inject
    Logger logger;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    Puller puller;

    @Override
    public String title() {
        return "Files";
    }

    @Override
    public String startPullingHeader(List<FileTraverseResult> contents) {
        return String.format("\rPulling %s", title());
    }

    @Override
    public String shortFormat(final FileTraverseResult content, final PullOptions pullOptions,
            Map<String, Object> customOptions) {

        boolean includeEmptyFolders = false;

        if (customOptions != null) {
            includeEmptyFolders = (boolean) customOptions.getOrDefault(INCLUDE_EMPTY_FOLDERS,
                    false);
        }

        var treeInfo = treeInfo(content, pullOptions, includeEmptyFolders);
        final TreeNode tree = treeInfo.getLeft();
        final TreeNodeInfo treeNodeInfo = treeInfo.getRight();

        StringBuilder sb = new StringBuilder();
        sb.append(header(content, treeNodeInfo));

        // We need to retrieve the languages
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final List<Language> languages = languageAPI.list().entity();

        // Display the result
        TreePrinter.getInstance().filteredFormat(
                sb,
                tree,
                includeEmptyFolders,
                languages
        );

        return sb.toString();
    }

    @Override
    @ActivateRequestContext
    public List<Exception> pull(FileTraverseResult content, final PullOptions pullOptions,
            Map<String, Object> customOptions, final OutputOptionMixin output) {

        boolean preserve = false;
        boolean includeEmptyFolders = false;

        if (customOptions != null) {
            preserve = (boolean) customOptions.getOrDefault(PRESERVE, false);
            includeEmptyFolders = (boolean) customOptions.getOrDefault(INCLUDE_EMPTY_FOLDERS,
                    false);
        }

        return pullTree(content, pullOptions, output, !preserve, includeEmptyFolders);
    }

    /**
     * This method pulls the tree of assets from the given content and options.
     *
     * @param content              The file traverse result.
     * @param options              The pull options.
     * @param output               The output option mixin.
     * @param overwrite            Indicates whether to overwrite existing assets.
     * @param generateEmptyFolders Indicates whether to generate empty folders.
     * @return A list of exceptions that occurred during the pulling process.
     */
    private List<Exception> pullTree(
            FileTraverseResult content,
            final PullOptions options,
            final OutputOptionMixin output,
            final boolean overwrite,
            final boolean generateEmptyFolders) {

        var treeInfo = treeInfo(content, options, generateEmptyFolders);
        final TreeNode tree = treeInfo.getLeft();
        final TreeNodeInfo treeNodeInfo = treeInfo.getRight();

        // Display the header
        output.info(header(content, treeNodeInfo));

        // ConsoleProgressBar instance to handle the download progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);

        CompletableFuture<List<Exception>> treeBuilderFuture = CompletableFuture.supplyAsync(
                () -> puller.pull(
                        tree,
                        treeNodeInfo,
                        options.destination(),
                        overwrite,
                        generateEmptyFolders,
                        options.failFast(),
                        progressBar
                ));

        progressBar.setFuture(treeBuilderFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        final List<Exception> foundErrors;

        try {

            // Waits for the completion of both the file system tree builder and console progress bar animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (treeBuilderFuture and animationFuture) have completed.
            CompletableFuture.allOf(treeBuilderFuture, animationFuture).join();
            foundErrors = treeBuilderFuture.get();

        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pulling assets: [%s].",
                    e.getMessage());
            logger.error(errorMessage, e);
            throw new PullException(errorMessage, e);
        }

        output.info(String.format("%n"));
        return foundErrors;
    }

    /**
     * Retrieves the tree node information and generates a tree node object based on the given
     * `content`, `options`, and `generateEmptyFolders` parameters.
     * <p>
     * If the `content` parameter contains a tree, it collects important information about the tree
     * and creates a TreeNodeInfo object.
     * <p>
     * If the `content` parameter contains an asset, it parses and validates the given path, creates
     * a simple tree node for the asset, and collects important information about the tree. The
     * `generateEmptyFolders` parameter is ignored in this case.
     *
     * @param content              the result of traversing the file system or an asset
     * @param options              the pull options for retrieving the content
     * @param generateEmptyFolders a flag to indicate whether empty folders should be generated
     * @return a Pair object containing the generated tree node and its information
     * @throws IllegalStateException if both tree and asset are absent in the `content` parameter
     */
    private Pair<TreeNode, TreeNodeInfo> treeInfo(FileTraverseResult content,
            final PullOptions options, final boolean generateEmptyFolders) {

        final TreeNode tree;
        final TreeNodeInfo treeNodeInfo;

        if (content.tree().isPresent()) {

            tree = content.tree().get();

            // Collect important information about the tree
            treeNodeInfo = tree.collectUniqueStatusAndLanguage(generateEmptyFolders);

        } else if (content.asset().isPresent()) {

            var asset = content.asset().get();

            // Parsing and validating the given path
            var dotCMSPath = AssetsUtils.parseRemotePath(options.contentKey().get());

            // Create a simple tree node for the asset to handle
            var folder = FolderView.builder()
                    .host(dotCMSPath.site())
                    .path(dotCMSPath.folderPath().toString())
                    .name(dotCMSPath.folderName())
                    .assets(asset)
                    .build();
            tree = new TreeNode(folder);

            // Collect important information about the tree
            treeNodeInfo = tree.collectUniqueStatusAndLanguage(false);
        } else {
            throw new IllegalStateException("Invalid state. Either tree or asset must be present.");
        }

        return Pair.of(tree, treeNodeInfo);
    }

    /**
     * Generates the header string for a given `content` and `treeNodeInfo`.
     * <p>
     * If `content` is a tree, the header includes the number of assets, number of folders, and
     * number of languages in the tree node.
     * <p>
     * If `content` is an asset, the header includes the number of assets and number of languages in
     * the tree node.
     *
     * @param content      the file traverse result (either a FileInfo or TreeInfo)
     * @param treeNodeInfo the collected information about the tree node
     * @return the formatted header string
     * @throws IllegalStateException if both `content.tree()` and `content.asset()` are absent
     */
    private String header(FileTraverseResult content, TreeNodeInfo treeNodeInfo) {

        if (content.tree().isPresent()) {

            return String.format("\r@|bold,green [%s]|@ Assets in " +
                            "@|bold,green [%s]|@ Folders and " +
                            "@|bold,green [%s]|@ Languages to pull\n\n",
                    treeNodeInfo.assetsCount(),
                    treeNodeInfo.foldersCount() == 0 ? 1 : treeNodeInfo.foldersCount(),
                    treeNodeInfo.languages().size());

        } else if (content.asset().isPresent()) {

            return String.format("\r@|bold,green [%s]|@ Assets in " +
                            "@|bold,green [%s]|@ Languages to pull\n\n",
                    1, treeNodeInfo.languages().size());
        } else {
            throw new IllegalStateException("Invalid state. Either tree or asset must be present.");
        }
    }

}
