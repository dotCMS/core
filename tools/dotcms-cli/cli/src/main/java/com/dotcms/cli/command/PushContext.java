package com.dotcms.cli.command;

public interface PushContext {

    boolean deletedAlready(String uri);

    <T> T execWithinLock(String key, Delegate<T> delegate) throws LockExecException;

    @FunctionalInterface
    public interface Delegate<T> {
        T execute() throws LockExecException;
    }

    class LockExecException extends Exception {
    }

}
