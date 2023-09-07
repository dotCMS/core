package com.dotcms.cli.command;

import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = PushCommand.NAME,
        header = "@|bold,blue dotCMS global push|@",
        description = {
                " This command push Sites, Content Types, Languages and Files to the server.",
                "" // empty string here so we can have a new line
        }
)
public class PushCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "push";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.Mixin
    PushMixin pushMixin;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    // Find the instances of all push subcommands
    Instance<DotPush> pushCommands = CDI.current().select(DotPush.class);

    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        // Preparing the list of arguments to be passed to the subcommands
        var expandedArgs = new ArrayList<>(spec.commandLine().getParseResult().expandedArgs());
        expandedArgs.add("--noValidateUnmatchedArguments");
        var args = expandedArgs.toArray(new String[0]);

        // Process each subcommand
        for (var subCommand : pushCommands) {

            var cmdLine = new CommandLine(subCommand);
            CustomConfigurationUtil.getInstance().customize(cmdLine);

            // Make sure unmatched arguments pass silently
            cmdLine.setUnmatchedArgumentsAllowed(true);

            // Use execute to parse the parameters with the subcommand
            int exitCode = cmdLine.execute(args);
            if (exitCode != CommandLine.ExitCode.OK) {
                return exitCode;
            }
        }

        return CommandLine.ExitCode.OK;
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
