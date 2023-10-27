package com.dotcms.cli.command;

import java.util.Optional;

public interface PushContext {

    boolean contains(String uri);

     <T>  Optional <T> execWithinLock(String key, Delegate<T> delegate) throws LockExecException;

    <T>  Optional <T> execDelete(String key, Delegate <T> delegate) throws LockExecException;

    <T>  Optional <T> execPush(String key, Delegate <T> delegate) throws LockExecException;

    <T>  Optional <T> execArchive(String key, Delegate <T> delegate) throws LockExecException;

    @FunctionalInterface
    interface Delegate <T> {
        Optional <T> execute() throws LockExecException;
    }
    class LockExecException extends Exception {

    }
    enum Operation {
        PUSH,ARCHIVE,DELETE
    }

}
