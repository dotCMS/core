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
          return instance.tryLock(key, callback);
    }

    @Override
    public void tryLock(final String key, final VoidDelegate callback) throws Throwable {
          instance.tryLock(key, callback);
    }
}
