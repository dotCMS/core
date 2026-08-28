package com.dotmarketing.common.reindex;

import static com.dotcms.shutdown.ShutdownCoordinator.isShutdownRelated;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.SystemCache;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ReindexPoolExhaustedException;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.shutdown.ShutdownCoordinator;
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
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.felix.framework.OSGISystem;

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
 * <h2>Worker lifecycle</h2>
 * <p>
 * Exactly one worker runnable may be executing per JVM. Two independent pieces of state describe
 * it, and conflating them caused the defects fixed in issue #36922:
 * </p>
 * <ul>
 * <li>{@code state} ({@link ThreadState}) is a <em>command</em> channel — what the system wants the
 * worker to do. {@code STOPPED} means "not running, restartable"; {@code SHUTDOWN} is terminal and
 * must never be restarted; {@code PAUSED} and {@code RUNNING} both imply a live worker.</li>
 * <li>{@code workerAlive} is a <em>fact</em> — whether a runnable is actually executing. It is
 * claimed by compare-and-set before the runnable is submitted and cleared by the runnable in a
 * {@code finally} that also covers {@link Error}. Liveness must never be inferred from
 * {@code state}: a runnable that died leaves {@code state} untouched.</li>
 * </ul>
 * <p>
 * The shutdown signal is {@link com.dotcms.shutdown.ShutdownCoordinator#isShutdownStarted()}, a
 * monotonic latch — <strong>not</strong>
 * {@link com.dotcms.shutdown.ShutdownCoordinator#isRequestDraining()}, which is only true during
 * shutdown Phase 1 (bounded by {@code shutdown.request.drain.timeout.seconds}, default 15 s) and is
 * cleared before this component's shutdown task even runs. {@code isRequestDraining()} remains a
 * "do not start expensive work" hint in {@link #finalizeReIndex()} and
 * {@link #switchOverIfNeeded()} only.
 * </p>
 * <p>
 * Every wait in this class goes through {@link #waitFor(long)} rather than
 * {@code ThreadUtils.sleep}, which silently swallows {@code InterruptedException} <em>and</em> the
 * interrupt flag, making a parked worker un-interruptible.
 * </p>
 * <p>
 * Recovery is node-local. In a cluster each node runs its own worker against journal rows claimed
 * by {@code ConfigUtils.getServerId()}; one node restarting its worker neither affects nor repairs
 * another's.
 * </p>
 *
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 */
@IndexLibraryIndependent
public class ReindexThread {

    private enum ThreadState {
        /** Never started, or explicitly stopped. Restartable via {@link #unpauseImpl()}. */
        STOPPED,
        /** Alive but parked: the queue drained, or a full reindex is bracketing the worker. */
        PAUSED,
        /** Alive and draining the queue. */
        RUNNING,
        /**
         * The JVM is shutting down. Terminal: the worker exits and must NOT be restarted.
         *
         * <p>Distinct from {@link #STOPPED} on purpose. {@code STOPPED} is the value
         * {@link #unpauseImpl()} keys off to re-submit the runnable, so reusing it for shutdown
         * would let a late commit listener resurrect the worker mid-shutdown (issue #36922).</p>
         */
        SHUTDOWN;
    }

    private final ContentletIndexAPI indexAPI;
    private final ReindexQueueAPI queueApi;
    private final NotificationAPI notificationAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;

    private static volatile ReindexThread instance;

    private final long SLEEP = Config.getLongProperty("REINDEX_THREAD_SLEEP", 250);
    private final int SLEEP_ON_ERROR = Config.getIntProperty("REINDEX_THREAD_SLEEP_ON_ERROR", 500);

    /**
     * Back-off while the mapping guard is refusing work because its storage is unreachable. Much
     * longer than {@link #SLEEP_ON_ERROR}: nothing is being failed or retried, we are only waiting
     * for the underlying dependency to answer again.
     */
    private final int SLEEP_WHEN_DEGRADED =
            Config.getIntProperty("REINDEX_THREAD_SLEEP_WHEN_DEGRADED", 30000);
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
    public static final int BULK_PROCESSOR_AWAIT_TIMEOUT = Config.getIntProperty(
            "BULK_PROCESSOR_AWAIT_TIMEOUT", 20);

    public static final int BACKOFF_POLICY_TIME_IN_SECONDS = Config.getIntProperty(
            "BACKOFF_POLICY_TIME_IN_SECONDS", 20);

    public static final int BACKOFF_POLICY_MAX_RETRYS = Config.getIntProperty(
            "BACKOFF_POLICY_MAX_RETRYS", 10);


    public static final int WAIT_BEFORE_PAUSE_SECONDS = Config.getIntProperty(
            "WAIT_BEFORE_PAUSE_SECONDS", 0);


    private final AtomicReference<ThreadState> state = new AtomicReference<>(ThreadState.STOPPED);

    /**
     * Whether a {@link #ReindexThreadRunnable} is actually executing right now.
     *
     * <p>Deliberately <em>separate</em> from {@link #state}. {@code state} is a command channel —
     * what the system wants the worker to do; this flag is a fact — whether a worker exists to obey
     * it. Bug 2 in issue #36922 was {@link #unpauseImpl()} inferring the second from the first: a
     * runnable that had died left {@code state} at {@code PAUSED}, so unpausing flipped a flag
     * nobody was reading and the queue silently never drained.</p>
     *
     * <p>Claimed by compare-and-set <strong>before</strong> the runnable is submitted (so two
     * concurrent callers cannot both start one) and cleared by the runnable in a {@code finally}
     * that also covers {@link Error}.</p>
     */
    private final AtomicBoolean workerAlive = new AtomicBoolean(false);

    /**
     * Single constant for the shutdown notice, used by every site that can observe shutdown, so
     * the "at most once per shutdown" guarantee holds across all of them.
     */
    private static final String SHUTDOWN_DETECTED_MSG =
            "Shutdown detected, stopping reindex operations";

    /**
     * Throttle key for the dead-worker recovery notice. A compile-time constant on purpose: the
     * key set {@code Logger}'s static throttle map can grow to must stay bounded, so a repeating
     * failure cannot turn logging into a memory-exhaustion vector (AC-019).
     */
    private static final String DEAD_WORKER_MESSAGE_KEY = "reindex-thread-dead-worker-restarted";

    private static final int DEAD_WORKER_LOG_INTERVAL_MS = 60000;


    private final static String REINDEX_THREAD_PAUSED = "REINDEX_THREAD_PAUSED";
    private final static Lazy<SystemCache> cache = Lazy.of(() -> CacheLocator.getSystemCache());

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
        try {
            while (!isTerminal()) {
                try {
                    runReindexLoop();
                } catch (Exception e) {
                    // Recoverable: log and let the loop retry.
                    Logger.error(this.getClass(), e.getMessage(), e);
                }
            }
        } catch (Throwable e) {
            // A JVM-level Error (OutOfMemoryError, LinkageError) must not be retried in a tight
            // loop — retrying is futile and can deepen the failure. It terminates the worker, and
            // the finally below clears liveness so the next unpause restarts it instead of the
            // node stalling silently forever (issue #36922, Bug 2).
            Logger.error(this.getClass(),
                    "ReindexThread terminating on unrecoverable error: " + e.getMessage(), e);
        } finally {
            // Invariant I2: cleared on EVERY exit path — normal, Exception, Error, interrupt,
            // executor shutdown. This is what makes a dead worker detectable.
            workerAlive.set(false);
            Logger.warn(this.getClass(),
                    "---  ReindexThread is stopping, background indexing will not take place");
        }
    };

    @VisibleForTesting
    long totalESPuts() {
        return contentletsIndexed;
    }

    /**
     * {@code true} when the worker must not keep running: either explicitly stopped or shut down.
     * Every loop in this class tests this rather than {@code == STOPPED}, so the terminal
     * {@link ThreadState#SHUTDOWN} genuinely ends the outer loop in {@link #ReindexThreadRunnable}
     * instead of letting it re-enter {@link #runReindexLoop()} (issue #36922, Bug 1).
     */
    /**
     * Interrupt-aware wait, used instead of {@code ThreadUtils.sleep} on this class's own wait
     * paths.
     *
     * <p>{@code ThreadUtils.sleep} is
     * {@code Try.run(() -> Thread.sleep(t)).onFailure(DotRuntimeException::new)}. {@code onFailure}
     * takes a {@code Consumer<Throwable>}, so that method reference only <em>constructs</em> an
     * exception and drops it: the {@code InterruptedException} is never rethrown or logged, and the
     * interrupt flag the JVM cleared when {@code Thread.sleep} threw is never restored. A worker
     * parked in it cannot be interrupted at all, which is why {@code shutdownNow()} from
     * {@code ReindexThreadShutdownTask} has no effect today.</p>
     *
     * <p>{@code ThreadUtils.sleep} itself is deliberately left alone — it has many callers across
     * the legacy codebase and changing its contract is out of scope for this fix.</p>
     *
     * @return {@code false} if the wait was interrupted, in which case the caller must stop working
     */
    private boolean waitFor(final long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (final InterruptedException e) {
            // Restore what Thread.sleep cleared, so the executor and anything up the stack can
            // still observe that this thread was interrupted.
            Thread.currentThread().interrupt();
            Logger.debug(this, "ReindexThread interrupted; terminating the worker");
            return false;
        }
    }

    private boolean isTerminal() {
        final ThreadState current = state.get();
        return current == ThreadState.STOPPED || current == ThreadState.SHUTDOWN;
    }

    /**
     * Checks for shutdown and, on the first observation, transitions to the terminal state and logs
     * once.
     *
     * <p>Uses {@link ShutdownCoordinator#isShutdownStarted()} — a monotonic latch — rather than
     * {@link ShutdownCoordinator#isRequestDraining()}, which is only true during shutdown Phase 1
     * (bounded by {@code shutdown.request.drain.timeout.seconds}, default 15 s) and is cleared
     * before the reindex shutdown task even runs. Keying the terminal decision off the transient
     * flag is what produced the hot-loop, and what let the worker resume indexing once Phase 1
     * ended. {@code isRequestDraining()} is still honored in {@link #finalizeReIndex()} and
     * {@link #switchOverIfNeeded()} as a "do not start expensive work" hint.</p>
     *
     * <p>{@code getAndSet} makes the log emission win-once: only the thread that actually performs
     * the transition writes the line, so the message appears at most once per shutdown at every
     * log level — not once per throttle window.</p>
     *
     * @return {@code true} if shutdown has started and the caller should stop working
     */
    private boolean shutdownRequested() {
        if (!ShutdownCoordinator.isShutdownStarted()) {
            return false;
        }
        if (state.getAndSet(ThreadState.SHUTDOWN) != ThreadState.SHUTDOWN) {
            Logger.info(this, SHUTDOWN_DETECTED_MSG);
        }
        return true;
    }


    /**
     * Handles queue-drained logic: attempts index switchover and pauses the thread
     * if no full reindex is in progress.
     */
    private void finalizeReIndex()
            throws InterruptedException, LanguageException, DotDataException, SQLException {
        // Don't perform switchover operations during shutdown
        if (!ShutdownCoordinator.isRequestDraining()) {
            switchOverIfNeeded();
            if (!indexAPI.isInFullReindex()) {
                ReindexThread.pause();
            }
        } else {
            Logger.debug(this, "Skipping reindex finalization due to shutdown in progress");
        }
    }


    /**
     * This method is constantly verifying the existence of records in the
     * {@code dist_reindex_journal} table. If a record is found, then it must be added to the
     * Elastic index. If that's not possible, a notification containing the content identifier will
     * be sent to the user via the Notifications API to take care of the problem as soon as
     * possible.
     *
     * <p><strong>Thread-safety:</strong> each batch gets its own {@link BulkProcessorListener}
     * and {@link IndexBulkProcessor}. The processor is closed (blocking until {@code afterBulk}
     * completes) before the next batch starts, so there is no shared mutable state between
     * consecutive batches and no TOCTOU race on the processor reference.</p>
     */
    private void runReindexLoop() {
        while (!isTerminal()) {
            try {
                if (shutdownRequested()) {
                    break;
                }

                final Map<String, ReindexEntry> workingRecords = queueApi.findContentToReindex();

                if (workingRecords.isEmpty()) {
                    finalizeReIndex();
                } else {
                    if (shutdownRequested()) {
                        break;
                    }

                    Logger.debug(this, "Found  " + workingRecords + " index items to process");

                    // Fresh listener per batch: each afterBulk callback resolves against its own
                    // immutable workingRecords snapshot — no race with the next putAll.
                    final BulkProcessorListener batchListener = new BulkProcessorListener();
                    batchListener.workingRecords.putAll(workingRecords);
                    try (final IndexBulkProcessor batchProcessor =
                            indexAPI.createBulkProcessor(batchListener)) {
                        indexAPI.appendToBulkProcessor(batchProcessor, workingRecords.values());
                    } // close() blocks until afterBulk completes before the next batch starts
                    contentletsIndexed += batchListener.getContentletsIndexed();
                }

            } catch (Throwable ex) {
                if (isShutdownRelated(ex) || ShutdownCoordinator.isRequestDraining()
                        || ex instanceof com.dotcms.shutdown.ShutdownException) {
                    Logger.debug(this, "ReindexThread stopping due to shutdown: " + ex.getMessage());
                    break;
                }
                if (ex instanceof Error) {
                    // Let JVM-level errors propagate to the runnable, which logs, terminates and
                    // clears liveness. Swallowing them here would spin this loop at
                    // SLEEP_ON_ERROR forever against a JVM that cannot recover.
                    throw (Error) ex;
                }
                if (ex instanceof ReindexPoolExhaustedException) {
                    // The mapping guard refused the batch because the storage it needs is not
                    // answering. No journal entry was failed, so the work is still queued — back
                    // off hard rather than spinning through the journal at SLEEP_ON_ERROR speed
                    // (issue #37038). The guard's own counters recover on their own, so indexing
                    // resumes here without a restart.
                    Logger.errorEvery(ReindexThread.class, "reindex-thread-pool-degraded",
                            "--- ReindexThread is backing off: " + ex.getMessage(),
                            SLEEP_WHEN_DEGRADED);
                    if (!waitFor(SLEEP_WHEN_DEGRADED)) {
                        requestStop();
                        break;
                    }
                } else {
                    Logger.error(this, "ReindexThread Exception", ex);
                    if (!waitFor(SLEEP_ON_ERROR)) {
                        requestStop();
                        break;
                    }
                }
            } finally {
                DbConnectionFactory.closeSilently();
            }
            sleep();
        }
    }
    
    

    private void sleep() {
        while (state.get() == ThreadState.PAUSED) {
            // A parked worker must not wake itself into RUNNING while the JVM is shutting down:
            // the pause marker is normally absent/expired, which is exactly the "resume" case.
            if (shutdownRequested()) {
                return;
            }
            if (!waitFor(SLEEP)) {
                requestStop();
                return;
            }
            //Logs every 60 minutes
            Logger.infoEvery(ReindexThread.class, "--- ReindexThread Paused",
                    Config.getIntProperty("REINDEX_THREAD_PAUSE_IN_MINUTES", 60) * 60000);
            Long restartTime = (Long) cache.get().get(REINDEX_THREAD_PAUSED);
            if (restartTime == null || restartTime < System.currentTimeMillis()) {
                state.compareAndSet(ThreadState.PAUSED, ThreadState.RUNNING);
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

    private void state(final ThreadState state) {
        getInstance().state.set(state);
    }

    /**
     * Moves the worker to the restartable {@link ThreadState#STOPPED} state, but never downgrades
     * the terminal {@link ThreadState#SHUTDOWN}.
     *
     * <p>{@code ReindexThreadShutdownTask} calls {@link #stopThread()} during shutdown. Without this
     * guard that call would rewrite {@code SHUTDOWN} to {@code STOPPED} — the one state
     * {@link #unpauseImpl()} treats as "safe to re-submit" — handing a late commit listener a way
     * to restart the worker while the JVM is tearing down.</p>
     */
    private void requestStop() {
        ThreadState current;
        do {
            current = state.get();
            if (current == ThreadState.SHUTDOWN) {
                return;
            }
        } while (!state.compareAndSet(current, ThreadState.STOPPED));
    }

    /**
     * Tells the thread to stop processing. Doesn't shut down the thread.
     */
    public static void stopThread() {
        Logger.info(ReindexThread.class, "Stopping ReindexThread...");
        getInstance().requestStop();
        
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
        // Correct double-checked locking: `instance` is volatile and the guarded branch assigns a
        // local rather than returning `new ReindexThread().instance`. The previous form relied on
        // the constructor's `instance = this` side effect through a NON-volatile field, which can
        // publish a partially constructed object — including a null workerAlive flag.
        ReindexThread result = instance;
        if (result == null) {
            synchronized (ReindexThread.class) {
                result = instance;
                if (result == null) {
                    result = new ReindexThread();
                    instance = result;
                }
            }
        }
        return result;
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

    /**
     * The single place a {@link #ReindexThreadRunnable} is submitted, shared by the cold-start
     * ({@code STOPPED}) and dead-worker-recovery ({@code PAUSED} with no live runnable) paths.
     *
     * <p>Order matters: shutdown is checked first (transition T9), then liveness is <em>claimed</em>
     * by compare-and-set <strong>before</strong> submitting. Claiming first is what guarantees a
     * single worker — two concurrent callers cannot both observe "dead" and both submit — and it
     * also makes the pool's {@code DiscardOldestPolicy} unreachable for this task, since at most
     * one submission is ever in flight.</p>
     *
     * <p>If the submit throws after the claim succeeds, the claim is rolled back (invariant I3). A
     * stranded {@code true} would make every future unpause believe a worker exists and block
     * recovery permanently — a worse failure than the bug this fixes.</p>
     *
     * @param bootstrapOsgi {@code true} only for a cold start. Dead-worker recovery must not
     *        re-bootstrap the OSGI framework: the process is already up, and only the worker died.
     * @return {@code true} if this call started a worker
     */
    private static boolean startWorker(final boolean bootstrapOsgi) {

        if (ShutdownCoordinator.isShutdownStarted()) {
            Logger.debug(ReindexThread.class,
                    "Not starting the ReindexThread worker: shutdown is in progress");
            return false;
        }

        final ReindexThread inst = getInstance();

        // Invariant I1: claim before submit. The loser of the race simply returns.
        if (!inst.workerAlive.compareAndSet(false, true)) {
            return false;
        }

        try {
            if (bootstrapOsgi) {
                OSGISystem.getInstance().initializeFramework();
            }
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
            inst.state(ThreadState.RUNNING);
            submitter.submit(inst.ReindexThreadRunnable);
            return true;
        } catch (Throwable t) {
            // Invariant I3: never leave the flag claimed with no worker behind it.
            inst.workerAlive.set(false);
            Logger.error(ReindexThread.class,
                    "Failed to start the ReindexThread worker: " + t.getMessage(), t);
            return false;
        }
    }

    private static void unpauseImpl() {

        // Transition T9: never bring the worker back while the JVM is shutting down. Commit
        // listeners can fire late in the shutdown sequence, and a restart here would resurrect the
        // worker against infrastructure that is already being torn down.
        if (ShutdownCoordinator.isShutdownStarted()) {
            Logger.debug(ReindexThread.class,
                    "Ignoring unpause request: shutdown is in progress");
            return;
        }

        final ReindexThread inst = getInstance();
        final ThreadState state = inst.state.get();

        if (state == ThreadState.PAUSED) {
            if (inst.workerAlive.get()) {
                // Healthy path: a live worker is parked in sleep(); a flag flip is all it needs.
                Logger.info(ReindexThread.class, "--- Unpausing reindex thread ");
                cache.get().remove(REINDEX_THREAD_PAUSED);
                inst.state(ThreadState.RUNNING);
            } else {
                // Issue #36922, Bug 2: PAUSED but nothing is alive to act on it. Previously this
                // logged a cheerful "Unpausing" and returned, and the queue never drained. Report
                // it at ERROR so it is alertable, then actually restart the worker.
                Logger.errorEvery(ReindexThread.class, DEAD_WORKER_MESSAGE_KEY,
                        "--- ReindexThread was PAUSED but no worker was alive; restarting it. "
                                + "Content queued for indexing on this node would otherwise never "
                                + "be indexed.", DEAD_WORKER_LOG_INTERVAL_MS);
                startWorker(false);
            }
        } else if (state == ThreadState.STOPPED) {
            Logger.info(ReindexThread.class, "--- Recreating ReindexThread from stopped");
            Logger.infoEvery(ReindexThread.class, "--- ReindexThread Running", 60000);
            startWorker(true);
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
