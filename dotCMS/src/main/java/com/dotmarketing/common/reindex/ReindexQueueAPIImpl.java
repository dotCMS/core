/**
 * 
 */
package com.dotmarketing.common.reindex;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ElasticReadOnlyCommand;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.ReturnableDelegate;
import com.dotcms.util.VoidDelegate;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 *
 */
public class ReindexQueueAPIImpl implements ReindexQueueAPI {

    private final ReindexQueueFactory reindexQueueFactory;
    private final ElasticReadOnlyCommand esReadOnlyMonitor;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private Thread reindexThread;

    private final ThreadLocal<Boolean> isExecuting = ThreadLocal.withInitial(() -> false);


    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();


    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    private <T> CompletableFuture<T> executeAsync(ReturnableDelegate<T> task) {
        try {
            start();
            CompletableFuture<T> future = new CompletableFuture<>();
            if (Boolean.TRUE.equals(isExecuting.get())) {
                // execute the task directly if the current thread is from the executor
                // Prevents recursive calls causing deadlocks
                try {
                    T response = task.execute();
                    future.complete(response);
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                }
            } else {
                queue.put(() -> {
                    try {
                        isExecuting.set(true);
                        T response = task.execute();
                        future.complete(response);
                    } catch (Throwable ex) {
                        future.completeExceptionally(ex);
                    } finally {
                        isExecuting.set(false);
                    }
                });
            }

            return future;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to execute task", ex);
        }
    }

    public <T>  T execute(ReturnableDelegate<T> task) throws DotDataException {
           return executeAsync(task).join();
    }


    public void execute(VoidDelegate task) throws DotDataException {
        try {
            executeAsync(task).join();
        } catch (Exception e) {
            throw new DotDataException(e);
        }
    }

    public void executeInTransaction(VoidDelegate task) throws DotDataException {
        try {
            executeAsync(() -> LocalTransaction.wrap(task)).join();
        } catch (Exception e) {
            throw new DotDataException(e);
        }
    }

    public <T>  T executeInTransaction(ReturnableDelegate<T> task) throws DotDataException {
        return executeAsync(() -> LocalTransaction.wrapReturn(task)).join();
    }

    private CompletableFuture<Void> executeAsync(VoidDelegate task) {
        try {
            start();
            CompletableFuture<Void> future = new CompletableFuture<>();
            if (isExecuting.get()) {
                // execute the task directly if the current thread is from the executor
                try {
                    task.execute();
                    future.complete(null);
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                }
            } else {
                queue.put(() -> {
                    try {
                        isExecuting.set(true);
                        task.execute();
                        future.complete(null);
                    } catch (Throwable ex) {
                        future.completeExceptionally(ex);
                    } finally {
                        isExecuting.set(false);
                    }
                });
            }

            return future;
        }  catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to execute task", ex);
        }

    }


    private void processQueue() {
        try {
            do {
                Runnable task = queue.take();
                task.run();
            } while (started.get() && !Thread.currentThread().isInterrupted());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to process queue", e);
        }
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            Logger.info(this, "Reindex Task Queue Starting");
            executor.submit(this::processQueue);
        }
    }

    public void stop() throws DotDataException {
        if (started.compareAndSet(true, false)) {
                execute(ExitTask::new);
        }
    }

    public ReindexQueueAPIImpl() {
        this(FactoryLocator.getReindexQueueFactory(), ElasticReadOnlyCommand.getInstance());
    }

    @VisibleForTesting
    public ReindexQueueAPIImpl(final ReindexQueueFactory reindexQueueFactory, final ElasticReadOnlyCommand esReadOnlyMonitor) {
        this.reindexQueueFactory = reindexQueueFactory;
        this.esReadOnlyMonitor = esReadOnlyMonitor;
    }

    @Override
    public void addStructureReindexEntries(final ContentType contentType) throws DotDataException {
        executeInTransaction(() -> reindexQueueFactory.addStructureReindexEntries(contentType));
    }

    @Override
    public synchronized void addAllToReindexQueue() throws DotDataException {
           execute(reindexQueueFactory::addAllToReindexQueue);
    }

    @Override
    @CloseDBIfOpened
    public Map<String, ReindexEntry> findContentToReindex() throws DotDataException {
        return this.findContentToReindex(
                            ReindexQueueFactory.REINDEX_RECORDS_TO_FETCH);
    }

    @Override
    @CloseDBIfOpened
    public Map<String, ReindexEntry> findContentToReindex(final int recordsToReturn) throws DotDataException {
        return reindexQueueFactory.findContentToReindex(recordsToReturn);
    }

    @Override
    public void deleteReindexEntry(ReindexEntry iJournal) throws DotDataException {
        executeInTransaction(()-> reindexQueueFactory.deleteReindexEntry(iJournal));
    }

    @Override
    @CloseDBIfOpened
    public boolean areRecordsLeftToIndex() throws DotDataException {
        return reindexQueueFactory.areRecordsLeftToIndex();
    }

    @Override
    @CloseDBIfOpened
    public long recordsInQueue() throws DotDataException {
        return recordsInQueue(DbConnectionFactory.getConnection());
    }
    
    @Override
    @CloseDBIfOpened
    public long failedRecordCount() throws DotDataException {
        return reindexQueueFactory.failedRecordCount();
    }
    @Override
    @CloseDBIfOpened
    public boolean hasReindexRecords() throws DotDataException {
        return reindexQueueFactory.hasReindexRecords();
    }

    @Override
    public long recordsInQueue(Connection conn) throws DotDataException {
        return reindexQueueFactory.recordsInQueue(conn);
    }

    @Override
    public void deleteReindexAndFailedRecords() throws DotDataException {
        executeInTransaction(reindexQueueFactory::deleteReindexAndFailedRecords);
    }

    @Override
    public void deleteReindexRecords() throws DotDataException {
        executeInTransaction(reindexQueueFactory::deleteReindexRecords);
    }
    
    
    @Override
    public void deleteFailedRecords() throws DotDataException {
        reindexQueueFactory.deleteFailedRecords();
    }

    @Override

    public void refreshContentUnderHost(Host host) throws DotDataException {
        executeInTransaction(()->reindexQueueFactory.refreshContentUnderHost(host));
    }

    @Override
    public void refreshContentUnderFolder(Folder folder) throws DotDataException {
        executeInTransaction(()->reindexQueueFactory.refreshContentUnderFolder(folder));
    }

    @Override
    public void refreshContentUnderFolderPath(String hostId, String folderPath) throws DotDataException {
        executeInTransaction(() ->reindexQueueFactory.refreshContentUnderFolderPath(hostId, folderPath));
    }

    @Override
    public List<ReindexEntry> getFailedReindexRecords() throws DotDataException {
        return reindexQueueFactory.getFailedReindexRecords();
    }

    @Override
    public void addIdentifierReindex(final String id) throws DotDataException {
        executeInTransaction(()->this.reindexQueueFactory.addIdentifierReindex(id));
    }

    @Override
    public void addIdentifierReindex(final String identifier, int priority) throws DotDataException {
        executeInTransaction(()->this.reindexQueueFactory.addIdentifierReindex(identifier, priority));
    }


    @Override
    public void addReindexHighPriority(final String identifier) throws DotDataException {

        executeInTransaction(()->this.reindexQueueFactory.addReindexHighPriority(identifier));
    }


    @Override
    public int addIdentifierReindex(final Collection<String> ids) throws DotDataException {

        return executeInTransaction(() -> this.reindexQueueFactory.addIdentifierReindex(ids));
    }

    @Override
    public int addIdentifierDelete(final Collection<String> ids) throws DotDataException {

        
        return executeInTransaction(()->this.reindexQueueFactory.addIdentifierDelete(ids,Priority.NORMAL.dbValue()));
        
    }

    @Override
    public int addIdentifierDelete(final String id) throws DotDataException {
        return addIdentifierDelete(List.of(id));
    }
    @Override
    public int addReindexHighPriority(final Collection<String> ids) throws DotDataException {

        return executeInTransaction(()->this.reindexQueueFactory.addReindexHighPriority(ids));
    }

    @Override
    public void addContentletReindex(final Contentlet contentlet) throws DotDataException {

        executeInTransaction(()->this.reindexQueueFactory.addIdentifierReindex(contentlet.getIdentifier()));
    }


    @Override
    public void addContentletsReindex(final Collection<Contentlet> contentlet) throws DotDataException {
        executeInTransaction(() -> contentlet.forEach(con -> {
            try {
                this.reindexQueueFactory.addIdentifierReindex(con.getIdentifier());
            } catch (DotDataException e) {
                Logger.warnAndDebug(this.getClass(), e);
            }

        }));
    }

    @Override
    public void addIdentifierReindex(final Identifier identifier) throws DotDataException {

        executeInTransaction(() ->this.reindexQueueFactory.addIdentifierReindex(identifier.getId()));
    }

    @Override
    public void deleteReindexEntry(List<ReindexEntry> recordsToDelete) throws DotDataException {
        executeInTransaction(() ->reindexQueueFactory.deleteReindexEntry(recordsToDelete));
    }

    @Override
    public void deleteReindexEntry(String identiferToDelete) throws DotDataException {
        executeInTransaction(() ->reindexQueueFactory.deleteReindexEntry(identiferToDelete));
    }

    @Override
    public void markAsFailed(final ReindexEntry idx, final String cause) throws DotDataException {
        executeInTransaction(() -> reindexQueueFactory.markAsFailed(idx, UtilMethods.shortenString(cause, 300)));
    }

    private class ExitTask implements Runnable {
        @Override
        public void run() {
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }


}
