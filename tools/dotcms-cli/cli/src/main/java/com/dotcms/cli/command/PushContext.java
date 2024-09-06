package com.dotcms.cli.command;

import java.util.Optional;

/**
 * This is a Push shared Context
 * it is basically a set of keys that are used to keep track of what has been pushed or deleted
 * The context can be shared across multiple threads to safely keep track of what has been pushed or deleted
 * The stored key is composed by the operation followed by the resource URI (e.g. delete::/content/123)
 */
public interface PushContext {

    /**
     * Queries the key in the context
     * @param key the key to query
     */
    boolean contains(String uri);

    /**
     * Executes the delegate within a lock and if the operation carried out by the delegate is successful the key is saved
     * @param key
     * @param delegate
     * @return an optional with the result of the delegate
     * @throws LockExecException
     */
    <T>  Optional <T> execDelete(String key, Delegate <T> delegate) throws LockExecException;

    /**
     * Executes the delegate within a lock and if the operation carried out by the delegate is successful the key is saved
     * @param key
     * @param delegate
     * @return an optional with the result of the delegate
     * @throws LockExecException
     */
    <T>  Optional <T> execPush(String key, Delegate <T> delegate) throws LockExecException;

    /**
     * Executes the delegate within a lock and if the operation carried out by the delegate is successful the key is saved
     * @param key
     * @param delegate
     * @return an optional with the result of the delegate
     * @param <T>
     * @throws LockExecException
     */
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

    /**
     * Clears the context
     */
    void clear();

}
