package com.dotcms.cli.common;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.ConfigCommand;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.arc.Arc;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.ParameterException;

/**
 * This class implements the execution strategy interface, with the added functionality of logging
 * the command being executed.
 */
public class DotExecutionStrategy implements IExecutionStrategy {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(
            DotExecutionStrategy.class.getName());
    private final IExecutionStrategy underlyingStrategy;

    /**
     * Constructs a new instance of LoggingExecutionStrategy with the provided underlying strategy.
     *
     * @param underlyingStrategy the underlying strategy to use for execution
     */
    public DotExecutionStrategy(IExecutionStrategy underlyingStrategy) {
        this.underlyingStrategy = underlyingStrategy;
    }

    /**
     * Executes the command specified in the parse result, while logging the executed command.
     *
     * @param parseResult the parse result from which to select one or more CommandSpec instances to
     *                    execute
     * @return the exit code of the executed command
     * @throws ExecutionException if an exception occurs during command execution
     * @throws ParameterException if there is an error with the command parameters
     */
    @Override
    public int execute(CommandLine.ParseResult parseResult)
            throws ExecutionException, ParameterException {

        final Optional<CommandsChain> chain = SubcommandProcessor.process(parseResult);

        if (chain.isPresent()) {
            final CommandsChain commandsChain = chain.get();

             if(commandsChain.isHelpRequestedAny()){
              // Here requesting help so no need to revise if there is any configuration
               return underlyingStrategy.execute(parseResult);
            }

            //Everything here is a sub command of EntryCommand
            final String command = commandsChain.command();
            final String format = String.format("Executing command: %s", command);
            LOGGER.info(format);

           final String parentCommand = commandsChain.firstSubcommand().map(p -> p.commandSpec().name()).orElse("UNKNOWN");

            if (!ConfigCommand.NAME.equals(parentCommand)){
                final ServiceManager manager = getServiceManager();
                try {
                    final List<ServiceBean> services = manager.services();
                    if (services.isEmpty()) {
                        throw new ExecutionException(
                                parseResult.commandSpec().commandLine(),
                                "No dotCMS configured instances were found. Please run '"+ConfigCommand.NAME+"' to setup an instance to use CLI.");
                    }
                } catch (IOException e) {
                    throw new ExecutionException(parseResult.commandSpec().commandLine(),
                            "Error reading dotCMS instances", e);
                }
            }
        }

        return underlyingStrategy.execute(parseResult);
    }

    /**
     * Returns the ServiceManager instance declared at EntryCommand.
     * @return the ServiceManager instance from the Arc container
     */
    private ServiceManager getServiceManager() {
        return Arc.container().instance(ServiceManager.class).get();
    }

}