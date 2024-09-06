package com.dotcms.cli.command;

import com.google.common.util.concurrent.Striped;
import io.quarkus.arc.DefaultBean;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * This is a Push shared Context
 * it is basically a set of keys that are used to keep track of what has been pushed or deleted
 * The context can be shared across multiple threads to safely keep track of what has been pushed or deleted
 * The stored key is composed by the operation followed by the resource URI (e.g. delete::/content/123)
 */
@DefaultBean
@Dependent
public class PushContextImpl implements PushContext {

    public static final String KEY_FORMAT = "%s::%s";
    @ConfigProperty(name = "push-context.strips", defaultValue = "100")
    int stripes;

    @ConfigProperty(name = "push-context.enabled", defaultValue = "true")
    boolean enabled;

    @Inject
    Logger logger;

    private final Set<String> savedKeys = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("UnstableApiUsage")
    Striped<Lock> lockStriped;

    @SuppressWarnings("UnstableApiUsage")
    @PostConstruct
    void init(){
        lockStriped = Striped.lazyWeakLock(stripes);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(String key) {
        final String keyLowerCase = key.toLowerCase();
        return savedKeys.contains(keyLowerCase);
    }

    /**
     * Executes the delegate within a lock and if the operation carried out by the delegate is successful the key is saved
     * But first a check is done to see if the key has already been saved if so no operation is carried out returning an empty optional
     * @param key
     * @param delegate
     * @return
     * @throws LockExecException
     */
      <T> Optional <T> execWithinLock(String key, Delegate<T> delegate) throws LockExecException {
        if(!enabled) {
            logger.warn("Push context is disabled");
            return delegate.execute();
        }
        final String keyLowerCase = key.toLowerCase();
        if (savedKeys.contains(keyLowerCase)) {
            return Optional.empty();
        }
        @SuppressWarnings("UnstableApiUsage")
        final Lock lock = this.lockStriped.get(keyLowerCase);
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
     * {@inheritDoc}
     */
    public <T> Optional <T> execDelete(String key, Delegate <T> delegate) throws LockExecException {
        return execWithinLock(String.format(KEY_FORMAT, Operation.DELETE, key), delegate);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Optional <T> execPush(String key, Delegate <T> delegate) throws LockExecException {
        return execWithinLock(String.format(KEY_FORMAT, Operation.PUSH, key), delegate);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Optional <T> execArchive(String key, Delegate <T> delegate) throws LockExecException {
        return execWithinLock(String.format(KEY_FORMAT, Operation.ARCHIVE, key), delegate);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        savedKeys.clear();
    }

}
