package com.dotcms.api.client.pull.file;

import static com.dotcms.api.client.pull.file.OptionConstants.INCLUDE_EMPTY_FOLDERS;
import static com.dotcms.api.client.pull.file.OptionConstants.PRESERVE;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.files.traversal.LocalTraversalService;
import com.dotcms.api.client.pull.CustomPullHandler;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodeInfo;
import com.dotcms.cli.command.files.TreePrinter;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.FilesUtils;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import com.dotcms.model.asset.FolderView;
import com.dotcms.model.language.Language;
import com.dotcms.model.pull.PullOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private Logger logger;

    @Inject
    LocalTraversalService traversalService;

    @Inject
    private RestClientFactory clientFactory;

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
                () -> processTree(
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

        try {

            // Waits for the completion of both the file system tree builder and console progress bar animation tasks.
            // This line blocks the current thread until both CompletableFuture instances
            // (treeBuilderFuture and animationFuture) have completed.
            CompletableFuture.allOf(treeBuilderFuture, animationFuture).join();

            return treeBuilderFuture.get();

        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pulling assets: [%s].",
                    e.getMessage());
            logger.error(errorMessage, e);
            throw new PullException(errorMessage, e);
        }
    }

    /**
     * Processes the file tree by retrieving languages, checking the base structure, and invoking
     * the appropriate methods for processing the tree by status.
     *
     * @param tree                 the tree node representing the file structure
     * @param treeNodeInfo         the collected information about the tree
     * @param destination          the destination path to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param failFast             true to fail fast, false to continue on error
     * @param progressBar          the progress bar for tracking the pull progress
     */
    @ActivateRequestContext
    private List<Exception> processTree(final TreeNode tree,
            final TreeNodeInfo treeNodeInfo,
            final File destination,
            final boolean overwrite,
            final boolean generateEmptyFolders,
            final boolean failFast,
            final ConsoleProgressBar progressBar) {

        // Preparing the languages for the tree
        var treeLanguages = prepareLanguages(treeNodeInfo);

        // Calculating the total number of steps
        progressBar.setTotalSteps(
                treeNodeInfo.assetsCount()
        );

        // Sort the sets and convert them into lists
        List<String> sortedLiveLanguages = new ArrayList<>(treeLanguages.liveLanguages);
        Collections.sort(sortedLiveLanguages);

        List<String> sortedWorkingLanguages = new ArrayList<>(treeLanguages.workingLanguages);
        Collections.sort(sortedWorkingLanguages);

        // Process the live tree
        var errors = processTreeByStatus(true, sortedLiveLanguages, tree,
                destination.getAbsolutePath(),
                overwrite, generateEmptyFolders, failFast, progressBar);
        var foundErrors = new ArrayList<>(errors);

        // Process the working tree
        errors = processTreeByStatus(false, sortedWorkingLanguages, tree,
                destination.getAbsolutePath(),
                overwrite, generateEmptyFolders, failFast, progressBar);
        foundErrors.addAll(errors);

        return foundErrors;
    }

    /**
     * Processes the file tree for a specific status and language.
     *
     * @param isLive               true if processing live tree, false for working tree
     * @param languages            the list of languages
     * @param rootNode             the root node of the file tree
     * @param destination          the destination path to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param failFast             true to fail fast, false to continue on error
     * @param progressBar          the progress bar for tracking the pull progress
     */
    @ActivateRequestContext
    private List<Exception> processTreeByStatus(boolean isLive, List<String> languages,
            TreeNode rootNode,
            final String destination, final boolean overwrite,
            final boolean generateEmptyFolders, final boolean failFast,
            final ConsoleProgressBar progressBar) {

        var foundErrors = new ArrayList<Exception>();

        if (languages.isEmpty()) {
            return foundErrors;
        }

        for (String lang : languages) {
            var errors = traversalService.pullTreeNode(rootNode, destination, isLive, lang,
                    overwrite,
                    generateEmptyFolders, failFast, progressBar);
            foundErrors.addAll(errors);
        }

        return foundErrors;
    }

    /**
     * Prepares the languages used in the tree node. If there are no unique live languages or
     * working languages specified in the given `treeNodeInfo`, it fallbacks to the default language
     * available in the list of all languages.
     *
     * @param treeNodeInfo the collected information about the tree node
     * @return an instance of the {@link NodeLanguages} class containing the set of live languages
     * and working languages
     */
    private NodeLanguages prepareLanguages(TreeNodeInfo treeNodeInfo) {

        // We need to retrieve the languages
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final List<Language> languages = languageAPI.list().entity();

        // Collect the list of unique statuses and languages
        final var uniqueLiveLanguages = treeNodeInfo.liveLanguages();
        final var uniqueWorkingLanguages = treeNodeInfo.workingLanguages();

        // If there are no unique live languages or working languages, fallback to the default language available
        if (uniqueLiveLanguages.isEmpty() && uniqueWorkingLanguages.isEmpty()) {
            FilesUtils.fallbackDefaultLanguage(languages, uniqueLiveLanguages);
        }

        return new NodeLanguages(uniqueLiveLanguages, uniqueWorkingLanguages);
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

    private static class NodeLanguages {

        public final Set<String> liveLanguages;
        public final Set<String> workingLanguages;

        public NodeLanguages(Set<String> liveLanguages, Set<String> workingLanguages) {
            this.liveLanguages = liveLanguages;
            this.workingLanguages = workingLanguages;
        }
    }

}
