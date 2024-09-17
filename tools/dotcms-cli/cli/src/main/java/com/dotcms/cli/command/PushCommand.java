package com.dotcms.cli.command;

import com.dotcms.cli.common.AuthenticationMixin;
import com.dotcms.cli.common.FullPushOptionsMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.WorkspaceManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
    CustomConfigurationUtil customConfigurationUtil;

    @Override
    public Integer call() throws Exception {
        // Find the instances of all push subcommands

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        // Validate we have a workspace at the specified path
        checkValidWorkspace(pushMixin.path());

        // Preparing the list of arguments to be passed to the subcommands
        var expandedArgs = new ArrayList<>(spec.commandLine().getParseResult().expandedArgs());
        expandedArgs.add("--noValidateUnmatchedArguments");
        var args = expandedArgs.toArray(new String[0]);

        // Sort the subcommands by order
        final var pushCommandsSorted = pushCommands.stream()
                .filter( dotPush ->  !dotPush.isGlobalPush() )
                .sorted(Comparator.comparingInt(DotPush::getOrder))
                .collect(Collectors.toList());

        // Process each subcommand
        for (var subCommand : pushCommandsSorted) {
            var cmdLine = createCommandLine(subCommand);

            // Use execute to parse the parameters with the subcommand
            int exitCode = cmdLine.execute(args);
            if (exitCode != CommandLine.ExitCode.OK) {
                return exitCode;
            }
        }

        return CommandLine.ExitCode.OK;
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
        customConfigurationUtil.customize(cmdLine);
        cmdLine.setOut(getOutput().out());

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

    @Override
    public WorkspaceManager workspaceManager() {
        return workspaceManager;
    }
}
