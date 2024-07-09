package com.dotcms.cli.common;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.WARN_ICON;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import com.dotcms.api.client.files.traversal.exception.TraversalTaskException;
import com.dotcms.api.client.pull.exception.PullException;
import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.cli.exception.ExceptionHandler;
import com.dotcms.cli.exception.ForceSilentExitException;
import io.quarkus.arc.Arc;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.specimpl.BuiltResponse;
import picocli.CommandLine;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

public class OutputOptionMixin implements MessageWriter {

    // Configuration values for exception message abbreviation.
    Boolean exceptionAbbreviateEnabled;
    Integer exceptionMaxWidth;

    static final boolean picocliDebugEnabled = "DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"));

    @CommandLine.Option(names = { "-e", "--errors" }, description = "Display error messages.", hidden = true)
    boolean showErrors;

    @CommandLine.Option(names = { "-v", "--verbose" }, description = "Verbose mode.", hidden = true)
    boolean verbose;

    @CommandLine.Option(names = {"--cli-test" }, description = "Manually set output streams for unit test purposes.", hidden = true)
    boolean cliTestMode;

    Path testProjectRoot;

    @CommandLine.Option(names = { "--cli-test-dir" }, hidden = true)
    void setTestProjectRoot(String path) {
        // Allow the starting/project directory to be specified. Used during test.
        testProjectRoot = Paths.get(path).toAbsolutePath();
    }

    @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
    CommandSpec mixee;

    ColorScheme scheme;
    PrintWriter out;
    PrintWriter err;

    ColorScheme colorScheme() {
        ColorScheme colors = scheme;
        if (colors == null) {
            colors = scheme = commandLine().getColorScheme();
        }
        return colors;
    }

    public PrintWriter out() {
        PrintWriter o = out;
        if (o == null) {
            o = out = commandLine().getOut();
        }
        return o;
    }

    public PrintWriter err() {
        PrintWriter e = err;
        if (e == null) {
            e = err = commandLine().getErr();
        }
        return e;
    }

    public boolean isShowErrors() {
        return showErrors || picocliDebugEnabled;
    }

    public boolean isVerbose() {
        return verbose || picocliDebugEnabled;
    }

    public boolean isCliTest() {
        return cliTestMode;
    }

    public boolean isAnsiEnabled() {
        return CommandLine.Help.Ansi.AUTO.enabled();
    }

    public void print(String text) {
        out().print(colorScheme().ansi().new Text(text, colorScheme()));
        out().flush(); // print is not flushing like println does
    }

    public void println(String text) {
        out().println(colorScheme().ansi().new Text(text, colorScheme()));
    }

    public void printText(String... text) {
        for (String line : text) {
            out().println(colorScheme().ansi().new Text(line, colorScheme()));
        }
    }

    public void printErrorText(String[] text) {
        for (String line : text) {
            err().println(colorScheme().errorText(line));
        }
    }

    public void printStackTrace(Exception ex) {
        if (isShowErrors()) {
            err().println(colorScheme().stackTraceText(ex));
        }
    }

    public Path getTestDirectory() {
        if (isCliTest()) {
            return testProjectRoot;
        }
        return null;
    }

    @Override
    public void info(String msg) {
        out().println(colorScheme().ansi().new Text(msg, colorScheme()));
    }

    @Override
    public void error(String msg) {
        err().println(colorScheme().errorText(ERROR_ICON + " " + msg));
    }

    @Override
    public boolean isDebugEnabled() {
        return isVerbose();
    }

    @Override
    public void debug(String msg) {
        if (isVerbose()) {
            out().println(colorScheme().ansi().new Text("@|faint [DEBUG] " + msg + "|@", colorScheme()));
        }
    }

    @Override
    public void warn(String msg) {
        out().println(colorScheme().ansi().new Text("@|yellow " + WARN_ICON + " " + msg + "|@", colorScheme()));
    }

    /**
     * Throws an UnmatchedArgumentException if there are any unmatched arguments in the given
     * CommandLine object.
     *
     * @param cmd the CommandLine object to check for unmatched arguments
     * @throws CommandLine.UnmatchedArgumentException if there are any unmatched arguments in the
     *                                                CommandLine object
     */
    public void throwIfUnmatchedArguments(CommandLine cmd) {
        List<String> unmatchedArguments = cmd.getUnmatchedArguments();
        if (!unmatchedArguments.isEmpty()) {
            throw new CommandLine.UnmatchedArgumentException(cmd, unmatchedArguments);
        }
    }

    /**
     * Returns the CommandLine object for the current command.
     * Convenience method to avoid having to call mixee.commandLine() directly. and to allow for easy mocking
     * @return the CommandLine object for the current command
     */
    public CommandLine commandLine() {
        return mixee.commandLine();
    }

    ExceptionHandler exceptionHandler;

    ExceptionHandler getExceptionHandler(){
        if(null == exceptionHandler){
           exceptionHandler = Arc.container().instance(ExceptionHandler.class).get();
        }
        return exceptionHandler;
    }

    /**
     * This method is used to handle exceptions that occur during command execution
     *
     * @param ex The exception that was thrown
     * @return The exit code
     */
    public int handleCommandException(Exception ex) {
        return handleCommandException(ex, null, isShowErrors(), true);
    }

    /**
     * This method is used to handle exceptions that occur during command execution
     *
     * @param ex            The exception that was thrown
     * @param showStackHelp Flag to show command help for error display
     * @return The exit code
     */
    public int handleCommandException(Exception ex, final boolean showStackHelp) {
        return handleCommandException(ex, null, isShowErrors(), showStackHelp);
    }

    /**
     * This method is used to handle exceptions that occur during command execution
     *
     * @param ex      The exception that was thrown
     * @param message A custom message to display
     * @return The exit code
     */
    public int handleCommandException(Exception ex, String message) {
        return handleCommandException(ex, message, isShowErrors(), true);
    }

    /**
     * This method allows me to explicitly set the showStack flag
     * in certain context like when an exception is thrown from an ExecutionHandler the showStack flag is set yet in our mixing
     * This is because the ExecutionHandler run too early and the command hasn't been prepared yet
     * Therefore, We need to be able to set it explicitly to remain consistent with the rest of the code
     * @param ex The exception that was thrown
     * @param message Custom message to display
     * @param showStack The flag to show errors
     * @param showStackHelp Flag to show command help for error display
     * @return The exit code
     */
    public int handleCommandException(final Exception ex, final String message,
            final boolean showStack, final boolean showStackHelp) {

        final ExceptionHandler exHandler = getExceptionHandler();
        // Handle ParameterException
        if (ex instanceof ParameterException) {
            CommandLine.UnmatchedArgumentException.printSuggestions(
                    (CommandLine.ParameterException) ex, out());
        }

        //Handle ExecutionException
        final Exception unwrappedEx = exHandler.unwrap(ex);

        if (unwrappedEx instanceof ForceSilentExitException) {
            //We don't want to print the stack trace if the exception is a ForceExitException
            return ((ForceSilentExitException) unwrappedEx).getExitCode();
        }

        //Extract the proper exception and remove all server side noise
        final Exception handledEx = exHandler.handle(unwrappedEx);
        //Short error message
        final String errorMessage = (message != null && !message.isEmpty() ? message + " " : "")
                + getMessage(ex, handledEx);

        error(errorMessage);
        Log.error(errorMessage, ex);
        //Won't print unless the "showStack" flag is on
        //We show the show message notice only if we're not already showing errors
        if (!showStack && showStackHelp) {
            info("@|bold,yellow run with -e or --errors for full details on the exception.|@");
        } else if (showStack) {
            err().println(colorScheme().stackTraceText(unwrappedEx));
        }

        final CommandLine cmd = commandLine();
        //If we want to force usage to be printed upon certain type of exception we could do:
        // cmd.usage(cmd.getOut());

        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper()
                .getExitCode(unwrappedEx)
                : mixee.exitCodeOnInvalidInput();
    }

    /**
     * Retrieves the error message for an exception.
     *
     * @param originalException the original exception that was thrown
     * @param handledEx         the exception that was handled
     * @return the error message for the exception, or a default message if no error message is
     * available
     */
    private String getMessage(final Exception originalException, final Exception handledEx) {

        var message = abbreviateMessageIfNeeded(handledEx);

        if (handledEx instanceof WebApplicationException) {
            message = getWebApplicationExceptionMessage((WebApplicationException) handledEx);
        }

        if (originalException instanceof PushException ||
                originalException instanceof PullException ||
                originalException instanceof TraversalTaskException) {

            if (message != null) {
                message = String.format(
                        "%s %n %s",
                        abbreviateMessageIfNeeded(originalException.getMessage()),
                        abbreviateMessageIfNeeded(message)
                );
            } else {
                message = abbreviateMessageIfNeeded(originalException);
            }
        }

        return message != null ? message : "An exception " + handledEx.getClass().getSimpleName()
                + " Occurred With no error message provided.";
    }

    /**
     * Abbreviates the exception message if needed based on the configuration.
     *
     * @param exception the exception with the message to abbreviate.
     * @return the abbreviated exception message, if enabled; otherwise, the complete exception
     * message.
     */
    private String abbreviateMessageIfNeeded(final Exception exception) {
        return abbreviateMessageIfNeeded(exception.getMessage());
    }

    /**
     * Abbreviates the message if needed based on the configuration.
     *
     * @param message the message to abbreviate.
     * @return the abbreviated message, if enabled; otherwise, the original message.
     */
    private String abbreviateMessageIfNeeded(final String message) {

        // If the configuration values are not set, read them from the configuration.
        if (exceptionAbbreviateEnabled == null) {
            readAbbreviateConfig();
        }

        if (exceptionAbbreviateEnabled) {
            return abbreviate(message, "...", exceptionMaxWidth);
        }

        return message;
    }

    /**
     * Reads the configuration for exception message abbreviation. Sets the values of
     * `exceptionAbbreviateEnabled` and `exceptionMaxWidth` based on the configuration. If the
     * configuration values are not present, the default values of `false` and `200` are used.
     */
    private void readAbbreviateConfig() {
        this.exceptionAbbreviateEnabled = ConfigProvider.getConfig()
                .getOptionalValue("exception.message.abbreviate.enabled", Boolean.class)
                .orElse(false);
        this.exceptionMaxWidth = ConfigProvider.getConfig()
                .getOptionalValue("exception.message.abbreviate.maxWidth", Integer.class)
                .orElse(200);
    }

    /**
     * On recent versions of RESTEasy, the custom message is stored in the reasonPhrase
     * The WebApplicationException message is immutable and can't be changed 404 is always "Not Found"
     * @param ex The exception that was thrown
     * @return The message to display
     */
    String getWebApplicationExceptionMessage(final WebApplicationException ex) {
        final Response response = ex.getResponse();
        if (response instanceof BuiltResponse) {
            final String reasonPhrase = ((BuiltResponse) response).getReasonPhrase();
            if (null != reasonPhrase) {
                return reasonPhrase;
            }
        }
        return ex.getMessage();
    }

    @Override
    public String toString() {
        return "OutputOptions [testMode=" + cliTestMode
                + ", showErrors=" + showErrors
                + ", verbose=" + verbose + "]";
    }

}
