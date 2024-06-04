package com.dotcms.cli.command;

import com.dotcms.api.client.util.DirectoryWatcherService;
import com.dotcms.cli.common.AuthenticationMixin;
import com.dotcms.cli.common.CommandInterceptor;
import com.dotcms.cli.common.FullPushOptionsMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.WorkspaceManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import picocli.CommandLine;

/**
 * Global Push Command
 * Represents a push command that is used to push Sites, Content Types, Languages, and Files to the
 * server.
 */
@ActivateRequestContext
@CommandLine.Command(
        name = PushCommand.NAME,
        header = "@|bold,blue dotCMS global push|@",
        description = {
                " This command push Sites, Content Types, Languages and Files to the server.",
                "" // empty string here so we can have a new line
        }
)

public class PushCommand implements Callable<Integer>, DotPush {

    static final String NAME = "push";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.Mixin
    FullPushOptionsMixin pushMixin;

    @CommandLine.Mixin
    AuthenticationMixin authenticationMixin;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject
    protected WorkspaceManager workspaceManager;

    @Inject
    Instance<DotPush> pushCommands;

    @Inject
    DirectoryWatcherService directoryWatcherService;

    @Override
    @CommandInterceptor
    public Integer call() throws Exception {
        // Find the instances of all push subcommands

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        // Validate we have a workspace at the specified path
        checkValidWorkspace(pushMixin.path());

        /*
        if(pushMixin.isWatchOn()){
            directoryWatcherService.watch(pushMixin.path(), pushMixin.interval, true, event -> {
                execCommands();
            });
            return CommandLine.ExitCode.OK;
        }
         */

        final Integer exitCode = execSubCommands();
        if (exitCode != null) {
            return exitCode;
        }

        return CommandLine.ExitCode.OK;
    }

    private Integer execSubCommands() {
        // Preparing the list of arguments to be passed to the subcommands
        var expandedArgs = new ArrayList<>(spec.commandLine().getParseResult().expandedArgs());
        expandedArgs.add("--noValidateUnmatchedArguments");
        var args = expandedArgs.toArray(new String[0]);

        // Sort the subcommands by order
        final var pushCommandsSorted = pushCommands.stream()
                .filter( dotPush ->  !dotPush.isGlobalPush() )
                .sorted(Comparator.comparingInt(DotPush::getOrder))
                .collect(Collectors.toList());

        // Usa  ExecutorService for parallel execution of the subcommands
        final ExecutorService executorService = Executors.newFixedThreadPool(pushCommandsSorted.size());
        final List<Future<Integer>> futures = new ArrayList<>();

        for (var subCommand : pushCommandsSorted) {
            Callable<Integer> task = () -> {
                var cmdLine = createCommandLine(subCommand);
                return cmdLine.execute(args);
            };
            futures.add(executorService.submit(task));
        }

        // Wait for all subcommands to finish and check for errors
        for (Future<Integer> future : futures) {
            try {
                int exitCode = future.get();
                if (exitCode != CommandLine.ExitCode.OK) {
                    executorService.shutdownNow();
                    return exitCode;
                }
            } catch (InterruptedException | ExecutionException e) {
                executorService.shutdownNow();
                throw new RuntimeException("Error executing subcommand", e);
            }
        }

        executorService.shutdown();
        return null;
    }

    /**
     * Creates a command line object for the given DotPush command and applies to the command a
     * custom configuration.
     *
     * @param command the DotPush command
     * @return the created CommandLine object
     */
    CommandLine createCommandLine(DotPush command) {

        var cmdLine = new CommandLine(command);
        CustomConfigurationUtil.getInstance().customize(cmdLine);

        // Make sure unmatched arguments pass silently
        cmdLine.setUnmatchedArgumentsAllowed(true);

        return cmdLine;
    }

    /**
     * Checks if the provided file is a valid workspace.
     *
     * @param path represents the workspace directory.
     * @throws IllegalArgumentException if no valid workspace is found at the specified path.
     */
    void checkValidWorkspace(final Path path) {

        final var workspace = workspaceManager.findWorkspace(path);

        if (workspace.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No valid workspace found at path: [%s]", path.toAbsolutePath()));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    @Override
    public PushMixin getPushMixin() {
        return this.pushMixin;
    }

    @Override
    public boolean isGlobalPush() {
        return true;
    }
}
