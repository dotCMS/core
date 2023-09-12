package com.dotcms.concurrent.lock;

/**
 * Cluster (ShedLock) implementation
 * @author jsanca
 */
public class ClusterLockManagerFactory implements DotKeyLockManagerFactory {

    @Override
    public DotKeyLockManager<String> create(final String name) {

        return new ClusterLockManagerImpl<>(name);
    }
}
