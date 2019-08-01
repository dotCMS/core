package com.dotcms.concurrent.lock;

import com.dotcms.concurrent.DotConcurrentException;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.util.concurrent.Striped;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


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

    private static final String DOTCMS_CONCURRENT_LOCK_DISABLE = "dotcms.concurrent.locks.disable";

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

        if (Config
                .getBooleanProperty(DOTCMS_CONCURRENT_LOCK_DISABLE, Boolean.FALSE)) {
            return callback.execute();
        }

        final ReentrantLock lock = ReentrantLock.class.cast(lockStripes.get(key));
        if (lock.isHeldByCurrentThread()) {
            Logger.debug(StripedLockImpl.class,
                    "Lock already held by current thread we can still proceed.");
            return callback.execute();
        } else {
            if (!lock.tryLock(time, unit)) {
                Logger.error(StripedLockImpl.class,dumpThread(lock, key));
                throw new DotConcurrentException(
                        String.format("Unable to acquire Lock on key `%s` and thread `%s` ", key,
                                Thread.currentThread().getName())
                );
            }
            try {
                return callback.execute();
            } finally {
                lock.unlock();
            }
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

        if (Config
                .getBooleanProperty(DOTCMS_CONCURRENT_LOCK_DISABLE, Boolean.FALSE)) {
            callback.execute();
            return;
        }

        final ReentrantLock lock = ReentrantLock.class.cast(lockStripes.get(key));
        if (lock.isHeldByCurrentThread()) {
            Logger.debug(StripedLockImpl.class,
                    "Lock already held by current thread we can still proceed.");
            callback.execute();
        } else {
            if (!lock.tryLock(time, unit)) {
                Logger.error(StripedLockImpl.class,dumpThread(lock, key));
                throw new DotConcurrentException(
                        String.format("Unable to acquire Lock on key `%s` and thread `%s` ", key,
                                Thread.currentThread().getName())
                );
            }
            try {
                callback.execute();
            } finally {
                lock.unlock();
            }
        }
    }

    private Thread getOwnerThread(final ReentrantLock lock) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
            final Method method = lock.getClass().getDeclaredMethod("getOwner");
            if(!method.isAccessible()){
                method.setAccessible(true);
            }
            return Thread.class.cast(method.invoke(lock));
    }

    private String dumpThread(final ReentrantLock lock, final K key){
        String dumpString = "";
          try{
            final Thread thread = getOwnerThread(lock);
            dumpString = String.format(" Key: `%s`, Thread: `%s`, Lock: `%s` \n %s",key, thread, lock,
                ExceptionUtil.getStackTraceAsString(thread.getStackTrace())
            );
            }catch (Exception e){
               //Empty suck it up.
            }
        return dumpString;
    }
}
