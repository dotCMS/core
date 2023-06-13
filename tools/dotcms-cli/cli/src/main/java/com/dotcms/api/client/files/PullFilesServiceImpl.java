package com.dotcms.api.client.files;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodeInfo;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.FilesUtils;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.language.Language;
import io.quarkus.arc.DefaultBean;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/**
 * Implementation of the PullFilesService interface for pulling files from the server.
 */
@DefaultBean
@Dependent
public class PullFilesServiceImpl implements PullFilesService {

    @Inject
    Logger logger;

    @Inject
    protected Downloader downloader;

    @Inject
    protected RestClientFactory clientFactory;

    /**
     * Pulls files from the server and saves them to the specified destination.
     *
     * @param output      the output option mixin for printing progress
     * @param tree        the tree node representing the file structure
     * @param destination the destination path to save the pulled files
     * @param overwrite   true to overwrite existing files, false otherwise
     */
    @ActivateRequestContext
    @Override
    public void pull(OutputOptionMixin output, final TreeNode tree, final String destination, final boolean overwrite) {

        // Collect important information about the tree
        final var treeNodeInfo = FilesUtils.CollectUniqueStatusesAndLanguages(tree);

        output.info(String.format("\rStarting pull process for: " +
                        "@|bold,green [%s]|@ Assets in " +
                        "@|bold,green [%s]|@ Folders and " +
                        "@|bold,green [%s]|@ Languages\n\n",
                treeNodeInfo.assetsCount(), treeNodeInfo.foldersCount(), treeNodeInfo.languages().size()));

        // ConsoleProgressBar instance to handle the download progress bar
        ConsoleProgressBar progressBar = new ConsoleProgressBar(output);

        CompletableFuture<Void> treeBuilderFuture = CompletableFuture.supplyAsync(
                () -> {
                    processTree(tree, treeNodeInfo, destination, overwrite, progressBar);
                    return null;
                });

        progressBar.setFuture(treeBuilderFuture);

        CompletableFuture<Void> animationFuture = CompletableFuture.runAsync(
                progressBar
        );

        // Waits for the completion of both the file system tree builder and console progress bar animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (treeBuilderFuture and animationFuture) have completed.
        CompletableFuture.allOf(treeBuilderFuture, animationFuture).join();
        try {
            treeBuilderFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            var errorMessage = String.format("Error occurred while pulling assets: [%s].", e.getMessage());
            logger.debug(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Processes the file tree by retrieving languages, checking the base structure,
     * and invoking the appropriate methods for processing the tree by status.
     *
     * @param tree         the tree node representing the file structure
     * @param treeNodeInfo the collected information about the tree
     * @param destination  the destination path to save the pulled files
     * @param overwrite    true to overwrite existing files, false otherwise
     * @param progressBar  the progress bar for tracking the pull progress
     */
    @ActivateRequestContext
    public void processTree(final TreeNode tree,
                            final TreeNodeInfo treeNodeInfo,
                            final String destination,
                            final boolean overwrite,
                            final ConsoleProgressBar progressBar) {

        try {

            // Make sure we have a valid destination
            var rootPath = checkBaseStructure(destination);

            // We need to retrieve the languages
            final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
            final List<Language> languages = languageAPI.list().entity();

            // Collect the list of unique statuses and languages
            final var uniqueLiveLanguages = treeNodeInfo.liveLanguages();
            final var uniqueWorkingLanguages = treeNodeInfo.workingLanguages();

            if (uniqueLiveLanguages.isEmpty() && uniqueWorkingLanguages.isEmpty()) {
                FilesUtils.FallbackDefaultLanguage(languages, uniqueLiveLanguages);
            }

            // Calculating the total number of steps
            progressBar.setTotalSteps(
                    treeNodeInfo.assetsCount()
            );

            // Sort the sets and convert them into lists
            List<String> sortedLiveLanguages = new ArrayList<>(uniqueLiveLanguages);
            Collections.sort(sortedLiveLanguages);

            List<String> sortedWorkingLanguages = new ArrayList<>(uniqueWorkingLanguages);
            Collections.sort(sortedWorkingLanguages);

            // Process the live tree
            processTreeByStatus(true, sortedLiveLanguages, tree, rootPath, overwrite, true, progressBar);
            // Process the working tree
            processTreeByStatus(false, sortedWorkingLanguages, tree, rootPath, overwrite, true, progressBar);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes the file tree for a specific status and language.
     *
     * @param isLive               true if processing live tree, false for working tree
     * @param sortedLanguages      the sorted list of languages
     * @param rootNode             the root node of the file tree
     * @param destination          the destination path to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param progressBar          the progress bar for tracking the pull progress
     */
    @ActivateRequestContext
    private void processTreeByStatus(boolean isLive, List<String> sortedLanguages, TreeNode rootNode,
                                     final String destination, final boolean overwrite,
                                     final boolean generateEmptyFolders, final ConsoleProgressBar progressBar) {

        if (sortedLanguages.isEmpty()) {
            return;
        }

        for (String lang : sortedLanguages) {

            // Filter the tree by status and language
            TreeNode filteredRoot = rootNode.cloneAndFilterAssets(isLive, lang, generateEmptyFolders);

            var rootPath = Paths.get(destination, FilesUtils.statusToString(isLive), lang, rootNode.folder().host());

            // ---
            var forkJoinPool = ForkJoinPool.commonPool();
            var task = new FileSystemTreeBuilderTask(
                    logger,
                    downloader,
                    filteredRoot,
                    rootPath.toString(),
                    overwrite,
                    generateEmptyFolders,
                    lang,
                    progressBar);
            forkJoinPool.invoke(task);
        }
    }

    /**
     * Checks the base structure of the destination path and creates the necessary directories.
     *
     * @param destination the destination path to save the pulled files
     * @return the root path for storing the files
     * @throws IOException if an I/O error occurs while creating directories
     */
    private String checkBaseStructure(final String destination) throws IOException {

        // For the pull of files, everything will be stored in a folder called "files"
        var filesFolder = Paths.get(destination, "files");

        // Create the folder if it does not exist
        if (!Files.exists(filesFolder)) {
            Files.createDirectories(filesFolder);
        }

        return filesFolder.toString();
    }

}
