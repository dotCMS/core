package com.dotcms.api.client.files.traversal;

import com.dotcms.api.client.files.traversal.data.Downloader;
import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.client.files.traversal.task.LocalFolderTraversalTask;
import com.dotcms.api.client.files.traversal.task.LocalFoldersTreeBuilderTask;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.common.AssetsUtils;
import io.quarkus.arc.DefaultBean;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ForkJoinPool;

import static com.dotcms.common.AssetsUtils.ParseLocalPath;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
@DefaultBean
@Dependent
public class LocalTraversalServiceImpl implements LocalTraversalService {

    @Inject
    Logger logger;

    @Inject
    protected Retriever retriever;

    @Inject
    protected Downloader downloader;

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents. The folders and contents are compared to the remote server in order to determine
     * if there are any differences between the local and remote file system.
     *
     * @param output             the output option mixin
     * @param workspace          the project workspace
     * @param source             local the source file or directory
     * @param removeAssets       true to allow remove assets, false otherwise
     * @param removeFolders      true to allow remove folders, false otherwise
     * @param ignoreEmptyFolders true to ignore empty folders, false otherwise
     * @return a pair representing a folder's local path structure and its corresponding root node of the hierarchical tree
     */
    @ActivateRequestContext
    @Override
    public Pair<AssetsUtils.LocalPathStructure, TreeNode> traverseLocalFolder(
            OutputOptionMixin output, final File workspace, final String source,
            final boolean removeAssets, final boolean removeFolders,
            final boolean ignoreEmptyFolders) {

        logger.debug(String.format("Traversing file system folder: %s - in workspace: %s",
                source, workspace.getAbsolutePath()));

        final var localPathStructure = ParseLocalPath(workspace, new File(source));

        // Initial check to see if the site exist
        var siteExists = true;
        try {
            retriever.retrieveFolderInformation(localPathStructure.site(), null);
        } catch (NotFoundException e) {

            siteExists = false;

            // Site doesn't exist on remote server
            logger.debug(String.format("Local site [%s] doesn't exist on remote server.", localPathStructure.site()));
        }

        // Checking if the language exist
        try {
            localPathStructure.setLanguageExists(true);
            retriever.retrieveLanguage(localPathStructure.language());
        } catch (NotFoundException e) {

            localPathStructure.setLanguageExists(false);

            // Site doesn't exist on remote server
            logger.debug(String.format("Language [%s] doesn't exist on remote server.", localPathStructure.language()));
        }

        var forkJoinPool = ForkJoinPool.commonPool();

        var task = new LocalFolderTraversalTask(
                logger,
                retriever,
                siteExists,
                source,
                workspace,
                removeAssets,
                removeFolders,
                ignoreEmptyFolders
        );

        return Pair.of(localPathStructure, forkJoinPool.invoke(task));
    }

    /**
     * Builds the file system tree from the specified root node. The tree is built using a ForkJoinPool, which allows
     * for parallel execution of the traversal tasks.
     *
     * @param rootNode             the root node of the file tree
     * @param destination          the destination path to save the pulled files
     * @param isLive               true if processing live tree, false for working tree
     * @param language             the language to process
     * @param overwrite            true to overwrite existing files, false otherwise
     * @param generateEmptyFolders true to generate empty folders, false otherwise
     * @param progressBar          the progress bar for tracking the pull progress
     */
    public void buildFileSystemTree(final TreeNode rootNode, final String destination, final boolean isLive,
                                    final String language, final boolean overwrite, final boolean generateEmptyFolders,
                                    ConsoleProgressBar progressBar) {

        // Filter the tree by status and language
        TreeNode filteredRoot = rootNode.cloneAndFilterAssets(isLive, language, generateEmptyFolders, false);

        var rootPath = Paths.get(destination, AssetsUtils.StatusToString(isLive), language, rootNode.folder().host());

        // ---
        var forkJoinPool = ForkJoinPool.commonPool();
        var task = new LocalFoldersTreeBuilderTask(
                logger,
                downloader,
                filteredRoot,
                rootPath.toString(),
                overwrite,
                generateEmptyFolders,
                language,
                progressBar);
        forkJoinPool.invoke(task);
    }

}
