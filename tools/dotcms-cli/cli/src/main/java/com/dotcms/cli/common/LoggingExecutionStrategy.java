package com.dotcms.cli.common;

import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.ParameterException;

/**
 * This class implements the execution strategy interface, with the added functionality of logging
 * the command being executed.
 */
public class LoggingExecutionStrategy implements IExecutionStrategy {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(
            LoggingExecutionStrategy.class.getName());
    private final IExecutionStrategy underlyingStrategy;

    /**
     * Constructs a new instance of LoggingExecutionStrategy with the provided underlying strategy.
     *
     * @param underlyingStrategy the underlying strategy to use for execution
     */
    public LoggingExecutionStrategy(IExecutionStrategy underlyingStrategy) {
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

        if (!parseResult.originalArgs().isEmpty()) {
            String commandLineString = String.join(" ", parseResult.originalArgs());
            LOGGER.info("Executing command: " + commandLineString);
        }

        return underlyingStrategy.execute(parseResult);
    }
}