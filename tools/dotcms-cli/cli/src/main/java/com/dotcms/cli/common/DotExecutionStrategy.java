package com.dotcms.cli.common;

import com.dotcms.api.client.analytics.AnalyticsService;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.ConfigCommand;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.command.DotPush;
import com.dotcms.cli.command.InstanceCommand;
import com.dotcms.cli.command.LoginCommand;
import com.dotcms.cli.command.StatusCommand;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.arc.Arc;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
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

    private static final org.jboss.logging.Logger LOGGER = org.jboss.logging.Logger.getLogger(
            DotExecutionStrategy.class.getName()
    );

    private static final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

    private final IExecutionStrategy underlyingStrategy;

    private final SubcommandProcessor processor;

    private final DirectoryWatcherService watchService;

    private final ServiceManager serviceManager;

    /**
     * Constructs a new instance of DotExecutionStrategy with the provided dependencies.
     *
     * @param underlyingStrategy the underlying execution strategy to delegate to
     * @param processor          the processor for handling subcommands and their results
     * @param watchService       the service responsible for directory watching
     * @param serviceManager     the manager for controlling application services
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

            final String command = commandsChain.command();
            if (LOGGER.isDebugEnabled()) {
                final List<String> arguments = parseResult.expandedArgs();
                final String debugCommand = String.format(
                        "Executing command [%s][%s]", command, String.join(" ", arguments)
                );
                LOGGER.debug(debugCommand);
            } else {
                final String format = String.format("Executing command: %s", command);
                LOGGER.info(format);
            }

            //Everything here is a sub command of EntryCommand

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
     * Executes the given command chain using the specified execution strategy and parse result.
     * This method performs the operation asynchronously, handling both event recording and command
     * execution. It waits for both tasks to complete before returning the execution result.
     *
     * @param commandsChain      the chain of commands to be executed
     * @param underlyingStrategy the execution strategy used to process the commands
     * @param parseResult        the parse result containing the command context and options
     * @return the exit code resulting from the execution of the command
     * @throws ExecutionException if an error occurs during execution or processing
     */
    int internalExecute(CommandsChain commandsChain,
            IExecutionStrategy underlyingStrategy,
            CommandLine.ParseResult parseResult) throws ExecutionException {

        CompletableFuture<Void> eventFuture = null;
        CompletableFuture<Integer> executeFuture = null;

        try (var handle = Arc.container().instance(ManagedExecutor.class)) {

            final var executor = handle.get();

            // Capture the current call depth for this thread
            final int currentCallDepth = callDepth.get();

            // Create a CompletableFuture for the event recording
            eventFuture = executor.runAsync(() -> {
                try {
                    recordEvent(commandsChain, parseResult);
                } catch (Exception e) {
                    LOGGER.error("Error recording event asynchronously", e);
                }
            });

            // Create a CompletableFuture for the command execution
            executeFuture = executor.supplyAsync(() -> {
                try {
                    // Set the callDepth in this new thread
                    callDepth.set(currentCallDepth);
                    return processCommandExecution(commandsChain, underlyingStrategy, parseResult);
                } finally {
                    // Clean up the ThreadLocal in this thread
                    callDepth.remove();
                }
            });

            // Wait for both futures to complete, handling interruption properly
            CompletableFuture.allOf(eventFuture, executeFuture).get();
            return executeFuture.get();
        } catch (InterruptedException e) {
            LOGGER.debug("Thread was interrupted during execution");
            cancelFuturesSafely(eventFuture, executeFuture);

            Thread.currentThread().interrupt();
            throw new ExecutionException(parseResult.commandSpec().commandLine(),
                    "Parallel execution interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(parseResult.commandSpec().commandLine(),
                    "Error in parallel execution", e.getCause());
        } catch (java.util.concurrent.CancellationException |
                 java.util.concurrent.CompletionException e) {
            cancelFuturesSafely(eventFuture, executeFuture);
            if (e.getCause() instanceof InterruptedException || Thread.currentThread()
                    .isInterrupted()) {
                LOGGER.debug("Thread was interrupted during execution");
                Thread.currentThread().interrupt();
            }

            throw new ExecutionException(parseResult.commandSpec().commandLine(),
                    "Error in parallel execution", e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * Cancels the provided CompletableFuture instances safely. If a future is not null and not
     * completed, it attempts to cancel it. Logs any exceptions that occur during cancellation.
     *
     * @param eventFuture   the CompletableFuture representing an event to be cancelled
     * @param executeFuture the CompletableFuture representing an execution task to be cancelled
     */
    private void cancelFuturesSafely(CompletableFuture<?> eventFuture,
            CompletableFuture<?> executeFuture) {
        try {
            if (eventFuture != null && !eventFuture.isDone()) {
                eventFuture.cancel(true);
            }

            if (executeFuture != null && !executeFuture.isDone()) {
                executeFuture.cancel(true);
            }
        } catch (Exception ex) {
            LOGGER.debug("Error cancelling futures", ex);
        }
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
    int processCommandExecution(final CommandsChain commandsChain,
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
     * Records an event by sending the command and arguments to the analytics service, if
     * available.
     *
     * @param commandsChain the chain of commands that holds the context of the command to be
     *                      recorded
     * @param parseResult   the result of the command parsing which contains detailed information,
     *                      such as expanded arguments
     * @throws IOException if an I/O error occurs during the recording process
     */
    void recordEvent(CommandsChain commandsChain, final ParseResult parseResult)
            throws IOException {

        final var analyticsEnabled = isAnalyticsEnabled();
        if (analyticsEnabled) {

            try (var handle = Arc.container().instance(AnalyticsService.class)) {

                AnalyticsService analyticsService = handle.get();
                if (analyticsService != null) {

                    final var trackCommand = trackCommand(commandsChain);
                    if (trackCommand) {

                        final String command = commandsChain.command();
                        final List<String> arguments = parseResult.expandedArgs();

                        analyticsService.recordCommand(command, arguments);
                    }
                } else {
                    LOGGER.warn("No analytics service available. Event will not be recorded.");
                }
            }
        }
    }

    /**
     * Determines whether the given command chain should be tracked for analytics purposes.
     * Excludes specific commands like ConfigCommand, LoginCommand, StatusCommand, and InstanceCommand
     * from being tracked.
     *
     * @param commandsChain the chain of commands to check for tracking eligibility
     * @return true if the command in the chain should be tracked, false otherwise
     */
    private boolean trackCommand(CommandsChain commandsChain) {

        final String parentCommand = commandsChain.firstSubcommand()
                .map(p -> p.commandSpec().name()).orElse("UNKNOWN");

        return !ConfigCommand.NAME.equals(parentCommand)
                && !LoginCommand.NAME.equals(parentCommand)
                && !StatusCommand.NAME.equals(parentCommand)
                && !InstanceCommand.NAME.equals(parentCommand);
    }

    /**
     * Determines whether analytics tracking is enabled based on the configuration. The
     * configuration is retrieved through the ConfigProvider, and the value is fetched using the key
     * "analytic.enabled". If the key is not defined in the configuration, it defaults to false.
     *
     * @return true if analytics tracking is enabled; false otherwise
     */
    private Boolean isAnalyticsEnabled() {

        // Validate if the analytics tracking is enabled
        Config config = ConfigProvider.getConfig();
        final var analyticsEnabledOpt = config.getOptionalValue(
                "analytic.enabled", Boolean.class
        );

        return analyticsEnabledOpt.orElse(false);
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

        final Path watchedPath = watchedPath(push);
        push.getOutput().println("Running in Watch Mode on " + watchedPath);
        try {
            final PushMixin pushMixin = push.getPushMixin();
            return watch(underlyingStrategy, parseResult, watchedPath, pushMixin.interval);
        } catch (IOException e) {
            throw new ExecutionException(parseResult.commandSpec().commandLine(), "Failure starting watch service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(parseResult.commandSpec().commandLine(), "Watch service interrupted", e);
        }
    }

    /**
     * Returns the path to watch for the given push command.
     * @param push the push command
     * @return the path to watch
     */
    private Path watchedPath(final DotPush push) {
        final PushMixin pushMixin = push.getPushMixin();
        if (pushMixin.isUserProvidedPath()) {
            return pushMixin.path();
        }
        try {
            return push.workingRootDir();
        } catch (IllegalArgumentException e) {
            // If we can't find a workspace, we default to the user provided path
            //I'm catching this exception here, so it can be properly handled in the calling command when it figures out that the given path isn't a workspace
            return pushMixin.path();
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
                decCallDepth();
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