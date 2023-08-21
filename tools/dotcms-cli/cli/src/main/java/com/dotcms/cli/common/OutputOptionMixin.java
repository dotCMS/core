package com.dotcms.cli.common;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.WARN_ICON;
import static org.apache.commons.lang3.StringUtils.*;

import io.quarkus.devtools.messagewriter.MessageWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;

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

    // CommandLine must be passed in (forwarded commands)
    public void throwIfUnmatchedArguments(CommandLine cmd) {
        List<String> unmatchedArguments = cmd.getUnmatchedArguments();
        if (!unmatchedArguments.isEmpty()) {
            throw new CommandLine.UnmatchedArgumentException(cmd, unmatchedArguments);
        }
    }

    public int handleCommandException(Exception ex, String message) {
        printStackTrace(ex);
        if (ex instanceof CommandLine.ParameterException) {
            CommandLine.UnmatchedArgumentException.printSuggestions(
                    (CommandLine.ParameterException) ex, out());
        }
            //Yeah, this doesn't look like the right logic
            //in the sense that we're printing out the error only when the showErrors is false
            //But there's a reason for that.
            //ShowErrors is on when we want to see the full stack trace and this block is meant to be used to show a shorten output
            if (!isShowErrors()) {
                if (null != ex) {
                    //Extract the proper exception and remove all server side noise
                    ex = ExceptionHandler.handle(ex);
                    message = String.format("%s %s  ", message,
                            ex.getMessage() != null ? abbreviate(ex.getMessage(), "...", 200)
                                    : "No error message was provided");
                }
                error(message);
                info("@|bold,yellow run with -e or --errors for full details on the exception.|@");
            }

        final CommandLine cmd = mixee.commandLine();
        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper()
                .getExitCode(ex)
                : mixee.exitCodeOnInvalidInput();
    }

    @Override
    public String toString() {
        return "OutputOptions [testMode=" + cliTestMode
                + ", showErrors=" + showErrors
                + ", verbose=" + verbose + "]";
    }

}
