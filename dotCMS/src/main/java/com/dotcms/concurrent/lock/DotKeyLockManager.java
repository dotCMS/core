package com.dotcms.concurrent.lock;

import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;

/**
 * Interface to expose a fine grained key base lock implementation
 * @param <K>
 */
public interface DotKeyLockManager<K> {

    /**
     * Tries to acquire a Lock based on a key (Gives up upon time limit is reached)
     * @param key The key to lock upon
     * @param callback The callback with the critical code to protect
     * @return the value returned by the callback
     * @throws Throwable
     */
    <R> R tryLock(K key, ReturnableDelegate<R> callback) throws Throwable;

    /**
     * Tries to acquire a Lock based on a key (Gives up upon time limit is reached)
     * @param key The key to lock upon
     * @param callback The void callback with the critical code to protect
     * @throws Throwable
     */
    void tryLock(K key, VoidDelegate callback) throws Throwable;


}
