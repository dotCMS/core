package com.dotcms.api.client.pull.file;

import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_EMPTY_FOLDERS;
import static com.dotcms.api.client.pull.file.OptionConstants.PRESERVE;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.pull.PullHandler;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.client.util.ErrorHandlingUtil;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodeInfo;
import com.dotcms.cli.command.files.TreePrinter;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.language.Language;
import com.dotcms.model.pull.PullOptions;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * The FilePullHandler class is responsible for pulling files from dotCMS. It extends the
 * PullHandler class and handles the pulling of FileTraverseResult objects providing its own
 * implementation of the pull method.
 */
@Dependent
public class FilePullHandler extends PullHandler<FileTraverseResult> {

    @Inject
    Logger logger;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ErrorHandlingUtil errorHandlerUtil;

    @Inject
    Puller puller;

    @Inject
    ManagedExecutor executor;

    @Override
    public String title() {
        return "Files";
    }

    @Override
    public String startPullingHeader(final List<FileTraverseResult> contents) {
        return String.format("\rPulling %s", title());
    }

    @Override
    public String shortFormat(final FileTraverseResult content, final PullOptions pullOptions) {

        boolean includeEmptyFolders = false;

        final var customOptions = pullOptions.customOptions();
        if (customOptions.isPresent()) {
            includeEmptyFolders = (boolean) customOptions.get().
                    getOrDefault(INCLUDE_EMPTY_FOLDERS, false);
        }

        var treeInfo = treeInfo(content, pullOptions, includeEmptyFolders);
        final TreeNode tree = treeInfo.getLeft();
        final TreeNodeInfo treeNodeInfo = treeInfo.getRight();

        final StringBuilder sb = new StringBuilder();
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
    public int pull(List<FileTraverseResult> contents,
            PullOptions pullOptions,
            OutputOptionMixin output) throws ExecutionException, InterruptedException {

        //Collect all exceptions from the returned contents
        final List<Exception> allExceptions = contents.stream().map(FileTraverseResult::exceptions)
                .flatMap(List::stream).collect(Collectors.toList());

        //Any failed TreeNode will not be present
        //So no need to separate the results

        //Save the error code for the traversal process. This will be used to determine the exit
        // code of the command if greater than 0 (zero)
        int errorCode = errorHandlerUtil.handlePullExceptions(allExceptions, output);

        boolean preserve = false;
        boolean includeEmptyFolders = false;

        final var customOptions = pullOptions.customOptions();
        if (customOptions.isPresent()) {
            preserve = (boolean) customOptions.get().getOrDefault(PRESERVE, false);
            includeEmptyFolders = (boolean) customOptions.get().
                    getOrDefault(INCLUDE_EMPTY_FOLDERS, false);
        }

        var isPullingHeaderDisplayed = false;

        for (final var content : contents) {

            // If the traversal process failed, there is no point in trying to pull the content
            if (!content.exceptions().isEmpty()) {
                continue;
            }

            if (!isPullingHeaderDisplayed) {
                output.info(startPullingHeader(contents));
                isPullingHeaderDisplayed = true;
            }

            var errors = pullTree(
                    content,
                    pullOptions,
                    output,
                    !preserve,
                    includeEmptyFolders
            );

            final int e = errorHandlerUtil.handlePullExceptions(errors, output);
            //This should always keep the highest error code
            // Meaning that if no errors occurred, the error code will be 0
            errorCode = Math.max(e, errorCode);

        }

        return errorCode;
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

        // If we don't generate empty folders, we need to check if there are assets to process
        // in the tree node, if not, we skip the pull process avoiding the creation of site
        // folders with no content.
        if (!generateEmptyFolders && treeNodeInfo.assetsCount() == 0) {
            return List.of();
        }

        // Display the header
        output.info(header(content, treeNodeInfo));

        // ConsoleProgressBar instance to handle the download progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);

        CompletableFuture<List<Exception>> treeBuilderFuture = executor.supplyAsync(
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

        CompletableFuture<Void> animationFuture = executor.runAsync(
                progressBar
        );

        final List<Exception> foundErrors;

        try {

            // Waits for the completion of both the file system tree builder and console progress bar animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (treeBuilderFuture and animationFuture) have completed.
            CompletableFuture.allOf(treeBuilderFuture, animationFuture).join();
            foundErrors = treeBuilderFuture.get();

        } catch (InterruptedException e) {
            var errorMessage = String.format(
                    "Error occurred while pulling assets: [%s].", e.getMessage()
            );
            logger.error(errorMessage, e);
            Thread.currentThread().interrupt();
            throw new PullException(errorMessage, e);
        } catch (ExecutionException | CompletionException e) {
            var cause = e.getCause();
            throw errorHandlerUtil.mapPullException(cause);
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

        var optionalTree = content.tree();
        var optionalAsset = content.asset();

        if (optionalTree.isPresent()) {

            // Collect important information about the tree
            tree = optionalTree.get();
            treeNodeInfo = tree.collectUniqueStatusAndLanguage(generateEmptyFolders);

        } else if (optionalAsset.isPresent()) {

            // Parsing and validating the given path
            var dotCMSPath = AssetsUtils.parseRemotePath(options.contentKey().orElseThrow());

            // Create a simple tree node for the asset to handle
            var asset = optionalAsset.get();
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

            return String.format("\r@|bold,green [%s]|@ - " +
                            "@|bold,green [%s]|@ Assets in " +
                            "@|bold,green [%s]|@ Folders and " +
                            "@|bold,green [%s]|@ Languages to pull\n\n",
                    treeNodeInfo.site(),
                    treeNodeInfo.assetsCount(),
                    treeNodeInfo.foldersCount(),
                    treeNodeInfo.languages().size());

        } else if (content.asset().isPresent()) {

            return String.format("\r@|bold,green [%s]|@ - " +
                            "@|bold,green [%s]|@ Assets in " +
                            "@|bold,green [%s]|@ Languages to pull\n\n",
                    treeNodeInfo.site(),
                    1,
                    treeNodeInfo.languages().size());
        } else {
            throw new IllegalStateException("Invalid state. Either tree or asset must be present.");
        }
    }

}
