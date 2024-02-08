package com.dotcms.cli.common;

import com.dotcms.cli.command.DotCommand;
import io.quarkus.arc.Arc;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public class DotExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) throws Exception {
        String commandName = "UNKNOWN" ;
        final Object object = commandLine.getCommand();
        final Optional<CommandsChain> chain = SubcommandProcessor.process(parseResult);
        if (chain.isPresent()) {
            final CommandsChain commandsChain = chain.get();
            commandName = commandsChain.command();
        }

        final String message = String.format("Error in command [%s] with message: %n ", commandName);
        if (object instanceof DotCommand) {
            final DotCommand command = (DotCommand) object;
            final OutputOptionMixin output = command.getOutput();
            return output.handleCommandException(ex, message);
        } else {
            OutputOptionMixin output = Arc.container().instance(OutputOptionMixin.class).get();
            if(null != output){
                return output.handleCommandException(ex, message);
            }

            commandLine.getErr().println(String.format("Unexpected error with message: %s ", ex.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
    }




}
