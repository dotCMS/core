package com.dotcms.concurrent.lock;

import com.dotcms.concurrent.DotConcurrentException;
import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.google.common.util.concurrent.Striped;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


/**
 * Strip lock Implementation - backed-up by Google Guava Striped class
 */
public class StripedLockImpl<K> implements DotKeyLockManager<K> {

    static final int DEFAULT_STRIPES = 64;

    static final int DEFAULT_TIME = 3;

    static final TimeUnit DEFAULT_TU = TimeUnit.SECONDS;

    private final TimeUnit unit;

    private final long time;

    //This is a collection like struct that holds references the Lock implementation of choice.
    //It'll allocate locks based upon a key.
    private final Striped<Lock> lockStripes;

    /**
     * Constructor 2 takes the number of strips to be allocated + a time specification to instruct
     * the try-lock methods This will instantiate the Collection of Stripes establishing an initial
     * size. The collection will hold null references until a lock instance is required. The
     * References are WeakReferences that will be collected once the entry is no longer in use.
     */
    StripedLockImpl(final int stripes, final long time, final TimeUnit unit) {
        lockStripes = Striped.lazyWeakLock(stripes);
        this.time = time;
        this.unit = unit;
    }

    /**
     * @inheritDoc
     */
    @Override
    public <R> R tryLock(final K key, final ReturnableDelegate<R> callback) throws Throwable {
        return this.tryLock(key, callback, this.time, this.unit);
    }


    /**
     * @inheritDoc
     */
    @Override
    public void tryLock(final K key, final VoidDelegate callback) throws Throwable {
        tryLock(key, callback, this.time, this.unit);
    }

    /**
     * Internal try lock impl. With Returnable Delegate
     * These methods are hidden from the public interface
     * @param key
     * @param callback
     * @param time
     * @param unit
     * @param <R>
     * @return
     * @throws Throwable
     */
    <R> R tryLock(final K key, final ReturnableDelegate<R> callback, final long time,
            final TimeUnit unit) throws Throwable {
        final Lock lock = lockStripes.get(key);
        if (!lock.tryLock(time, unit)) {
            throw new DotConcurrentException(
                    String.format("Unable to acquire Lock on key `%s` and thread `%s` ", key, Thread.currentThread().getName())
                    );
        }
        try {
            return callback.execute();
        } finally {
            lock.unlock();
        }
    }


    /**
     * Internal try lock impl. With Void Delegate
     * These methods are hidden from the public interface
     * @param key
     * @param callback
     * @param time
     * @param unit
     * @throws Throwable
     */
    void tryLock(final K key, final VoidDelegate callback, final long time, final TimeUnit unit)
            throws Throwable {
        final Lock lock = lockStripes.get(key);
        if (!lock.tryLock(time, unit)) {
            throw new DotConcurrentException(
                    String.format("Unable to acquire Lock on key `%s` and thread `%s` ", key, Thread.currentThread().getName())
                    );
        }
        try {
            callback.execute();
        } finally {
            lock.unlock();
        }
    }
}
