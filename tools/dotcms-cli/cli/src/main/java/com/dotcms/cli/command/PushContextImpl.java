package com.dotcms.cli.command;

import com.google.common.util.concurrent.Striped;
import io.quarkus.arc.DefaultBean;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import javax.enterprise.context.Dependent;

/**
 * Push shared Context??
 */
@DefaultBean
@Dependent
public class PushContextImpl implements PushContext {

    private final Set<String> deletedResourceURI = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("UnstableApiUsage")
    Striped<Lock> lockStriped = Striped.lazyWeakLock(50);

    public boolean deletedAlready(String uri) {
        return deletedResourceURI.contains(uri.toLowerCase());
    }

    public <T> T execWithinLock(String key, Delegate<T> delegate) throws LockExecException {
        @SuppressWarnings("UnstableApiUsage")
        Lock lock = this.lockStriped.get(key);
        lock.lock();
        try {
            return delegate.execute();
        } finally {
            lock.unlock();
        }
    }

}
