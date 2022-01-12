package com.dotmarketing.util;

import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Class with convenience methods related to the system DotCMS is currently running.
 */
public class RuntimeUtils {

    private static final String DOCKERENV_FILE = "/.dockerenv";
    private static final boolean INSIDE_DOCKER = Files.exists(Paths.get(DOCKERENV_FILE));

    public static boolean isThisInsideDocker() {
        return INSIDE_DOCKER;
    }

    public static Process runProcess(final String... commands) {
        final ProcessBuilder processBuilder = buildProcess(commands);
        return Try.of(processBuilder::start).getOrElseThrow(DotRuntimeException::new);
    }

    public static InputStream getRunProcessStream(final String... commands) throws IOException {
        final ProcessBuilder processBuilder = buildProcess(commands);
        return new BufferedInputStream(processBuilder.start().getInputStream());
    }

    public static Optional<String> runProcessAndGetOutput(final String... commands) {
        final Process process = runProcess(commands);
        final String input = Try.of(() -> IOUtils.toString(
                        process.getInputStream(),
                        Charset.defaultCharset()))
                .getOrNull();

        if (Try.of(process::waitFor).getOrElse(1) != 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(UtilMethods.isSet(input) ? input.replace(System.lineSeparator(), "") : null);
    }

    public static String resolveDockerHostArch() {
        try {
            return runProcessAndGetOutput("uname", "-r")
                    .orElseThrow(() -> new DotRuntimeException("Unable to determine docker host arch"));
        } catch (Exception e) {
            Logger.warn(RuntimeUtils.class, "Error resolving docker host, falling back to JVM system property", e);
            return System.getProperty("os.arch");
        }
    }

    public static String resolveArch() {
        return isThisInsideDocker() ? resolveDockerHostArch() : System.getProperty("os.arch");
    }

    @NotNull
    private static ProcessBuilder buildProcess(String[] commands) {
        Logger.info(RuntimeUtils.class, String.format("Executing commands %s", String.join(" ", commands)));
        return new ProcessBuilder(commands).redirectErrorStream(true);
    }

}

