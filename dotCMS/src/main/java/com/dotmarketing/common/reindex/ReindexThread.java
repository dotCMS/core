package com.dotmarketing.common.reindex;

import com.dotmarketing.common.reindex.BulkProcessorListener.ReindexResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.SystemCache;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ElasticReadOnlyCommand;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import com.dotmarketing.util.ThreadUtils;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.elasticsearch.action.bulk.BulkProcessor;

/**
 * This thread is in charge of re-indexing the contenlet information placed in the
 * {@code dist_reindex_journal} table. This process is constantly checking the existence of any
 * record in the table and will add its information to the Elastic index.
 * <p>
 * The records added to the table will have a priority level set by the
 * {@link ReindexQueueFactory.Priority} enum. During the process, all the "correct" contents will be
 * processed and re-indexed first. All the "bad" records (contents that could not be re-indexed)
 * will be set a different priority level and be given more opportunities to be re-indexed after all
 * of the correct contents have already been processed.
 * </p>
 * <p>
 * The number of times the bad contents can re-try the re-index process is specified by the
 * {@link ReindexQueueFactory#REINDEX_MAX_FAILURE_ATTEMPTS} property, which can be customized
 * through the {@code dotmarketing-config.properties} file. If a content cannot be re-indexed after
 * all the specified attempts, a notification will be sent to the Notification Bar indicating the
 * Identifier of the bad contentlet. This way users can keep track of the failed records and check
 * the logs to get more information about the failure.
 * </p>
 * <p>
 * The reasons why a content cannot be re-indexed can be, for example:
 * <ul>
 * <li>Incorrect data format in the contentlet's data, such as malformed JSON data.</li>
 * <li>Association to orphaned data, such as being associated to an Inode that does not exist in the
 * system.</li>
 * <li>A Content Page, in which one or more of its parent folders do not exist in the system
 * anymore.</li>
 * </ul>
 * </p>
 *
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 */
public class ReindexThread {



    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final ScheduledExecutorService executor;

    AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    private CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);

    private final ContentletIndexAPI indexAPI;
    private final ReindexQueueAPI queueApi;
    private final NotificationAPI notificationAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;

    private static final ReindexThread INSTANCE = new ReindexThread();

    private final long sleep = Config.getLongProperty("REINDEX_THREAD_SLEEP", 250);
    private final int sleepOnError = Config.getIntProperty("REINDEX_THREAD_SLEEP_ON_ERROR", 500);
    private final AtomicLong contentletsIndexed = new AtomicLong(0);
    // bulk up to this many requests
    public static final int ELASTICSEARCH_BULK_ACTIONS =
            Config.getIntProperty("REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS", 10);

    // How often should the bulk request processor should flush its request - default 3 seconds
    public static final int ELASTICSEARCH_BULK_FLUSH_INTERVAL =
            Config.getIntProperty("REINDEX_THREAD_ELASTICSEARCH_BULK_FLUSH_INTERVAL_MS", 3000);

    // Setting this to number > 0 makes each bulk request asynchronous,
    // If set to 0 the bulk requests will be performed synchronously
    public static final int ELASTICSEARCH_CONCURRENT_REQUESTS =
            Config.getIntProperty("REINDEX_THREAD_CONCURRENT_REQUESTS", 1);

    // Max Bulk size in MB. -1 means disabled
    public static final int ELASTICSEARCH_BULK_SIZE =
            Config.getIntProperty("REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE", 1);

    // Time (in seconds) to wait before closing bulk processor in a full reindex
    private static final int BULK_PROCESSOR_AWAIT_TIMEOUT = Config.getIntProperty(
            "BULK_PROCESSOR_AWAIT_TIMEOUT", 20);

    public static final int BACKOFF_POLICY_TIME_IN_SECONDS = Config.getIntProperty(
            "BACKOFF_POLICY_TIME_IN_SECONDS", 20);

    public static final int BACKOFF_POLICY_MAX_RETRYS = Config.getIntProperty(
            "BACKOFF_POLICY_MAX_RETRYS", 10);


    public static final int WAIT_BEFORE_PAUSE_SECONDS = Config.getIntProperty(
            "WAIT_BEFORE_PAUSE_SECONDS", 0);


    private static final String REINDEX_THREAD_PAUSED = "REINDEX_THREAD_PAUSED";
    private static final Lazy<SystemCache> cache = Lazy.of(() -> CacheLocator.getSystemCache());

    private static final AtomicBoolean rebuildBulkIndexer = new AtomicBoolean(false);

    public static void rebuildBulkIndexer() {
        Logger.warn(ReindexThread.class, "--- ReindexThread BulkProcessor needs to be Rebuilt");
        ReindexThread.rebuildBulkIndexer.set(true);
    }

    private ReindexThread() {

        this(APILocator.getReindexQueueAPI(), APILocator.getNotificationAPI(),
                APILocator.getUserAPI(), APILocator.getRoleAPI(),
                APILocator.getContentletIndexAPI());
    }

    @VisibleForTesting
    ReindexThread(final ReindexQueueAPI queueApi, final NotificationAPI notificationAPI,
            final UserAPI userAPI,
            final RoleAPI roleAPI, final ContentletIndexAPI indexAPI) {
        this.queueApi = queueApi;
        this.notificationAPI = notificationAPI;
        this.userAPI = userAPI;
        this.roleAPI = roleAPI;
        this.indexAPI = indexAPI;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public static ReindexThread getInstance() {
        return INSTANCE;
    }


    public void start() {
        if (running.compareAndSet(false, true)) {

            currentTask = CompletableFuture.runAsync(this::run, executor)
                    .exceptionally(ex -> {
                        Logger.error(ReindexThread.class, "ReindexThread Task failed: " + ex.getMessage());
                        return null;
                    })
                    .thenRun(() -> running.set(false));
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            shutdownRequested.set(true);
            paused.set(false);
            try {
                currentTask.get(60, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                Logger.error(ReindexThread.class, "Error stopping ReindexThread", e);
            }
        }
    }


    public void setPaused() {
        if (paused.compareAndSet(false, true)) {
            Logger.debug(ReindexThread.class, "--- ReindexThread - Paused");
            cache.get().put(REINDEX_THREAD_PAUSED, System.currentTimeMillis() + Duration
                    .ofMinutes(Config.getIntProperty("REINDEX_THREAD_PAUSE_IN_MINUTES", 10))
                    .toMillis());
        }
    }

    public void unpauseInt() {
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            Logger.debug(ReindexThread.class, "--- Adding unpause commit listener");
            HibernateUtil.addCommitListener("unpauseIndex", this::unpauseImpl);
        } else {
            unpauseImpl();
        }
    }

    private void unpauseImpl() {
        if (paused.compareAndSet(true, false)) {
            Logger.debug(ReindexThread.class, "--- ReindexThread - Unpaused");
            cache.get().remove(REINDEX_THREAD_PAUSED);
        }
        if (!running.get()) {
            start();
        }
    }


    private void run() {
        Logger.info(this.getClass(),
                "---  ReindexThread is starting, background indexing will begin");
        BulkProcessorContext context = new BulkProcessorContext(null, null);
        while (running.get() && !shutdownRequested.get() && !Thread.currentThread().isInterrupted()) {
            try {
            runReindexLoop(context);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Logger.error(this, "ReindexThread Exception", e);
            }
        }
        Logger.warn(this.getClass(),
                "---  ReindexThread is stopping, background indexing will not take place");

    }

    @VisibleForTesting
    long totalESPuts() {
        return contentletsIndexed.get();
    }


    private synchronized void closeBulkProcessor(final BulkProcessorContext context) {
        if (context.getBulkProcessor() != null) {
            try {
                boolean closed = context.getBulkProcessor().awaitClose(BULK_PROCESSOR_AWAIT_TIMEOUT, TimeUnit.SECONDS);
                if (!closed) {
                    Logger.warn(this.getClass(), "BulkProcessor did not close within the timeout period.");
                }
                boolean isDone = handleResults(context.getBulkProcessorListener());
                if (!isDone) {
                    Logger.warn(this.getClass(), "BulkProcessor did not finish processing all records.");
                }
            } catch (InterruptedException e) {
                Logger.error(this.getClass(), "Interrupted while waiting for BulkProcessor to close", e);
                Thread.currentThread().interrupt(); // Restore the interrupted status
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error closing BulkProcessor", e);
            }

            // Perform any cleanup if necessary for BulkProcessorListener
            context.setBulkProcessorListener(null);
            context.setBulkProcessor(null);
        }

        rebuildBulkIndexer.set(false);
    }

    private boolean handleResults(BulkProcessorListener bulkProcessorListener) {

        List<ReindexResult> results = new ArrayList<>();
        bulkProcessorListener.getQueue().drainTo(results,250);
        while (!results.isEmpty()) {
            int failureCount = 0;
            List<ReindexEntry> success = new ArrayList<>();
            for (ReindexResult result : results) {
                if (result.success) {
                    success.add(result.entry);
                } else {
                    failureCount++;
                    handleFailure(result.entry, result.error);
                }
            }
            handleSuccess(success);
            Logger.info(
                    this.getClass(),
                    "Completed "
                            + results.size()
                            + " reindex requests with "
                            + failureCount
                            + " failures");
            results.clear();
            bulkProcessorListener.getQueue().drainTo(results,250);
        }
        return bulkProcessorListener.getWorkingRecords().isEmpty();
    }
    private void handleSuccess(final List<ReindexEntry> successful) {

        try {
            if (!successful.isEmpty()) {
                APILocator.getReindexQueueAPI().deleteReindexEntry(successful);
                CacheLocator.getESQueryCache().clearCache();
            }
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), "unable to delete indexjournal:" + e.getMessage(), e);
        }
    }

    private void handleFailure(final ReindexEntry idx, final String cause) {
        try {
            APILocator.getReindexQueueAPI().markAsFailed(idx, cause);
        } catch (DotDataException e) {
            Logger.warnAndDebug(this.getClass(), "unable to reque indexjournal:" + idx, e);
        }
    }


    private BulkProcessorContext finalizeReIndex(BulkProcessorContext context) throws DotDataException {
        closeBulkProcessor(context);
        switchOverIfNeeded();
        if (!indexAPI.isInFullReindex()) {
            ReindexThread.pause();
        }
        return context;
    }


    /**
     * This method is constantly verifying the existence of records in the
     * {@code dist_reindex_journal} table. If a record is found, then it must be added to the
     * Elastic index. If that's not possible, a notification containing the content identifier will
     * be sent to the user via the Notifications API to take care of the problem as soon as
     * possible.
     */
    private void runReindexLoop(BulkProcessorContext context) throws InterruptedException {

        try {
            processReindexing(context);
        }
        catch (Exception ex) {
            Logger.error(this, "ReindexThread Exception", ex);
            closeBulkProcessor(context);
            context.setBulkProcessor(null);
            context.setBulkProcessorListener(null);
            ThreadUtils.sleep(sleepOnError);
        } finally {
            DbConnectionFactory.closeSilently();
        }

        while (paused.get()) {

            // Even when we pause we may still have results of in process requests we need to handle
            if (context.getBulkProcessorListener()!=null && !context.getBulkProcessorListener().getWorkingRecords().isEmpty()) {
                boolean gotAll = handleResults(context.getBulkProcessorListener());
                if (gotAll)
                {
                    Logger.info(this, "All running records processed, pausing");
                } else
                    // Don't wait too long to handle completed records
                    TimeUnit.MILLISECONDS.sleep(sleep<2000 ? sleep : 2000);
            } else
                TimeUnit.MILLISECONDS.sleep(sleep);

            Long restartTime = (Long) cache.get().get(REINDEX_THREAD_PAUSED);
            if (restartTime == null || restartTime < System.currentTimeMillis()) {
                unpauseImpl();
            }

        }
    }

    private void processReindexing(BulkProcessorContext context) throws DotDataException {
        final Map<String, ReindexEntry> workingRecords = queueApi.findContentToReindex();

        if (workingRecords.isEmpty()) {
            finalizeReIndex(context);
            return;
        }

        if (!isClusterReadOnly() && !workingRecords.isEmpty()) {
            try {
                processRecords(workingRecords, context);
            } catch (Exception e) {
                Logger.error(this, "Error processing records", e);
                closeBulkProcessor(context);
                throw e; // Rethrow to handle in runReindexLoop
            }
        }
        // Handle any results from the BulkProcessor
        if (context.getBulkProcessorListener()!=null)
            handleResults(context.getBulkProcessorListener());
    }

    private boolean switchOverIfNeeded() throws DotDataException {
        if (ESReindexationProcessStatus.inFullReindexation() && queueApi.recordsInQueue() == 0) {
            // The re-indexation process has finished successfully
            if (indexAPI.reindexSwitchover(false)) {
                // Generate and send an user notification
                sendNotification("notification.reindexing.success", null, null, false);
            }
            return true;
        }
        return false;
    }


    public static void startThread() {
        getInstance().unpauseInt();
    }

    public static void unpause()
    {
        getInstance().unpauseInt();
    }

    /**
     * Tells the thread to stop processing. Doesn't shut down the thread.
     */
    public static void stopThread() {
        getInstance().stop();
    }


    public static void pause() {
        getInstance().setPaused();
    }

    public static boolean isWorking() {
        return getInstance().isRunning();
    }

    private boolean isRunning() {
        return running.get();
    }

    private boolean isClusterReadOnly() {
        return ElasticReadOnlyCommand.getInstance().isIndexOrClusterReadOnly();
    }

    private void processRecords(Map<String, ReindexEntry> workingRecords, BulkProcessorContext context)
            throws DotDataException {
        Logger.debug(this, "Found " + workingRecords.size() + " index items to process");

        if (context.getBulkProcessor() == null || rebuildBulkIndexer.get()) {
            closeBulkProcessor(context);
            context.setBulkProcessorListener(new BulkProcessorListener());

            try {
                BulkProcessor newBulkProcessor = indexAPI.createBulkProcessor(context.getBulkProcessorListener());
                context.setBulkProcessor(newBulkProcessor);
                context.getBulkProcessorListener().addWorkingRecord(workingRecords);
                indexAPI.appendToBulkProcessor(newBulkProcessor, workingRecords.values());
                contentletsIndexed.addAndGet(context.getBulkProcessorListener().getContentletsIndexed());
            } catch (Exception e) {
                Logger.error(this, "Error creating or using new BulkProcessor", e);
                throw new DotDataException("Error creating or using new BulkProcessor", e);
            }
        } else {
            context.getBulkProcessorListener().workingRecords.putAll(workingRecords);
            indexAPI.appendToBulkProcessor(context.getBulkProcessor(), workingRecords.values());
            contentletsIndexed.addAndGet(context.getBulkProcessorListener().getContentletsIndexed());
        }
    }

    /**
     * Generates a new notification displayed at the top left side of the back-end page in dotCMS.
     * This utility method allows you to send reports to the user regarding the operations performed
     * during the re-index, whether they succeeded or failed.
     *
     * @param key        - The message key that should be present in the language properties files.
     * @param msgParams  - The parameters, if any, that will replace potential placeholders in the
     *                   message. E.g.: "This is {0} test."
     * @param defaultMsg - If set, the default message in case the key does not exist in the
     *                   properties file. Otherwise, the message key will be returned.
     * @param error      - true if we want to send an error notification
     * @throws DotDataException  The notification could not be posted to the system.
     * @throws DotDataException The language properties could not be retrieved.
     */
    protected void sendNotification(final String key, final Object[] msgParams,
            final String defaultMsg, boolean error)
            throws DotDataException {

        NotificationLevel notificationLevel =
                error ? NotificationLevel.ERROR : NotificationLevel.INFO;

        // Search for the CMS Admin role and System User
        final Role cmsAdminRole = this.roleAPI.loadCMSAdminRole();
        final User systemUser = this.userAPI.getSystemUser();

        this.notificationAPI.generateNotification(
                new I18NMessage("notification.reindex.error.title"), // title = Reindex Notification
                new I18NMessage(key, defaultMsg, msgParams), null, // no actions
                notificationLevel, NotificationType.GENERIC, Visibility.ROLE, cmsAdminRole.getId(),
                systemUser.getUserId(),
                systemUser.getLocale());
    }

    private static class BulkProcessorContext {
        private BulkProcessor bulkProcessor;
        private BulkProcessorListener bulkProcessorListener;

        public BulkProcessorContext(BulkProcessor bulkProcessor, BulkProcessorListener bulkProcessorListener) {
            this.bulkProcessor = bulkProcessor;
            this.bulkProcessorListener = bulkProcessorListener;
        }

        public BulkProcessor getBulkProcessor() {
            return bulkProcessor;
        }

        public void setBulkProcessor(BulkProcessor bulkProcessor) {
            this.bulkProcessor = bulkProcessor;
        }

        public BulkProcessorListener getBulkProcessorListener() {
            return bulkProcessorListener;
        }

        public void setBulkProcessorListener(BulkProcessorListener bulkProcessorListener) {
            this.bulkProcessorListener = bulkProcessorListener;
        }
    }
}