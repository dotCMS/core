package com.dotcms.cli.command;

import com.google.common.util.concurrent.Striped;
import io.quarkus.arc.DefaultBean;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * This is a Push shared Context
 * it is basically a set of keys that are used to keep track of what has been pushed or deleted
 * The context can be shared across multiple threads to safely keep track of what has been pushed or deleted
 * The stored key is composed by the operation followed by the resource URI (e.g. delete::/content/123)
 */
@DefaultBean
@Dependent
public class PushContextImpl implements PushContext {

    @ConfigProperty(name = "push-context.strips",defaultValue = "100")
    int stripes;

    private final Set<String> savedKeys = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("UnstableApiUsage")
    Striped<Lock> lockStriped;

    @SuppressWarnings("UnstableApiUsage")
    @PostConstruct
    void init(){
        lockStriped = Striped.lazyWeakLock(stripes);
    }

    public boolean contains(String key) {
        final String keyLowerCase = key.toLowerCase();
        return savedKeys.contains(keyLowerCase);
    }

    /**
     * Executes the delegate within a lock and if the operation carried out by the delegate is successful the key is saved
     * But first a check is done to see if the key has already been saved if so no operation is carried out
     * @param key
     * @param delegate
     * @return
     * @throws LockExecException
     */
    public  <T>  Optional <T> execWithinLock(String key, Delegate<T> delegate) throws LockExecException {
        final String keyLowerCase = key.toLowerCase();
        if (savedKeys.contains(keyLowerCase)) {
            return Optional.empty();
        }
        @SuppressWarnings("UnstableApiUsage")
        Lock lock = this.lockStriped.get(keyLowerCase);
        lock.lock();
        try {
            final Optional <T> result = delegate.execute();
            if (result.isPresent()) {
                savedKeys.add(keyLowerCase);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Executes the delegate within a lock and if the operation carried out by the delegate is successful the key is saved
     * @param key
     * @param delegate
     * @return
     * @throws LockExecException
     */
    public <T>  Optional <T> execDelete(String key, Delegate <T> delegate) throws LockExecException {
        return execWithinLock(String.format("%s::%s", Operation.DELETE, key), delegate);
    }

    /**
     * Executes the delegate within a lock and if the operation carried out by the delegate is successful the key is saved
     * @param key
     * @param delegate
     * @return
     * @throws LockExecException
     */
    public <T>  Optional <T> execPush(String key, Delegate <T> delegate) throws LockExecException {
        return execWithinLock(String.format("%s::%s", Operation.PUSH, key), delegate);
    }

    public <T>  Optional <T> execArchive(String key, Delegate <T> delegate) throws LockExecException {
        return execWithinLock(String.format("%s::%s", Operation.ARCHIVE, key), delegate);
    }


}
