package com.dotcms.cli.common;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.ConfigCommand;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.command.DotPush;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

/**
 * This class implements the execution strategy interface, with the added functionality of logging
 * the command being executed.
 */
public class DotExecutionStrategy implements IExecutionStrategy {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(
            DotExecutionStrategy.class.getName());

    private static final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

    private final IExecutionStrategy underlyingStrategy;

    private final SubcommandProcessor processor;

    private final DirectoryWatcherService watchService;

    private final ServiceManager serviceManager;


    /**
     * Constructs a new instance of LoggingExecutionStrategy with the provided underlying strategy.
     *
     * @param underlyingStrategy the underlying strategy to use for execution
     */
    public DotExecutionStrategy(final IExecutionStrategy underlyingStrategy,
            final SubcommandProcessor processor, final DirectoryWatcherService watchService,
            final ServiceManager serviceManager) {
        this.underlyingStrategy = underlyingStrategy;
        this.processor = processor;
        this.watchService = watchService;
        this.serviceManager = serviceManager;
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

        final Optional<CommandsChain> chain = processor.process(parseResult);

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

            // If the dotCMS URL and token are set, we can proceed with the command execution, we
            // can bypass the configuration check as we have everything we need for a remote call
            if (isRemoteURLSet(commandsChain, parseResult.commandSpec().commandLine())) {
                return internalExecute(commandsChain, underlyingStrategy, parseResult);
            }

            //If no remote URL is set, we need to check if there is a configuration
            verifyConfigExists(parseResult, commandsChain);

            //If we have a configuration, we can proceed with the command execution
            return internalExecute(commandsChain, underlyingStrategy, parseResult);

        }

        return underlyingStrategy.execute(parseResult);
    }

    /**
     * Checks if the remote URL is set in the CommandsChain object. If the remote URL and token are
     * both set, it returns true. If the remote URL is set but the token is not set, it throws a
     * ParameterException. If the remote URL is not set, it returns false.
     *
     * @param commandsChain the CommandsChain object to check
     * @param commandLine   the CommandLine object for error handling
     * @return true if the remote URL and token are both set, false otherwise
     * @throws ParameterException if the remote URL is set but the token is not set
     */
    private boolean isRemoteURLSet(final CommandsChain commandsChain,
            final CommandLine commandLine) {

        if (commandsChain.isRemoteURLSet() && commandsChain.isTokenSet()) {
            return true;
        } else if (commandsChain.isRemoteURLSet() && !commandsChain.isTokenSet()) {
            throw new ParameterException(commandLine,
                    "The token is required when the dotCMS URL is set.");
        }

        return false;
    }

    /**
     * Verifies that a configuration exists in the dotCMS CLI. If no configuration exists, it throws
     * an ExecutionException.
     *
     * @param parseResult    the parse result from which to select one or more CommandSpec instances
     *                       to execute
     * @param commandsChain  the CommandsChain object to check
     * @throws ExecutionException if no configuration exists
     */
    void verifyConfigExists(ParseResult parseResult, CommandsChain commandsChain) {
        final String parentCommand = commandsChain.firstSubcommand()
                .map(p -> p.commandSpec().name()).orElse("UNKNOWN");

        if (!ConfigCommand.NAME.equals(parentCommand)){
            try {
                final List<ServiceBean> services = serviceManager.services();
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

    /**
     * Executes the command specified in the parse result, while logging the executed command. If the
     * @param commandsChain the CommandsChain object with the parsed options
     * @param underlyingStrategy the underlying strategy to use for execution
     * @param parseResult the parse result from which to select one or more CommandSpec instances to execute
     * @return the exit code of the executed command
     * @throws ExecutionException if an exception occurs during command execution
     * @throws ParameterException if there is an error with the command parameters
     */
    int internalExecute(final CommandsChain commandsChain,
            final IExecutionStrategy underlyingStrategy,
            final ParseResult parseResult)
            throws ExecutionException, ParameterException {
        try {
            if (isWatchModeAlreadyRunning()) {
                   incCallDepth();
                   return underlyingStrategy.execute(parseResult);
            }

            if (commandsChain.isWatchMode()) {
                //We need to figure out how to handle the watch
                final Optional<DotCommand> optional = command(commandsChain);
                if (optional.isPresent()) {
                    final DotCommand command = optional.get();
                    if (command instanceof DotPush) {
                        final DotPush push = (DotPush) command;
                        return handleWatchPush(underlyingStrategy, parseResult, push);
                    }
                }
            }
            incCallDepth();
            return underlyingStrategy.execute(parseResult);
        } finally {
            // Decrement the call depth and remove ThreadLocal if it's zero
            decCallDepth();
        }
    }

    /**
     * Checks if the watch mode is already running.
     *
     * @return true if the watch mode is already running, false otherwise
     */
    private boolean isWatchModeAlreadyRunning() {
        return callDepth.get() > 0;
    }

    /**
     * Decrements the call depth by one. If the call depth is zero, it removes the ThreadLocal.
     */
    private static void decCallDepth() {
        int depth = callDepth.get() - 1;
        callDepth.set(depth);
        if (depth == 0) {
            callDepth.remove();
        }
    }

    /**
     * Increments the call depth by one.
     */
    private static void incCallDepth() {
        callDepth.set(callDepth.get() + 1);
    }

    int handleWatchPush(final IExecutionStrategy underlyingStrategy, final ParseResult parseResult, final DotPush push) {
        final PushMixin pushMixin = push.getPushMixin();
        push.getOutput().println("Running in Watch Mode on " + push.workingRootDir());
        try {
            return watch(underlyingStrategy, parseResult, push.workingRootDir(), pushMixin.interval);
        } catch (IOException e) {
            throw new ExecutionException(parseResult.commandSpec().commandLine(), "Failure starting watch service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(parseResult.commandSpec().commandLine(), "Watch service interrupted", e);
        }
    }

    private int watch(final IExecutionStrategy underlyingStrategy, final ParseResult parseResult,
            final Path workingDir, final int interval) throws IOException, InterruptedException {

        int result = ExitCode.OK;
        final BlockingQueue<WatchEvent<?>> eventQueue = watchService.watch(workingDir, interval);
        while (watchService.isRunning()) {
            final WatchEvent<?> event = eventQueue.take();
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }
            try {
                //Disengage the watch service to avoid recursion issues
                //The command itself might trigger a file change
                watchService.suspend();
                incCallDepth();
                result = underlyingStrategy.execute(parseResult);
            } finally {
                //Re-engage the watch mode
                watchService.resume();
            }
        }
        return result;
    }

    /**
     * Returns the PushMixin instance from the last subcommand in the CommandsChain object.
     * @param commandsChain the CommandsChain object to check
     * @return an Optional of PushMixin
     */
    Optional<DotCommand> command(final CommandsChain commandsChain) {
        final Optional<ParseResult> lastSubcommand = commandsChain.lastSubcommand();
        if (lastSubcommand.isPresent()) {
            final ParseResult parseResult = lastSubcommand.get();
            final Object command = parseResult.commandSpec().userObject();
            if (command instanceof DotCommand) {
                return Optional.of((DotCommand) command);
            }
        }
        return Optional.empty();
    }
}