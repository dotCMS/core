package com.dotcms.cli.command;

public interface PushContext {

    boolean contains(String uri);

    boolean execWithinLock(String key, SaveDelegate delegate) throws LockExecException;

    @FunctionalInterface
    interface SaveDelegate {
        boolean execute() throws LockExecException;
    }

    class LockExecException extends Exception {
    }

}
