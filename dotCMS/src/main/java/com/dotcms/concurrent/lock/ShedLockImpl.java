package com.dotcms.concurrent.lock;

import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;

import java.time.Instant;

/**
 * Implements a ShedLock (a cluster lock)
 * By default implements lock by database and 600 seconds of timeout
 * @author jsanca
 */
public class ShedLockImpl<K> implements DotKeyLockManager<K> {

    private static final String DEFAULT_SHEDLOCK_TABLE_NAME = "shedlock";
    private static final int DEFAULT_SECONDS_TIMEOUT = 600;
    private final LockingTaskExecutor executor;
    private final long timeOut;
    private final String name;

    ShedLockImpl (final String name) {

        this(name, new JdbcLockProvider(DbConnectionFactory.getDataSource(), DEFAULT_SHEDLOCK_TABLE_NAME));
    }

    ShedLockImpl (final String name, final LockProvider lockProvider) {

        this(name, lockProvider, DEFAULT_SECONDS_TIMEOUT);
    }

    public ShedLockImpl (final String name, final LockProvider lockProvider, final long timeout) {

        this.name     = name;
        this.executor = new DefaultLockingTaskExecutor(lockProvider);
        this.timeOut  = timeout;
    }

    @Override
    public <R> R tryLock(final K key, final ReturnableDelegate<R> callback) throws Throwable {

        Logger.debug(this, ()-> "Calling the ShedLock, key: " + key);

        final Instant lockAtMostUntil = Instant.now().plusSeconds(this.timeOut);
        final LockingTaskExecutor.TaskWithResult<R> task = ()-> callback.execute();
        final LockingTaskExecutor.TaskResult<R> taskResult = this.executor.executeWithLock(task, new LockConfiguration(key.toString(), lockAtMostUntil));
        return null != taskResult? taskResult.getResult(): null;
    }

    @Override
    public void tryLock(final K key, final VoidDelegate callback) throws Throwable {

        Logger.debug(this, ()-> "Calling the ShedLock, key: " + key);

        final Instant lockAtMostUntil = Instant.now().plusSeconds(this.timeOut);
        final Runnable task = ()-> Try.run(()->callback.execute()).getOrElseThrow(e-> new RuntimeException(e));
        this.executor.executeWithLock(task, new LockConfiguration(key.toString(), lockAtMostUntil));
    }

    @Override
    public String toString() {
        return "ShedLockImpl{" +
                "timeOut=" + timeOut +
                ", name='" + name + '\'' +
                '}';
    }
}
