package com.dotmarketing.util;

import com.dotmarketing.exception.DotRuntimeException;
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
        final Process process = Try.of(() -> runProcess(commands)).getOrNull();
        if (process == null) {
            Logger.warn(
                    RuntimeUtils.class,
                    String.format("Cannot run process for provided command %s", String.join(" ", commands)));
            return Optional.empty();
        }

        try {
            final String input = Try.of(() -> IOUtils.toString(
                            process.getInputStream(),
                            Charset.defaultCharset()))
                    .getOrNull();

            if (Try.of(process::waitFor).getOrElse(1) != 0) {
                return Optional.empty();
            }

            return Optional.ofNullable(UtilMethods.isSet(input) ? input.replace(System.lineSeparator(), "") : null);
        } catch (Exception e) {
            Logger.error(RuntimeUtils.class, String.format("Error running commands %s", String.join(" ", commands)), e);
            return Optional.empty();
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

}

