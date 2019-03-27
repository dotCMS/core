package com.dotcms.concurrent.lock;

public interface DotKeyLockManagerFactory {

    <K> DotKeyLockManager<K> create(String name);

}
