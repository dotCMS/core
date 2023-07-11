package com.dotcms.api.client.files;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.files.traversal.Downloader;
import com.dotcms.api.client.files.traversal.FileSystemTreeBuilderTask;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.api.traversal.TreeNodeInfo;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.FilesUtils;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.language.Language;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.jboss.logging.Logger;

public class PullBase {

    @Inject
    protected Logger logger;

    @Inject
    protected Downloader downloader;

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    protected WorkspaceManager workspaceManager;

    /**
     * Processes the file tree by retrieving languages, checking the base structure,
     * and invoking the appropriate methods for processing the tree by status.
     *
     * @param tree                 the tree node representing the file structure
     * @param treeNodeInfo         the collected information about the tree
     * @param destination          the destination path to save the pulled files
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param progressBar          the progress bar for tracking the pull progress
     */
    @ActivateRequestContext
    protected void processTree(final TreeNode tree,
                               final TreeNodeInfo treeNodeInfo,
                               final String destination,
                               final boolean overwrite,
                               final boolean generateEmptyFolders,
                               final ConsoleProgressBar progressBar) {

        try {

            // Make sure we have a valid destination
            var rootPath = checkBaseStructure(destination);

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
            processTreeByStatus(true, sortedLiveLanguages, tree, rootPath, overwrite, generateEmptyFolders, progressBar);
            // Process the working tree
            processTreeByStatus(false, sortedWorkingLanguages, tree, rootPath, overwrite, generateEmptyFolders, progressBar);

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
    protected void processTreeByStatus(boolean isLive, List<String> sortedLanguages, TreeNode rootNode,
                                       final String destination, final boolean overwrite,
                                       final boolean generateEmptyFolders, final ConsoleProgressBar progressBar) {

        if (sortedLanguages.isEmpty()) {
            return;
        }

        for (String lang : sortedLanguages) {

            // Filter the tree by status and language
            TreeNode filteredRoot = rootNode.cloneAndFilterAssets(isLive, lang, generateEmptyFolders);

            var rootPath = Paths.get(destination, FilesUtils.StatusToString(isLive), lang, rootNode.folder().host());

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
    protected String checkBaseStructure(final String destination) throws IOException {

        final Path path = Paths.get(destination);
        final Workspace workspace = workspaceManager.resolve(path);
        return workspace.files().toString();

        /*// For the pull of files, everything will be stored in a folder called "files"
        var filesFolder = Paths.get(destination, "files");

        // Create the folder if it does not exist
        if (!Files.exists(filesFolder)) {
            Files.createDirectories(filesFolder);
        }

        return filesFolder.toString();*/
    }

    /**
     * Prepares the languages used in the tree node.
     * If there are no unique live languages or working languages specified in the given `treeNodeInfo`,
     * it fallbacks to the default language available in the list of all languages.
     *
     * @param treeNodeInfo the collected information about the tree node
     * @return an instance of the {@link NodeLanguages} class containing the set of live languages and working languages
     */
    protected NodeLanguages prepareLanguages(TreeNodeInfo treeNodeInfo) {

        // We need to retrieve the languages
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final List<Language> languages = languageAPI.list().entity();

        // Collect the list of unique statuses and languages
        final var uniqueLiveLanguages = treeNodeInfo.liveLanguages();
        final var uniqueWorkingLanguages = treeNodeInfo.workingLanguages();

        // If there are no unique live languages or working languages, fallback to the default language available
        if (uniqueLiveLanguages.isEmpty() && uniqueWorkingLanguages.isEmpty()) {
            FilesUtils.FallbackDefaultLanguage(languages, uniqueLiveLanguages);
        }

        return new NodeLanguages(uniqueLiveLanguages, uniqueWorkingLanguages);
    }

    protected static class NodeLanguages {

        public final Set<String> liveLanguages;
        public final Set<String> workingLanguages;

        public NodeLanguages(Set<String> liveLanguages, Set<String> workingLanguages) {
            this.liveLanguages = liveLanguages;
            this.workingLanguages = workingLanguages;
        }
    }

}
