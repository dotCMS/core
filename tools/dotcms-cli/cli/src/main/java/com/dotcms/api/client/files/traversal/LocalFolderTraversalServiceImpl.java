package com.dotcms.api.client.files.traversal;

import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.OutputOptionMixin;
import io.quarkus.arc.DefaultBean;
import org.jboss.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.util.concurrent.ForkJoinPool;

import static com.dotcms.common.AssetsUtils.ParseLocalPath;

/**
 * Service for traversing a file system directory and building a hierarchical tree representation of
 * its contents. The traversal is performed using a ForkJoinPool, which allows for parallel
 * execution of the traversal tasks.
 */
@DefaultBean
@Dependent
public class LocalFolderTraversalServiceImpl implements LocalFolderTraversalService {

    @Inject
    Logger logger;

    @Inject
    protected Retriever retriever;

    /**
     * Traverses the file system directory at the specified path and builds a hierarchical tree
     * representation of its contents. The folders and contents are compared to the remote server in order to determine
     * if there are any differences between the local and remote file system.
     *
     * @param output        the output option mixin
     * @param workspacePath the workspace path
     * @param source        local the source file or directory
     * @return the root node of the hierarchical tree
     */
    @ActivateRequestContext
    @Override
    public TreeNode traverse(OutputOptionMixin output, final String workspacePath, final String source) {

        logger.debug(String.format("Traversing file system folder: %s - in workspace: %s", source, workspacePath));

        // Initial check to see if the site exist
        var siteExists = true;
        final var localPathStructure = ParseLocalPath(new File(workspacePath), new File(source));
        try {
            retriever.retrieveFolderInformation(localPathStructure.site(), null);
        } catch (NotFoundException e) {

            siteExists = false;

            // Site doesn't exist on remote server
            logger.debug(String.format("Local site [%s] doesn't exist on remote server.", localPathStructure.site()));
        }

        var forkJoinPool = ForkJoinPool.commonPool();

        var task = new LocalFolderTraversalTask(
                logger,
                retriever,
                siteExists,
                source,
                workspacePath
        );

        return forkJoinPool.invoke(task);
    }

}
