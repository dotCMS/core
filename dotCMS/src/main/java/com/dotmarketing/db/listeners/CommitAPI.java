package com.dotmarketing.db.listeners;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

public class CommitAPI {


    public enum CommitListenerStatus {
        DISABLED, ALLOW_SYNC, ALLOW_ASYNC;
    }
    
    private static class apiHolder {
        static final CommitAPI api = new CommitAPI();
    }

    public static CommitAPI getInstance() {
        return apiHolder.api;
    }
    
    private CommitListenerStatus nextStatus = CommitListenerStatus.ALLOW_ASYNC;
    
    
    public void forceStatus(final CommitListenerStatus status) {
        this.nextStatus=status;
    }
    
    /**
     * Status for listeners of thread-local -based transactions. This allows to control as to which
     * listeners are fired on a commit
     */
    private final ThreadLocal<CommitListenerStatus> listenersStatus = new ThreadLocal<CommitListenerStatus>() {
        protected CommitListenerStatus initialValue() {
            return CommitListenerStatus.ALLOW_ASYNC;
        }
    };


    /**
     * Returns the listeners status currently associated to the thread-local -based transaction
     * (ALLOW_ASYNC by default)
     */
    public CommitListenerStatus status() {
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

    /**
     * Sets the commit listener status - convienience method
     * 
     * @param status
     */
    public void status(CommitListenerStatus status) {
        this.setCommitListenerStatus(status);
    }

    final int COMMIT_LISTENER_QUEUE_SIZE;
    final int COMMIT_LISTENER_THREADPOOL_SIZE;
    final ExecutorService submitter;

    
    final LinkedBlockingDeque<Runnable> asyncQueue;
    
    
    private CommitAPI() {
        COMMIT_LISTENER_QUEUE_SIZE = Config.getIntProperty("COMMIT_LISTENER_QUEUE_SIZE", 10000);
        COMMIT_LISTENER_THREADPOOL_SIZE = Config.getIntProperty("COMMIT_LISTENER_THREADPOOL_SIZE", 10);
        asyncQueue =  new LinkedBlockingDeque<Runnable>(COMMIT_LISTENER_QUEUE_SIZE);
        submitter = new ThreadPoolExecutor(1, COMMIT_LISTENER_THREADPOOL_SIZE, 30, TimeUnit.SECONDS,
                        asyncQueue );

    }







    @VisibleForTesting
    static final ThreadLocal<Map<String, DotListener>> asyncCommitListeners = ThreadLocal.withInitial(LinkedHashMap::new);

    @VisibleForTesting
    static final ThreadLocal<Map<String, DotListener>> syncCommitListeners = ThreadLocal.withInitial(LinkedHashMap::new);

    @VisibleForTesting
    static final ThreadLocal<Map<String, DotListener>> rollbackListeners = ThreadLocal.withInitial(LinkedHashMap::new);


    /**
     * adds this to the end of async CommitListener list
     */
    public void addCommitListenerAsync(final String key, final Runnable runnable) {
        if (!DbConnectionFactory.inTransaction())
            return;

        CommitListener listener=new CommitListener() {
            
            @Override
            public void run() {
                runnable.run();
                
            }
            
            @Override
            public String key() {
                return key;
            }
        };
        addCommitListenerAsync(listener);
        
    }
    
    /**
     * adds this to the end of async CommitListener list
     */
    public void addCommitListenerSync(final String key, final Runnable runnable) {
        if (!DbConnectionFactory.inTransaction())
            return;

        CommitListener listener=new CommitListener() {
            
            @Override
            public void run() {
                runnable.run();
                
            }
            
            @Override
            public String key() {
                return key;
            }
        };
        addCommitListenerSync(listener);
        
    }
    /**
     * adds this to the end of async CommitListener list
     */
    public void addCommitListenerAsync(final CommitListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        asyncCommitListeners.get().remove(listener.key(), listener);
        asyncCommitListeners.get().put(listener.key(), listener);
    }
    
    

    /**
     * adds this to the end of sync CommitListener list
     */
    public void addCommitListenerSync(final CommitListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        syncCommitListeners.get().remove(listener.key(), listener);
        syncCommitListeners.get().put(listener.key(), listener);
    }

    /**
     * adds this to the end of sync CommitListener list
     */
    public void addReindexListenerSync(final ReindexListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        syncCommitListeners.get().remove(listener.key(), listener);
        syncCommitListeners.get().put(listener.key(), listener);
    }

    /**
     * adds this to the end of async CommitListener list
     */
    public void addReindexListenerAsync(final ReindexListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        asyncCommitListeners.get().remove(listener.key(), listener);
        asyncCommitListeners.get().put(listener.key(), listener);
    }

    /**
     * adds this to the end of sync CommitListener list
     */
    public void addFlushCacheSync(final FlushCacheListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        syncCommitListeners.get().remove(listener.key());
        syncCommitListeners.get().put(listener.key(), listener);
        addRollBackListener(listener);
    }

    /**
     * adds this to the end of async CommitListener list
     */
    public void addFlushCacheAsync(final FlushCacheListener listener) {
        if (!DbConnectionFactory.inTransaction())
            return;
        asyncCommitListeners.get().remove(listener.key());
        asyncCommitListeners.get().put(listener.key(), listener);
        addRollBackListener(listener);

    }

    /**
     * adds this to the end of rollback CommitListener list
     */
    public void addRollBackListener(final RollbackListener runable) {
        if (!DbConnectionFactory.inTransaction())
            return;
        rollbackListeners.get().remove(runable.key(), runable);
        rollbackListeners.get().put(runable.key(), runable);
    }

    /**
     * adds this to the end of rollback CommitListener list
     */
    public void addRollBackListener(final DotListener listener) {
        addRollBackListener(new RollbackListener(listener));
    }

    /**
     * clears the listeners and resets the state to ALLOW_ASYNC
     */
    private void resetListeners() {
        rollbackListeners.get().clear();
        syncCommitListeners.get().clear();
        asyncCommitListeners.get().clear();
        setCommitListenerStatus(nextStatus);
        nextStatus = CommitListenerStatus.ALLOW_ASYNC;
    }


    /**
     * This method is fired AFTER a commit and BEFORE a connection is closed and returned to the pool
     */
    public void finalizeListeners() {
        try {
            if (DbConnectionFactory.inTransaction()) {
                throw new DotStateException("Commit Listeners need run after a commit has taken place");
            }
            switch (status()) {
                case DISABLED:
                    return;
                case ALLOW_SYNC:
                    syncCommitListeners.get().putAll(asyncCommitListeners.get());
                    runSyncListeners();
                    return;
                default:
                    runSyncListeners();
                    runAsyncListeners();
            }

        } finally {
            resetListeners();
        }
    }

    /**
     * This method is fired AFTER a failed commit has been rolled back and BEFORE a connection is closed
     * and returned to the pool
     */
    public void finalizeRollback() {
        try {
            if (DbConnectionFactory.inTransaction()) {
                throw new DotStateException("Rollback Listeners need run after a commit been attempted");
            }
            switch (status()) {
                case DISABLED:
                    return;
                default:
                    runRollbackListeners();
            }
        } finally {
            resetListeners();
        }
    }

    /**
     * Runs the SyncListeners
     */
    private void runSyncListeners() {
        final List<Runnable> listeners = new ArrayList<>(syncCommitListeners.get().values());
        if (listeners.isEmpty()) {
            return;
        }
        Logger.info(this.getClass(), "Running " + syncCommitListeners.get().size() + " SyncListeners");
        listeners.stream().filter(r -> r instanceof FlushCacheListener).forEach(r -> r.run());
        listeners.stream().filter(r -> r instanceof CommitListener).forEach(r -> r.run());
        listeners.stream().filter(r -> r instanceof ReindexListener).forEach(r -> r.run());

    }

    /**
     * Runs the AyncListeners
     */
    private void runAsyncListeners() {

        final List<Runnable> listeners = new ArrayList<>(asyncCommitListeners.get().values());
        if (listeners.isEmpty()) {
            return;
        }
        Logger.info(this.getClass(), "Async Queue " + asyncCommitListeners.get().size() + " submitted");
        listeners.stream().filter(r -> r instanceof FlushCacheListener).forEach(r -> submitter.submit(r));
        listeners.stream().filter(r -> r instanceof CommitListener).forEach(r -> submitter.submit(r));
        listeners.stream().filter(r -> r instanceof ReindexListener).forEach(r -> submitter.submit(r));

        Logger.info(this.getClass(), "Async Queue " + asyncQueue.size() + " AsyncListeners Pending");

    }

    /**
     * Runs the RollbackListeners
     */
    private void runRollbackListeners() {
        Logger.info(this.getClass(), "Running " + rollbackListeners.get().size() + " RollbackListeners");
        rollbackListeners.get().values().stream().forEach(r -> r.run());
    }

    public void startListeners() {
        if (!DbConnectionFactory.inTransaction()) {
            throw new DotStateException("Listeners need to be started in committable transaction");
        }
        resetListeners();

    }



}
