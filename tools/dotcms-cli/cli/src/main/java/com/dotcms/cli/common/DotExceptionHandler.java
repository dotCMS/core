package com.dotcms.cli.common;

import com.dotcms.cli.command.DotCommand;
import io.quarkus.arc.Arc;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/**
 * This class is meant to handle exceptions that occur during command execution
 * It will delegate the handling of the exception to the OutputOptionMixin
 * If the OutputOptionMixin is not available, it will print the error message to the console
 */
public class DotExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) throws Exception {
        String commandName = "UNKNOWN" ;
        boolean isShowErrors = false;
        final Object object = commandLine.getCommand();
        //This takes the parseResult and unwraps the subcommands to get the command that was executed
        final Optional<CommandsChain> chain = new SubcommandProcessor().process(parseResult);
        if (chain.isPresent()) {
            final CommandsChain commandsChain = chain.get();
            commandName = commandsChain.command();
            isShowErrors = commandsChain.isShowErrorsAny();
        }

        //We usually expect a subcommand to be a DotCommand, but if it's not we'll use the default OutputOptionMixin as a fallback
        final String message = String.format("Error in command [%s] with message: %n ", commandName);
        if (object instanceof DotCommand) {
            final DotCommand command = (DotCommand) object;
            final OutputOptionMixin output = command.getOutput();
            return output.handleCommandException(ex, message, isShowErrors, true);
        } else {
            final OutputOptionMixin output = Arc.container().instance(OutputOptionMixin.class).get();
            if(null != output){
                return output.handleCommandException(ex, message);
            }
            //The intention is to use the OutputOptionMixin to handle the exception, but if it's not available we'll just print the error message
            commandLine.getErr().println(String.format("Unexpected error with message: %s ", ex.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
    }




}
