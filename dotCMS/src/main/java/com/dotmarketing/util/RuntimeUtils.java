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
     *
     * @param commands list of commands
     * @return optional wrapping the result of running the process
     */
    public static Optional<String> runProcessAndGetOutput(final String... commands) {
        final TerminalOutput commandOutput = runProcessAndGetOutput(false, true, commands);
        return UtilMethods.isSet(commandOutput.output()) ? Optional.of(commandOutput.output()) : Optional.empty();
    }

    /**
     *
     * @param commands
     * @return
     */
    public static TerminalOutput runProcessAndGetOutputWithError(final String... commands) {
        return runProcessAndGetOutput(true, false, commands);
    }

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
                //return Optional.ofNullable(UtilMethods.isSet(input) ? input.replace(System.lineSeparator(), "") : null);
                terminalOutput.output(UtilMethods.isSet(input) ? input.replace(System.lineSeparator(), "") : null);
                return terminalOutput;
            } else {
                terminalOutput.output(UtilMethods.isSet(input) ? input : null);
            }
            return terminalOutput;
        } catch (Exception e) {
            Logger.error(RuntimeUtils.class, String.format("Error running commands %s", String.join(" ", commands)), e);
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
     *
     */
    public static class TerminalOutput {

        private int exitValue;
        private String output;

        public TerminalOutput() {
            this(-1, StringPool.BLANK);
        }

        public TerminalOutput(final int exitValue, final String output) {
            this.exitValue = exitValue;
            this.output = output;
        }

        public int exitValue() {
            return exitValue;
        }

        public void exitValue(int exitValue) {
            this.exitValue = exitValue;
        }

        public String output() {
            return output;
        }

        public void output(String output) {
            this.output = output;
        }

        @Override
        public String toString() {
            return "TerminalOutput{" + "exitValue=" + exitValue + ", output='" + output + '\'' + '}';
        }

    }

}

