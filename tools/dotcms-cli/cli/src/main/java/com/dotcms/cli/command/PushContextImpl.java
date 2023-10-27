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

    private final Set<String> savedKeys = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("UnstableApiUsage")
    Striped<Lock> lockStriped = Striped.lazyWeakLock(50);

    public boolean contains(String key) {
        final String keyLowerCase = key.toLowerCase();
        return savedKeys.contains(keyLowerCase);
    }

    public boolean execWithinLock(String key, SaveDelegate delegate) throws LockExecException {

        final String keyLowerCase = key.toLowerCase();
        if (savedKeys.contains(keyLowerCase)) {
            return false;
        }
        @SuppressWarnings("UnstableApiUsage")
        Lock lock = this.lockStriped.get(keyLowerCase);
        lock.lock();
        try {
            final boolean ok = delegate.execute();
            if (ok) {
                savedKeys.add(keyLowerCase);
            }
            return ok;
        } finally {
            lock.unlock();
        }
    }

}
