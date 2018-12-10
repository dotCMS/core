package com.dotcms.concurrent.lock;

import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;

public class IdentifierStripedLock implements DotKeyLockManager<String> {

    private final DotKeyLockManager<String> instance;

    public IdentifierStripedLock(final DotKeyLockManager<String> instance) {
        this.instance = instance;
    }

    @Override
    public <R> R tryLock(final String key, final ReturnableDelegate<R> callback) throws Throwable {
        final String threadName = Thread.currentThread().getName();
        Thread.currentThread().setName(threadName + " : " + key);
        try {
            return instance.tryLock(key, callback);
        }finally {
            Thread.currentThread().setName(threadName);
        }
    }

    @Override
    public void tryLock(final String key, final VoidDelegate callback) throws Throwable {
        final String threadName = Thread.currentThread().getName();
        Thread.currentThread().setName(threadName + " : " + key);
        try {
            instance.tryLock(key, callback);
        }finally {
            Thread.currentThread().setName(threadName);
        }
    }
}
