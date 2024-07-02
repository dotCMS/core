package com.dotcms.cli.command.files;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.files.traversal.RemoteTraversalService;
import com.dotcms.api.client.files.traversal.task.TraverseTaskResult;
import com.dotcms.api.traversal.TreeNode;
import com.dotcms.cli.common.ConsoleLoadingAnimation;
import com.dotcms.model.language.Language;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import picocli.CommandLine;

/**
 * This abstract class is used for implementing files listing commands. It provides common
 * functionality for listing the contents of a remote directory.
 */
public abstract class AbstractFilesListingCommand extends AbstractFilesCommand {

    @CommandLine.Mixin
    FilesListingMixin filesMixin;

    @Inject
    RemoteTraversalService remoteTraversalService;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject
    ManagedExecutor executor;

    /**
     * Executes the listing of a remote folder at the specified depth.
     *
     * @param depth the depth of the folder traversal
     * @return an exit code indicating the success of the operation
     * @throws ExecutionException   if an exception occurs during execution
     * @throws InterruptedException if the execution is interrupted
     */
    protected Integer listing(final Integer depth) throws ExecutionException, InterruptedException {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        var includeFolderPatterns = parsePatternOption(
                filesMixin.globMixin.includeFolderPatternsOption
        );
        var includeAssetPatterns = parsePatternOption(
                filesMixin.globMixin.includeAssetPatternsOption
        );
        var excludeFolderPatterns = parsePatternOption(
                filesMixin.globMixin.excludeFolderPatternsOption
        );
        var excludeAssetPatterns = parsePatternOption(
                filesMixin.globMixin.excludeAssetPatternsOption
        );

        CompletableFuture<TraverseTaskResult> folderTraversalFuture = executor.supplyAsync(
                () ->
                        // Service to handle the traversal of the folder
                        remoteTraversalService.traverseRemoteFolder(
                                filesMixin.folderPath,
                                depth,
                                // Fail fast is set to true to stop the traversal as soon as an exception is encountered
                                // therefore if we encounter permissions error the command will not complete
                                false,
                                includeFolderPatterns,
                                includeAssetPatterns,
                                excludeFolderPatterns,
                                excludeAssetPatterns
                        )
        );

        // ConsoleLoadingAnimation instance to handle the waiting "animation"
        ConsoleLoadingAnimation consoleLoadingAnimation = new ConsoleLoadingAnimation(
                output,
                folderTraversalFuture
        );

        CompletableFuture<Void> animationFuture = executor.runAsync(
                consoleLoadingAnimation
        );

        // Waits for the completion of both the folder traversal and console loading animation tasks.
        // This line blocks the current thread until both CompletableFuture instances
        // (folderTraversalFuture and animationFuture) have completed.
        CompletableFuture.allOf(folderTraversalFuture, animationFuture).join();
        final var result = folderTraversalFuture.get();

        if (result == null) {
            output.error(String.format(
                    "Error occurred while pulling folder info: [%s].", filesMixin.folderPath));
            return CommandLine.ExitCode.SOFTWARE;
        }

        // We need to retrieve the languages
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final List<Language> languages = languageAPI.list().entity();

        // Display the result
        StringBuilder sb = new StringBuilder();
        final Optional<TreeNode> optional = result.treeNode();
        if(optional.isPresent()){
            //We got back a tree node lets print it out
            TreePrinter.getInstance()
                    .filteredFormat(sb, optional.get(), !filesMixin.excludeEmptyFolders, languages);
            output.info(sb.toString());
        } else {
            reportErrorWhenNothingWasReturned(result);
        }
        return CommandLine.ExitCode.OK;
    }

    /**
     * Reports an error when nothing was returned from the traversal.
     * @param result the result of the traversal
     */
    private void reportErrorWhenNothingWasReturned(final TraverseTaskResult result) {
        //TreeNode didn't make it back, lets print out the exceptions
        //though the exceptions should have been handled by the ExceptionHandler if thrown
        //We still need to cover the case when no fail fast is set therefore we get a list of exceptions
        if(result.exceptions().isEmpty()){
            output.info("Error occurred while pulling folder info: [" + filesMixin.folderPath + "].");
        } else {
            final Iterator<Exception> iterator = result.exceptions().iterator();
            while (iterator.hasNext()) {
                final Exception e = iterator.next();
                if (iterator.hasNext()){
                    output.handleCommandException(e);
                } else {
                    output.handleCommandException(e,"Error occurred while pulling folder info: [" + filesMixin.folderPath + "].");
                }
            }
        }
    }

}
