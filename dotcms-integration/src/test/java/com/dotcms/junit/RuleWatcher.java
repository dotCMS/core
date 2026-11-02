package com.dotcms.junit;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.common.reindex.ReindexQueueFactory;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static com.dotcms.contenttype.business.ContentTypeAPIImpl.DELETE_CONTENT_TYPE_ASYNC;
import static com.dotmarketing.util.Config.USE_CONFIG_TEST_OVERRIDE_TRACKER;

public class RuleWatcher extends TestWatcher {

    public static final String USE_TEST_TYPE_TRACKER = "USE_TEST_TYPE_TRACKER";
    public static final String USE_TEST_INDEXER_TRACKER = "USE_TEST_INDEXER_TRACKER";
    public static final String USE_TEST_TRANSACTION_TRACKER = "USE_TEST_TRANSACTION_TRACKER";
    public static final String TEST_TRANSACTION_TRACKER_AUTO_CLOSE_TYPE = "TEST_TRANSACTION_TRACKER_AUTO_CLOSE_TYPE";
    public static final String TRANSACTION_AUTO_CLOSE_DEFAULT = "rollback";
    /**
     * How long to wait for the reindex queue to drain after a test before pausing the reindex
     * thread and deleting what is left. Longer than {@code DEFAULT_INDEX_QUEUE_WAIT} because a
     * slow-draining test would otherwise leave the thread paused for the tests that follow; it
     * costs nothing when the queue drains quickly.
     */
    private static final int INDEXER_DRAIN_TIMEOUT_SECONDS = 60;
    private Map<Integer, Map<String, String>> overrides = new ConcurrentHashMap<>();

    private Set<String> contentTypes = new HashSet<>();

    @Override
    protected void starting(Description description) {
        Logger.info(RuleWatcher.class, ">>>>>>>>>>>>>>>>>>>>>>>>>>");
        Logger.info(RuleWatcher.class,
                String.format(">>> %s - %s",
                        description,
                        "starting..."));
        Logger.info(RuleWatcher.class, ">>>>>>>>>>>>>>>>>>>>>>>>>>");
        configOverrideTrackerBefore(description);
        configureContentTypeTracker(description);

    }

    private void configureContentTypeTracker(Description description) {
        if (Config.getBooleanProperty(USE_TEST_TYPE_TRACKER, false)) {
            try {
                contentTypes.clear();
                List<ContentType> types = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .findAll();
                types.stream().map(ContentType::variable).forEach(contentTypes::add);
            } catch (DotDataException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void finished(Description description) {
        Logger.info(RuleWatcher.class, ">>>>>>>>>cleanup>>>>>>>>>>");
        transactionTrackerAfter(description);
        configOverrideTrackerAfter(description);
        contentTypeTrackerAfter(description);
        indexerTrackerAfter();
        Logger.info(RuleWatcher.class,
                String.format(">>> %s - %s",
                        description,
                        "finished..."));
    }

    private void contentTypeTrackerAfter(Description description) {
        if (Config.getBooleanProperty(USE_TEST_TYPE_TRACKER, false)) {
            try {
                List<ContentType> types = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .findAll();
                if (!types.isEmpty()) {
                    Logger.info(RuleWatcher.class, "Checking for created content types");
                    HashSet<ContentType> createdTypes = types.stream()
                            .filter(c -> !contentTypes.contains(c.variable()))
                            .collect(Collectors.toCollection(HashSet::new));

                    boolean asyncDelete = Config.getBooleanProperty(DELETE_CONTENT_TYPE_ASYNC);
                    if (!createdTypes.isEmpty()) {
                        try {
                            Config.getBooleanProperty(DELETE_CONTENT_TYPE_ASYNC, false);

                            createdTypes.forEach(c -> {
                                try {
                                    Logger.info(RuleWatcher.class,
                                            "Deleting content type " + c.variable());
                                    APILocator.getContentTypeAPI(APILocator.systemUser(),
                                            false).delete(c);
                                } catch (Exception e) {
                                    Logger.error(this.getClass(),
                                            "Error deleting content type in cleanup "
                                                    + c.variable(),
                                            e);
                                }
                            });
                        } finally {
                            Config.setProperty(DELETE_CONTENT_TYPE_ASYNC, asyncDelete);
                        }
                    }
                }

                contentTypes.clear();
            } catch (DotDataException e) {
                Logger.error(this.getClass(), "Error deleting content types in cleanup", e);
            }
        }
    }

    //TODO: these should be moved to different classes they could be handled as their own Rules

    /**
     * After a test, waits for the reindex queue to drain so pending index work does not leak into
     * the next test. If it does not drain within {@link #INDEXER_DRAIN_TIMEOUT_SECONDS}, the
     * reindex thread is paused and the whole journal is cleared by
     * {@link #clearStuckJournal(String)}, so rows the worker can never take cannot make every
     * following test wait as well.
     */
    private static void indexerTrackerAfter() {
        if (Config.getBooleanProperty(USE_TEST_INDEXER_TRACKER, false)) {
            Logger.info(MainBaseSuite.class, "Checking indexer status...");
            try {
                ReindexQueueAPI queueAPI = APILocator.getReindexQueueAPI();
                if (queueAPI.areRecordsLeftToIndex()) {
                    Logger.info(MainBaseSuite.class,
                            "Indexer is not empty, waiting for it to finish");
                    if (!ReindexThread.isWorking()) {
                        Logger.info(MainBaseSuite.class, "Indexer was not running, unpausing now");
                        ReindexThread.unpause();
                    }
                    boolean queueEmpty =
                            TestDataUtils.waitForEmptyQueue(INDEXER_DRAIN_TIMEOUT_SECONDS);
                    Logger.info(MainBaseSuite.class, "Indexer Complete=" + queueEmpty);
                    if (!queueEmpty) {
                        // Read before pausing, or the state would always say PAUSED.
                        final String workerState = reindexWorkerState();
                        ReindexThread.pause();
                        clearStuckJournal(workerState);
                    }
                }
            } catch (DotDataException e) {
                throw new RuntimeException("Error accessing Index", e);
            }
        }
    }

    /**
     * Deletes every row of {@code dist_reindex_journal} after the queue failed to drain, and logs
     * how many rows were removed per priority and action, a sample identifier, the row id range,
     * the last recorded failure cause and the reindex worker's state, so a run shows what was
     * stuck and why. A worker state of RUNNING with a cursor at or above the rows' ids means rows
     * were handed out and never finished; PAUSED means nothing woke the worker; a worker that is
     * not alive means it died. All rows are removed, not only those at REINDEX priority or above
     * ({@code deleteReindexAndFailedRecords}): rows at NORMAL priority that the worker cannot take
     * would otherwise survive and stall every later test as well.
     *
     * @param workerState the reindex worker's state, read before the suite paused it
     * @throws DotDataException if the journal cannot be read or cleared
     */
    private static void clearStuckJournal(final String workerState) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        final List<Map<String, Object>> byPriority = dotConnect.setSQL(
                "select priority, dist_action, count(*) as total, min(ident_to_index) as sample,"
                        + " min(id) as min_id, max(id) as max_id, max(index_val) as last_error"
                        + " from dist_reindex_journal group by priority, dist_action order by priority")
                .loadObjectResults();
        dotConnect.setSQL("delete from dist_reindex_journal").loadResult();
        Logger.info(MainBaseSuite.class, "Cleaned stuck reindex journal, rows removed by "
                + "priority and action: " + byPriority + "; " + workerState);
    }

    /**
     * Describes the reindex worker for the stuck-journal log line: its state, whether a worker is
     * alive, and the journal cursor ({@code ReindexQueueFactory.lastIdIndexed}). These are
     * package-private in core, so they are read by reflection, and only on this failure path.
     *
     * @return a one-line description, or the reason it could not be read
     */
    private static String reindexWorkerState() {
        try {
            final ReindexThread thread = ReindexThread.getInstance();
            final java.lang.reflect.Method state =
                    ReindexThread.class.getDeclaredMethod("currentStateName");
            final java.lang.reflect.Method alive =
                    ReindexThread.class.getDeclaredMethod("isWorkerAlive");
            final java.lang.reflect.Field cursor =
                    ReindexQueueFactory.class.getDeclaredField("lastIdIndexed");
            state.setAccessible(true);
            alive.setAccessible(true);
            cursor.setAccessible(true);
            return "reindex worker state=" + state.invoke(thread) + ", alive=" + alive.invoke(thread)
                    + ", journal cursor=" + cursor.getLong(null);
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return "reindex worker state unavailable: " + e;
        }
    }

    private void configOverrideTrackerBefore(Description description) {
        if (Config.getBooleanProperty(USE_CONFIG_TEST_OVERRIDE_TRACKER, false)) {
            if (Logger.isDebugEnabled(RuleWatcher.class)) {
                Config.getOverrides().forEach((k, v) -> Logger.warn(this.getClass(),
                        () -> "Config overrides before test: " + k + " = " + v));
            }
            overrides.put(description.hashCode(), Config.getOverrides());
        }
    }

    private void configOverrideTrackerAfter(Description description) {
        if (Config.getBooleanProperty(USE_CONFIG_TEST_OVERRIDE_TRACKER, false)) {
            Logger.warn(RuleWatcher.class, "Checking for modified Config overrides...");
            Map<String, String> modifiedOverrides = Config.compareOverrides(
                    overrides.get(description.hashCode()));
            overrides.remove(description.hashCode());
            if (!modifiedOverrides.isEmpty()) {
                String mapContents = modifiedOverrides.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce((a, b) -> a + ", " + b).orElse("empty");
                Logger.warn(IntegrationTestBase.class,
                        "Modified Config overrides after:" + mapContents);
            }
        }
    }

    private static void transactionTrackerAfter(Description description) {
        if (Config.getBooleanProperty(USE_TEST_TRANSACTION_TRACKER, false)) {
            if (DbConnectionFactory.inTransaction()) {
                Logger.error(RuleWatcher.class,
                        "Test " + description + " has open transaction after");
                Object responseAction = Config.getStringProperty(
                        TEST_TRANSACTION_TRACKER_AUTO_CLOSE_TYPE, TRANSACTION_AUTO_CLOSE_DEFAULT);
                try {
                    if (Config.getStringProperty(TEST_TRANSACTION_TRACKER_AUTO_CLOSE_TYPE,
                            TRANSACTION_AUTO_CLOSE_DEFAULT).equals("commit")) {
                        HibernateUtil.commitTransaction();
                    } else {
                        HibernateUtil.rollbackTransaction();
                    }
                } catch (DotHibernateException e) {
                    Logger.error(RuleWatcher.class,
                            "Error force " + responseAction + " back transaction");
                }
            }

            if (DbConnectionFactory.connectionExists()) {
                Logger.warn(RuleWatcher.class,
                        "Test " + description + " has open connection after");
                DbConnectionFactory.closeSilently();
            }

        }
    }

}
