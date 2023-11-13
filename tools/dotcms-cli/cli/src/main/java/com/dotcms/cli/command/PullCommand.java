package com.dotcms.cli.command;

import com.dotcms.cli.common.AuthenticationMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PullMixin;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import picocli.CommandLine;

/**
 * Represents a pull command that is used to pull Sites, Content Types, Languages, and Files to the
 * server.
 */
@ActivateRequestContext
@CommandLine.Command(
        name = PullCommand.NAME,
        header = "@|bold,blue dotCMS global pull|@",
        description = {
                " This command pull Sites, Content Types, Languages and Files to the server.",
                "" // empty string here so we can have a new line
        }
)
public class PullCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "pull";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.Mixin
    PullMixin pullMixin;

    @CommandLine.Mixin
    AuthenticationMixin authenticationMixin;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject
    Instance<DotPull> pullCommands;


    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        // Preparing the list of arguments to be passed to the subcommands
        var expandedArgs = new ArrayList<>(spec.commandLine().getParseResult().expandedArgs());
        expandedArgs.add("--noValidateUnmatchedArguments");
        var args = expandedArgs.toArray(new String[0]);

        // Process each subcommand
        for (var subCommand : pullCommands) {

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
     * Creates a command line object for the given DotPull command and applies to the command a
     * custom configuration.
     *
     * @param command the DotPull command
     * @return the created CommandLine object
     */
    CommandLine createCommandLine(DotPull command) {

        var cmdLine = new CommandLine(command);
        CustomConfigurationUtil.getInstance().customize(cmdLine);

        // Make sure unmatched arguments pass silently
        cmdLine.setUnmatchedArgumentsAllowed(true);

        return cmdLine;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

}
