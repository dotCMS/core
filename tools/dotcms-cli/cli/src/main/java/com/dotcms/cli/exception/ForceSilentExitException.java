package com.dotcms.cli.exception;

/**
 * Exception to force a silent exit from the CLI
 */
public class ForceSilentExitException extends RuntimeException {

    /**
     * The exit code to be used when exiting
     */
    final int exitCode;

    /**
     * Constructor
     * @param exitCode The exit code to be used when exiting
     */
    public ForceSilentExitException(int exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Returns the exit code to be used when exiting
     * @return The exit code to be used when exiting
     */
    public int getExitCode() {
        return exitCode;
    }

}
