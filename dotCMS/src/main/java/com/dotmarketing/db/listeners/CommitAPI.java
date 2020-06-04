package com.dotmarketing.db.listeners;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

public class CommitAPI {


    public enum CommitListenerStatus {
        ENABLED, DISABLED;
    }

    /**
     * Status for listeners of thread-local -based transactions. This allows to control whether
     * listeners are appended or not (ENABLED by default)
     */
    private final ThreadLocal<CommitListenerStatus> listenersStatus = new ThreadLocal<CommitListenerStatus>() {
        protected CommitListenerStatus initialValue() {
            return CommitListenerStatus.ENABLED;
        }
    };


    /**
     * Returns the listeners status currently associated to the thread-local -based transaction (ENABLED
     * by default)
     */
    public CommitListenerStatus getCommitListenerStatus() {
        return listenersStatus.get();
    }

    /**
     * Allows to override the status of the listeners associated to the current thread-local -based
     * transaction (DISABLED if overriden) When using TransactionListenerStatus.DISABLED, client code
     * should be aware of controlling the operations that are suppossed to be done by listeners
     * 
     * @param status TransactionListenerStatus
     */
    public void setCommitListenerStatus(CommitListenerStatus status) {
        listenersStatus.set(status);
    }
    
    final int COMMIT_LISTENER_QUEUE_SIZE ;
    final int COMMIT_LISTENER_THREADPOOL_SIZE ;
    final ExecutorService submitter ;
    
    private CommitAPI() {
        COMMIT_LISTENER_QUEUE_SIZE = Config.getIntProperty("COMMIT_LISTENER_QUEUE_SIZE", 10000);
        COMMIT_LISTENER_THREADPOOL_SIZE= Config.getIntProperty("COMMIT_LISTENER_THREADPOOL_SIZE", 10);
        submitter = new ThreadPoolExecutor(1, COMMIT_LISTENER_QUEUE_SIZE, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(COMMIT_LISTENER_THREADPOOL_SIZE));

    }



    private static class apiHolder {
        static final CommitAPI api = new CommitAPI();
    }

    public static CommitAPI getInstance() {

        return apiHolder.api;

    }


    
    @VisibleForTesting
    static final ThreadLocal<Map<String, DotListener>> asyncCommitListeners = ThreadLocal.withInitial(LinkedHashMap::new);

    @VisibleForTesting
    static final ThreadLocal<Map<String, DotListener>> syncCommitListeners = ThreadLocal.withInitial(LinkedHashMap::new);
    
    @VisibleForTesting
    static final ThreadLocal<Map<String, DotListener>> rollbackListeners = ThreadLocal.withInitial(LinkedHashMap::new);



    public void addCommitListenerAsync(final CommitListener runnable) {
        if (!DbConnectionFactory.inTransaction())
            return;
        asyncCommitListeners.get().remove(runnable.key());
        asyncCommitListeners.get().put(runnable.key(), runnable);

    }


    public void addCommitListenerSync(final CommitListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        syncCommitListeners.get().remove(listener.key(), listener);
        syncCommitListeners.get().put(listener.key(), listener);
    }

    public void addReindexListenerSync(final ReindexListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        syncCommitListeners.get().remove(listener.key(), listener);
        syncCommitListeners.get().put(listener.key(), listener);
    }

    public void addReindexListenerAsync(final ReindexListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        asyncCommitListeners.get().remove(listener.key(), listener);
        asyncCommitListeners.get().put(listener.key(), listener);
    }


    public void addFlushCacheSync(final FlushCacheListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        syncCommitListeners.get().remove(listener.key());
        syncCommitListeners.get().put(listener.key(), listener);
        addRollBackListener(listener);
    }

    public void addFlushCacheAsync(final FlushCacheListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        asyncCommitListeners.get().remove(listener.key());
        asyncCommitListeners.get().put(listener.key(), listener);
        addRollBackListener(listener);
        
    }

    public void addRollBackListener(final RollbackListener runable) {
        if (!DbConnectionFactory.inTransaction())
            return;
        rollbackListeners.get().remove(runable.key(), runable);
        rollbackListeners.get().put(runable.key(), runable);
    }

    public void addRollBackListener(final DotListener listener) {
        addRollBackListener(new RollbackListener(listener));
    }


    /**
     * The commit has happened
     */
    public void finalize() {
        runSyncListeners();
        rollbackListeners.get().clear();
        runAsyncListeners();

    }

    private void runSyncListeners() {
        final List<Runnable> listeners = new ArrayList<>(syncCommitListeners.get().values());
        if(!listeners.isEmpty()) {
            syncCommitListeners.get().clear();
            listeners.stream().filter(r -> r instanceof FlushCacheListener).forEach(r -> r.run());
            listeners.stream().filter(r -> r instanceof CommitListener).forEach(r -> r.run());
            listeners.stream().filter(r -> r instanceof ReindexListener).forEach(r -> r.run());
        }
    }


    private void runAsyncListeners() {

        final List<Runnable> listeners = new ArrayList<>(asyncCommitListeners.get().values());
        if(!listeners.isEmpty()) {
            asyncCommitListeners.get().clear();
            listeners.stream().filter(r -> r instanceof FlushCacheListener).forEach(r -> submitter.submit(r));
            listeners.stream().filter(r -> r instanceof CommitListener).forEach(r -> submitter.submit(r));
            listeners.stream().filter(r -> r instanceof ReindexListener).forEach(r -> submitter.submit(r));
            
        }
    }

    public void runRollbackListeners() {
        syncCommitListeners.get().clear();
        asyncCommitListeners.get().clear();
        rollbackListeners.get().values().stream().forEach(r -> r.run());
        rollbackListeners.get().clear();

    }

    public void startTransaction() {
        if (!DbConnectionFactory.inTransaction()) {
            syncCommitListeners.get().clear();
            asyncCommitListeners.get().clear();
            rollbackListeners.get().clear();
        }else {
            Logger.warn(this.getClass(), "Unable to startTransaction - we are already in a transaction");
        }
        
    }



}
