package com.dotmarketing.common.reindex;

import static com.dotcms.shutdown.ShutdownCoordinator.isShutdownRelated;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.SystemCache;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.shutdown.ShutdownCoordinator;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.*;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import org.apache.felix.framework.OSGISystem;
import org.elasticsearch.action.bulk.BulkProcessor;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private enum ThreadState {
        STOPPED, PAUSED, RUNNING;
    }

    private final ContentletIndexAPI indexAPI;
    private final ReindexQueueAPI queueApi;
    private final NotificationAPI notificationAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;

    private static ReindexThread instance;

    private final long SLEEP = Config.getLongProperty("REINDEX_THREAD_SLEEP", 250);
    private final int SLEEP_ON_ERROR = Config.getIntProperty("REINDEX_THREAD_SLEEP_ON_ERROR", 500);
    private long contentletsIndexed = 0;
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


    private AtomicReference<ThreadState> state = new AtomicReference<>(ThreadState.STOPPED);


    private final static String REINDEX_THREAD_PAUSED = "REINDEX_THREAD_PAUSED";
    private final static Lazy<SystemCache> cache = Lazy.of(() -> CacheLocator.getSystemCache());

    private final static AtomicBoolean rebuildBulkIndexer = new AtomicBoolean(false);

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
        instance = this;

    }


    private final Runnable ReindexThreadRunnable = () -> {
        Logger.info(this.getClass(),
                "---  ReindexThread is starting, background indexing will begin");

        while (state.get() != ThreadState.STOPPED) {
            try {
                runReindexLoop();
            } catch (Exception e) {
                Logger.error(this.getClass(), e.getMessage(), e);
            }
        }
        Logger.warn(this.getClass(),
                "---  ReindexThread is stopping, background indexing will not take place");

    };

    @VisibleForTesting
    long totalESPuts() {
        return contentletsIndexed;
    }


    private BulkProcessor closeBulkProcessor(final BulkProcessor bulkProcessor)
            throws InterruptedException {
        if (bulkProcessor != null) {
            bulkProcessor.awaitClose(BULK_PROCESSOR_AWAIT_TIMEOUT, TimeUnit.SECONDS);
        }
        rebuildBulkIndexer.set(false);
        return null;
    }


    private BulkProcessor finalizeReIndex(BulkProcessor bulkProcessor)
            throws InterruptedException, LanguageException, DotDataException, SQLException {
        bulkProcessor = closeBulkProcessor(bulkProcessor);
        
        // Don't perform switchover operations during shutdown
        if (!ShutdownCoordinator.isRequestDraining()) {
            switchOverIfNeeded();
            if (!indexAPI.isInFullReindex()) {
                ReindexThread.pause();
            }
        } else {
            Logger.debug(this, "Skipping reindex finalization due to shutdown in progress");
        }
        return bulkProcessor;

    }


    /**
     * This method is constantly verifying the existence of records in the
     * {@code dist_reindex_journal} table. If a record is found, then it must be added to the
     * Elastic index. If that's not possible, a notification containing the content identifier will
     * be sent to the user via the Notifications API to take care of the problem as soon as
     * possible.
     */
    private void runReindexLoop() {
        BulkProcessor bulkProcessor = null;
        BulkProcessorListener bulkProcessorListener = null;
        while (state.get() != ThreadState.STOPPED) {
            try {
                // Check for shutdown before doing any database operations
                if (ShutdownCoordinator.isRequestDraining()) {
                    Logger.info(this, "Shutdown detected, stopping reindex operations");
                    break;
                }

                final Map<String, ReindexEntry> workingRecords = queueApi.findContentToReindex();

                if (workingRecords.isEmpty()) {
                    bulkProcessor = finalizeReIndex(bulkProcessor);
                }

                if (!workingRecords.isEmpty()) {
                    // Check again before processing records
                    if (ShutdownCoordinator.isRequestDraining()) {
                        Logger.info(this, "Shutdown detected during record processing, stopping reindex operations");
                        break;
                    }

                    Logger.debug(this,
                            "Found  " + workingRecords + " index items to process");

                    if (bulkProcessor == null || rebuildBulkIndexer.get()) {
                        closeBulkProcessor(bulkProcessor);
                        bulkProcessorListener = new BulkProcessorListener();
                        bulkProcessor = indexAPI.createBulkProcessor(bulkProcessorListener);
                    }
                    bulkProcessorListener.workingRecords.putAll(workingRecords);
                    indexAPI.appendToBulkProcessor(bulkProcessor, workingRecords.values());
                    contentletsIndexed += bulkProcessorListener.getContentletsIndexed();
                    // otherwise, reindex normally

                }
            } catch (Throwable ex) {
                // Check if this is a shutdown-related exception
                if (isShutdownRelated(ex) || ShutdownCoordinator.isRequestDraining() || 
                    ex instanceof com.dotcms.shutdown.ShutdownException) {
                    Logger.debug(this, "ReindexThread stopping due to shutdown: " + ex.getMessage());
                    break;
                }
                Logger.error(this, "ReindexThread Exception", ex);
                ThreadUtils.sleep(SLEEP_ON_ERROR);
            } finally {
                DbConnectionFactory.closeSilently();
            }
            sleep();
        }
        
        // Clean up bulk processor on exit
        try {
            if (bulkProcessor != null) {
                closeBulkProcessor(bulkProcessor);
            }
        } catch (Exception e) {
            Logger.debug(this, "Exception while closing bulk processor during shutdown: " + e.getMessage());
        }
    }
    
    

    private void sleep() {
        while (state.get() == ThreadState.PAUSED) {
            ThreadUtils.sleep(SLEEP);
            //Logs every 60 minutes
            Logger.infoEvery(ReindexThread.class, "--- ReindexThread Paused",
                    Config.getIntProperty("REINDEX_THREAD_PAUSE_IN_MINUTES", 60) * 60000);
            Long restartTime = (Long) cache.get().get(REINDEX_THREAD_PAUSED);
            if (restartTime == null || restartTime < System.currentTimeMillis()) {
                state.set(ThreadState.RUNNING);
            }
        }
    }


    private boolean switchOverIfNeeded()
            throws LanguageException, DotDataException, SQLException, InterruptedException {
        // Skip switchover operations during shutdown
        if (ShutdownCoordinator.isRequestDraining()) {
            Logger.debug(this, "Skipping reindex switchover due to shutdown in progress");
            return false;
        }
        
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

    /**
     * Tells the thread to start processing. Starts the thread
     */
    public static void startThread() {
        unpause();
    }

    private void state(ThreadState state) {
        getInstance().state.set(state);
    }

    /**
     * Tells the thread to stop processing. Doesn't shut down the thread.
     */
    public static void stopThread() {
        Logger.info(ReindexThread.class, "Stopping ReindexThread...");
        getInstance().state(ThreadState.STOPPED);
        
        // Give the thread a moment to notice the state change and exit gracefully
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Logger.info(ReindexThread.class, "ReindexThread stopped");
    }


    /**
     * This instance is intended to already be started. It will try to restart the thread if
     * instance is null.
     */
    public static ReindexThread getInstance() {
        if (instance == null) {
            synchronized (ReindexThread.class) {
                if (instance == null) {
                    return new ReindexThread().instance;
                }
            }

        }
        return instance;
    }

    public static void pause() {
        Logger.debug(ReindexThread.class, "--- ReindexThread - Paused");
        cache.get().put(REINDEX_THREAD_PAUSED, System.currentTimeMillis() + Duration
                .ofMinutes(Config.getIntProperty("REINDEX_THREAD_PAUSE_IN_MINUTES", 10))
                .toMillis());
        getInstance().state(ThreadState.PAUSED);
    }

    public static void unpause() {
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            Logger.debug(ReindexThread.class, "--- Adding unpause commit listener");
            HibernateUtil.addCommitListener("unpauseIndex", ReindexThread::unpauseImpl);
        } else {
            unpauseImpl();
        }
    }

    private static void unpauseImpl() {

        ThreadState state = getInstance().state.get();
        if (state == ThreadState.PAUSED) {
            Logger.info(ReindexThread.class, "--- Unpausing reindex thread ");
            cache.get().remove(REINDEX_THREAD_PAUSED);
            getInstance().state(ThreadState.RUNNING);
        } else if (state == ThreadState.STOPPED) {
            Logger.info(ReindexThread.class, "--- Recreating ReindexThread from stopped");
            OSGISystem.getInstance().initializeFramework();
            Logger.infoEvery(ReindexThread.class, "--- ReindexThread Running", 60000);
            cache.get().remove(REINDEX_THREAD_PAUSED);

            final DotSubmitter submitter = DotConcurrentFactory.getInstance()
                    .getSubmitter("ReindexThreadSubmitter",
                            new DotConcurrentFactory.SubmitterConfigBuilder()
                                    .poolSize(1)
                                    .maxPoolSize(1)
                                    .queueCapacity(2)
                                    .rejectedExecutionHandler(
                                            new ThreadPoolExecutor.DiscardOldestPolicy())
                                    .build()
                    );
            getInstance().state(ThreadState.RUNNING);
            submitter.submit(getInstance().ReindexThreadRunnable);
        }

    }

    public static boolean isWorking() {
        return getInstance().state.get() == ThreadState.RUNNING;
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
     * @throws LanguageException The language properties could not be retrieved.
     */
    protected void sendNotification(final String key, final Object[] msgParams,
            final String defaultMsg, boolean error)
            throws DotDataException, LanguageException {

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
}
