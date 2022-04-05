package com.dotcms.concurrent.lock;

/**
 * ShedLock implementation
 * @author jsanca
 */
public class ShedKeyLockManagerFactory implements DotKeyLockManagerFactory {

    @Override
    public <K> DotKeyLockManager<K> create(final String name) {

        return new ShedLockImpl<>(name);
    }
}
