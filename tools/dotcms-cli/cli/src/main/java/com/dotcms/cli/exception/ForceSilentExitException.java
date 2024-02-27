package com.dotcms.cli.exception;

public class ForceSilentExitException extends RuntimeException{

    final int exitCode;

    public ForceSilentExitException(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

}
