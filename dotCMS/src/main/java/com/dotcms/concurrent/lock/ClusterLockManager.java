package com.dotcms.concurrent.lock;

import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;

/**
 * Defines a ClusterLockManager that allows to do cluster lock
 * @author jsanca
 */
public interface ClusterLockManager<K> extends DotKeyLockManager<K> {

    /**
     * Gets the key of the lock (usually would be the name of the lock manager)
     * @return
     */
    K getName();

    /**
     * Tries to acquire a cluster Lock based
     * @param callback The callback with the critical code to protect
     * @return the value returned by the callback
     * @throws Throwable
     */
    default <R> R tryClusterLock(final ReturnableDelegate<R> callback) throws Throwable {

        return this.tryLock(this.getName(), callback);
    }


    /**
     * Tries to acquire a cluster Lock based
     * @param callback The void callback with the critical code to protect
     * @throws Throwable
     */
    default void tryClusterLock(final VoidDelegate callback) throws Throwable {

        this.tryLock(this.getName(), callback);
    }
}
