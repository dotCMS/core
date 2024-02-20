package com.dotcms.cli.common;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.WARN_ICON;
import static org.apache.commons.lang3.StringUtils.abbreviate;

import com.dotcms.cli.exception.ExceptionHandler;
import io.quarkus.arc.Arc;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.logging.Log;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

public class OutputOptionMixin implements MessageWriter {

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
            colors = scheme = mixee.commandLine().getColorScheme();
        }
        return colors;
    }

    public PrintWriter out() {
        PrintWriter o = out;
        if (o == null) {
            o = out = mixee.commandLine().getOut();
        }
        return o;
    }

    public PrintWriter err() {
        PrintWriter e = err;
        if (e == null) {
            e = err = mixee.commandLine().getErr();
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


    ExceptionHandler exceptionHandler;

    ExceptionHandler getExceptionHandler(){
        if(null == exceptionHandler){
           exceptionHandler = Arc.container().instance(ExceptionHandler.class).get();
        }
        return exceptionHandler;
    }

    public int handleCommandException(Exception ex, String message){
        return handleCommandException(ex, message, isShowErrors());
    }

    /**
     * This method allows me to explicitly set the showErrors flag
     * in certain context like when an exception is thrown from an ExecutionHandler the showErrors flag is set yet in our mixing
     * This is because the ExecutionHandler run too early and the command hasn't been prepared yet
     * Therefore, We need to be able to set it explicitly to remain consistent with the rest of the code
     * @param ex The exception that was thrown
     * @param message The message to display
     * @param showErrors The flag to show errors
     * @return The exit code
     */
    public int handleCommandException(Exception ex, String message, boolean showErrors) {

        final ExceptionHandler exHandler = getExceptionHandler();

        // Handle ParameterException
        if (ex instanceof ParameterException) {
            CommandLine.UnmatchedArgumentException.printSuggestions(
                    (CommandLine.ParameterException) ex, out());
        }

        //Handle ExecutionException

        final Exception unwrappedEx = exHandler.unwrap(ex);

        //Extract the proper exception and remove all server side noise
        final Exception handledEx = exHandler.handle(unwrappedEx);
        //Short error message
        message = String.format("%s %s  ", message,
                handledEx.getMessage() != null ? abbreviate(handledEx.getMessage(), "...", 200)
                        :  "An exception " + handledEx.getClass().getSimpleName() + " Occurred With no error message provided.");
        error(message);
        Log.error(message, ex);
        //Won't print unless the "showErrors" flag is on
        //We show the show message notice only if we're not already showing errors
        if(!showErrors) {
            info("@|bold,yellow run with -e or --errors for full details on the exception.|@");
        } else {
            err().println(colorScheme().stackTraceText(unwrappedEx));
        }

        final CommandLine cmd = mixee.commandLine();
        //If we want to force usage to be printed upon certain type of exception we could do:
        // cmd.usage(cmd.getOut());

        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper()
                .getExitCode(unwrappedEx)
                : mixee.exitCodeOnInvalidInput();
    }

    @Override
    public String toString() {
        return "OutputOptions [testMode=" + cliTestMode
                + ", showErrors=" + showErrors
                + ", verbose=" + verbose + "]";
    }

}
