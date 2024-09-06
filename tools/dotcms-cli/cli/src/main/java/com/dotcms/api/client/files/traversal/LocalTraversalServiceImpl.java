package com.dotcms.api.client.files.traversal;

import static com.dotcms.common.AssetsUtils.parseLocalPath;

import com.dotcms.api.client.FileHashCalculatorService;
import com.dotcms.api.client.files.traversal.data.Downloader;
import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.files.traversal.task.LocalFolderTraversalTask;
import com.dotcms.api.client.files.traversal.task.LocalFolderTraversalTaskParams;
import com.dotcms.api.client.files.traversal.task.PullTreeNodeTask;
import com.dotcms.api.client.files.traversal.task.PullTreeNodeTaskParams;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.common.AssetsUtils;
import com.dotcms.common.LocalPathStructure;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletionException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

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

    @Inject
    ManagedExecutor executor;

    @Inject
    FileHashCalculatorService fileHashCalculatorService;

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents. The folders and contents are compared to the remote server in
     * order to determine if there are any differences between the local and remote file system.
     *
     * @param params Traverse params
     * @return a TraverseResult corresponding root node of the hierarchical tree
     */
    @ActivateRequestContext
    @Override
    public TraverseResult traverseLocalFolder(final LocalTraverseParams params) {
        final String source = params.sourcePath();
        final File workspace = params.workspace();

        logger.debug(String.format("Traversing file system folder: %s - in workspace: %s",
                source, workspace.getAbsolutePath()));

        var localPath = parseLocalPath(workspace, new File(source));

        // Initial check to see if the site exist
        var siteExists = true;
        try {
            retriever.retrieveFolderInformation(localPath.site(), null);
        } catch (NotFoundException e) {

            siteExists = false;

            // Site doesn't exist on remote server
            logger.debug(String.format("Local site [%s] doesn't exist on remote server.", localPath.site()));
        }

        // Checking if the language exists
        try {
            final var languageResponse = retriever.retrieveLanguage(localPath.language());
            localPath = LocalPathStructure.builder().from(localPath).
                    languageExists(true).
                    isDefaultLanguage(languageResponse.defaultLanguage().orElse(false)).
                    build();
        } catch (NotFoundException e) {
            localPath = LocalPathStructure.builder().from(localPath).languageExists(false).build();

            // Language doesn't exist on remote server
            logger.debug(String.format("Language [%s] doesn't exist on remote server.", localPath.language()));
        }

        var task = new LocalFolderTraversalTask(
                logger,
                executor,
                retriever,
                fileHashCalculatorService
        );

        task.setTaskParams(LocalFolderTraversalTaskParams.builder()
                .siteExists(siteExists)
                .sourcePath(params.sourcePath())
                .workspace(params.workspace())
                .removeAssets(params.removeAssets())
                .removeFolders(params.removeFolders())
                .ignoreEmptyFolders(params.ignoreEmptyFolders())
                .failFast(params.failFast())
                .build()
        );

        try {
            var result = task.compute().join();
            return TraverseResult.builder()
                    .exceptions(result.exceptions())
                    .localPaths(localPath)
                    .treeNode(result.treeNode()).build();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TraversalTaskException) {
                throw (TraversalTaskException) cause;
            } else {
                throw new TraversalTaskException(cause.getMessage(), cause);
            }
        }
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
     * @param failFast             true to fail fast, false to continue on error
     * @param progressBar          the progress bar for tracking the pull progress
     * @return a list of exceptions that occurred during the pull
     */
    public List<Exception> pullTreeNode(final TreeNode rootNode, final String destination, final boolean isLive,
                                    final String language, final boolean overwrite, final boolean generateEmptyFolders,
                                               final boolean failFast, ConsoleProgressBar progressBar) {

        // Filter the tree by status and language
        TreeNode filteredRoot = rootNode.cloneAndFilterAssets(isLive, language, generateEmptyFolders, false);

        var rootPath = Paths.get(destination, AssetsUtils.statusToString(isLive), language,
                rootNode.folder().host());

        // ---
        var task = new PullTreeNodeTask(
                logger,
                executor,
                downloader,
                fileHashCalculatorService
        );

        task.setTaskParams(PullTreeNodeTaskParams.builder()
                .rootNode(filteredRoot)
                .destination(rootPath.toString())
                .overwrite(overwrite)
                .generateEmptyFolders(generateEmptyFolders)
                .failFast(failFast)
                .language(language)
                .progressBar(progressBar)
                .build()
        );

        try {
            return task.compute().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TraversalTaskException) {
                throw (TraversalTaskException) cause;
            } else {
                throw new TraversalTaskException(cause.getMessage(), cause);
            }
        }
    }

}
