package com.dotmarketing.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Class with convenience methods related to the runtime DotCMS is currently running in.
 *
 * @author vico
 */
public class RuntimeUtils {

    private static final String DOCKERENV_FILE = "/.dockerenv";
    private static final boolean INSIDE_DOCKER = Files.exists(Paths.get(DOCKERENV_FILE));

    public static final String AMD64_ARCH = "amd64";
    public static final String ARM64_ARCH = "aarch64";
    public static final String X86_64_ARCH = "x86_64";

    /**
     * Evaluates if instance is running inside Docker.
     *
     * @return true if runningn inside Docker, otherwise false
     */
    public static boolean isInsideDocker() {
        return INSIDE_DOCKER;
    }

    /**
     * Retrieves the {@link InputStream} associated to the {@link Process} as result of calling the provided commands.
     *
     * @param commands list of commands
     * @return stream as a result of running a process
     * @throws IOException
     */
    public static InputStream getRunProcessStream(final String... commands) throws IOException {
        final ProcessBuilder processBuilder = buildProcess(commands);
        return new BufferedInputStream(processBuilder.start().getInputStream());
    }

    /**
     * Retrieves a {@link String} as a result from running the {@link Process} of calling the provided commands.
     * Potential command errors are NOT reported back, and the terminal output is slightly formatted.
     *
     * @param commands list of commands
     * @return optional wrapping the result of running the process
     */
    public static Optional<String> runProcessAndGetOutput(final String... commands) {
        final TerminalOutput commandOutput = runProcessAndGetOutput(false, true, commands);
        return UtilMethods.isSet(commandOutput.output()) ? Optional.of(commandOutput.output()) : Optional.empty();
    }

    /**
     * Generates a {@link TerminalOutput} as a result from running the {@link Process} of calling the provided commands.
     * Potential command errors are reported back, and the terminal output is NOT formatted at all.
     *
     * @param commands The list of commands that will be run.
     * @return The {@link TerminalOutput} object with details of the result of the operation.
     */
    public static TerminalOutput runProcessAndGetOutputWithError(final String... commands) {
        return runProcessAndGetOutput(true, false, commands);
    }

    /**
     * Generates a {@link TerminalOutput} as a result from running the {@link Process} of calling the provided commands.
     * A {@link TerminalOutput} object will be returned in order to provide more information on the final status of the
     * operation.
     *
     * @param returnErrors If potential error messages need to be tracked, set this to {@code true}.
     * @param formatOutput If slight formatting must be applied to the terminal output, set this to {@code true}.
     * @param commands     The list of commands that will be executed.
     *
     * @return The {@link TerminalOutput} object with details of the result of the operation.
     */
    protected static TerminalOutput runProcessAndGetOutput(final boolean returnErrors, final boolean formatOutput, final String... commands) {
        final TerminalOutput terminalOutput = new TerminalOutput();
        final Process process = Try.of(() -> runProcess(commands)).getOrNull();
        if (process == null) {
            Logger.warn(
                    RuntimeUtils.class,
                    String.format("Cannot run process for provided command %s", String.join(" ", commands)));
            return terminalOutput;
        }

        try {
            final String input = Try.of(() -> IOUtils.toString(
                            process.getInputStream(),
                            Charset.defaultCharset()))
                                         .getOrNull();
            final int exitValue = Try.of(process::waitFor).getOrElse(1);
            terminalOutput.exitValue(exitValue);
            if (!returnErrors && exitValue != 0) {
                terminalOutput.output(StringPool.BLANK);
                return terminalOutput;
            }
            if (formatOutput) {
                terminalOutput.output(UtilMethods.isSet(input) ? input.replace(System.lineSeparator(), "") : null);
                return terminalOutput;
            } else {
                terminalOutput.output(UtilMethods.isSet(input) ? input : null);
            }
            return terminalOutput;
        } catch (final Exception e) {
            Logger.error(RuntimeUtils.class, String.format("Error running commands [ %s ]: %s", String.join(" ",
                    commands), e.getMessage()), e);
            return terminalOutput;
        } finally {
            process.destroy();
        }
    }

    /**
     * Creates a {@link ProcessBuilder} with the provided commands to be executed later.
     *
     * @param commands list of commands
     * @return process builder
     */
    private static ProcessBuilder buildProcess(String[] commands) {
        Logger.info(RuntimeUtils.class, String.format("Executing commands %s", String.join(" ", commands)));
        return new ProcessBuilder(commands).redirectErrorStream(true);
    }

    /**
     * Execute commands and return a {@link Process} instance.
     *
     * @param commands list of commands
     * @return process instances
     */
    private static Process runProcess(final String... commands) {
        final ProcessBuilder processBuilder = buildProcess(commands);
        return Try.of(processBuilder::start).getOrElseThrow(DotRuntimeException::new);
    }

    /**
     * Provides more information related to the execution of a CLI command. For example, you can retrieve the exit value
     * generated by the Terminal along with the output generated upon a command's execution.
     */
    public static class TerminalOutput {

        private int exitValue;
        private String output;

        /**
         * Default class constructor.
         */
        public TerminalOutput() {
            this(-1, StringPool.BLANK);
        }

        /**
         * Creates an instance of this class with a given exit value and Terminal output.
         *
         * @param exitValue The exit value generated after executing a command.
         * @param output    The Terminal output.
         */
        public TerminalOutput(final int exitValue, final String output) {
            this.exitValue = exitValue;
            this.output = output;
        }

        /**
         * Returns the exit value generated by the Terminal.
         *
         * @return The exit value.
         */
        public int exitValue() {
            return this.exitValue;
        }

        /**
         * Sets the exit value generated by the Terminal.
         *
         * @param exitValue The exit value.
         */
        public void exitValue(int exitValue) {
            this.exitValue = exitValue;
        }

        /**
         * Returns the output generated by the Terminal.
         *
         * @return The Terminal output.
         */
        public String output() {
            return this.output;
        }

        /**
         * Sets the output generated by the Terminal.
         *
         * @param output The Terminal output.
         */
        public void output(String output) {
            this.output = output;
        }

        /**
         * Returns {@code true} when the exit value after running the specified command equals zero, which means a
         * successful execution.
         *
         * @return If the command execution was successful, returns {@code true}.
         */
        public boolean successful() {
            return 0 == this.exitValue ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Returns {@code true} when the exit value after running the specified command equals zero, and additional
         * Terminal output is generated. This allows you to get potential information when a command is successfully
         * executed.
         *
         * @return If the command execution was successful, returns {@code true}.
         */
        public boolean successfulWithOutput() {
            return 0 == this.exitValue && UtilMethods.isSet(this.output) ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Returns {@code true} when the exit value after running the specified command is NOT zero.
         *
         * @return If the command execution failed or an error was returned, returns {@code true}.
         */
        public boolean failed() {
            return 0 != this.exitValue ? Boolean.TRUE : Boolean.FALSE;
        }

        @Override
        public String toString() {
            return "TerminalOutput{" + "exitValue=" + exitValue + ", output='" + output + '\'' + '}';
        }

    }

}
