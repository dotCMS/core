package com.dotcms.cli.common;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.InitCommand;
import com.dotcms.cli.exception.ExceptionHandler;
import com.dotcms.cli.exception.UninitializedStateException;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.arc.Arc;
import java.io.IOException;
import java.util.List;
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

        System.out.println("Executing command: " + parseResult.commandSpec().name());
        //System.out.println("Executing sub-command: " + parseResult.subcommand().commandSpec().name());
        if (!parseResult.originalArgs().isEmpty()) {
            final String commandLineString = String.join(" ", parseResult.originalArgs());
            final String format = String.format("Executing command: %s", commandLineString);
            LOGGER.info(format);

            if(!InitCommand.NAME.equals(parseResult.commandSpec().name()) && parseResult.subcommand() != null){
                final ServiceManager manager = getServiceManager();
                try{
                final List<ServiceBean> services = manager.services();
                if(services.isEmpty()){
                    throw new UninitializedStateException(parseResult.commandSpec().commandLine(),"No dotCMS instances found. Please run 'dotCMS init' to initialize the CLI.");
                }}catch (IOException e){
                    throw new ExecutionException(parseResult.commandSpec().commandLine(), "Error reading dotCMS instances", e);
                }
            }
        }

        return underlyingStrategy.execute(parseResult);
    }


    ServiceManager serviceManager;

    ServiceManager getServiceManager(){
        if(null == serviceManager){
            serviceManager = Arc.container().instance(ServiceManager.class).get();
        }
        return serviceManager;
    }

}