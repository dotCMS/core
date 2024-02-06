package com.dotcms.cli.exception;

import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;

public class UninitializedStateException extends ExecutionException {

    public UninitializedStateException(CommandLine commandLine, String msg) {
        super(commandLine, msg);
    }

}
