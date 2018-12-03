package com.dotcms.concurrent.lock;

import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.google.common.util.concurrent.Striped;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


/**
 * Strip lock Implementation - backed-up by Google Guava Strip class
 * @param <K>
 * @param <R>
 */
public class StripedLockImpl <K,R> implements DotKeyLockManager<K,R> {

    static final int DEFAULT_STRIPES = 26;

    static final int DEFAULT_TIME = 1;

    static final TimeUnit DEFAULT_TU = TimeUnit.SECONDS;

    private final TimeUnit unit;

    private final long time;

    //This is a collection like struct that holds references the Lock implementation of choice.
    //It'll allocate locks based upon a key.
    private final Striped<Lock> lockStripes;

    /**
     * constructor 1 takes only the number of strips to be allocated
     * @param stripes
     */
    StripedLockImpl(final int stripes) {
        this(stripes, DEFAULT_TIME, DEFAULT_TU);
    }

    /**
     * Constructor 2 takes the number of strips to be allocated + a time specification to instruct the try-lock methods
     * This will instantiate the Collection of Strips establishing an initial size.
     * The collection will hold null references until a lock instance is required.
     * The References are WeakReferences that will be collected once the entry is no longer in use.
     * @param stripes
     * @param time
     * @param unit
     */
    StripedLockImpl(final int stripes, final long time, final TimeUnit unit) {
        lockStripes = Striped.lazyWeakLock(stripes);
        this.time = time;
        this.unit = unit;
    }

    /**
     * @inheritDoc
     *
     */
    @Override
    public R lock(final K key, final ReturnableDelegate<R> callback) throws Throwable {
        final Lock lock = lockStripes.get(key);
        try {
             lock.lock();
             return callback.execute();
        } finally {
             lock.unlock();
        }
    }

    /**
     * @inheritDoc
     *
     */
    @Override
    public R tryLock(final K key, final ReturnableDelegate<R> callback) throws Throwable {
        final Lock lock = lockStripes.get(key);
        try {
            lock.tryLock(time, unit);
            return callback.execute();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @inheritDoc
     *
     */
    @Override
    public void lock(final K key, final VoidDelegate callback) throws Throwable {
        final Lock lock = lockStripes.get(key);
        try {
            lock.lock();
            callback.execute();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @inheritDoc
     *
     */
    @Override
    public void tryLock(final K key, final VoidDelegate callback) throws Throwable {
        final Lock lock = lockStripes.get(key);
        try {
            lock.tryLock(time, unit);
            callback.execute();
        } finally {
            lock.unlock();
        }
    }

}
