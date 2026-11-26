package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueFactory.ReindexAction;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the failed records of the reindex journal (#37270).
 *
 * <h2>What is at stake</h2>
 * <p>A journal row's {@code priority} is its band (ASAP 0, NORMAL 100, STRUCTURE 200, REINDEX 300)
 * plus one per failed attempt; a row whose retries ran out is parked at {@code ERROR (400)} plus
 * the priority of its last attempt. Before this change the failed count read {@code priority >=
 * 400} and the failed listing read {@code priority > 300}, so the number shown in Maintenance and
 * the list an administrator downloaded did not describe the same rows: a reindex row on its second
 * attempt was listed but not counted, a save on its second attempt was neither.</p>
 *
 * <p>Rows are inserted with explicit priorities under fresh identifiers, and every assertion is
 * scoped to those identifiers or measured as a delta, so rows left by other tests do not matter.
 * The reindex thread is paused so it does not claim them.</p>
 *
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
 *       -Dmaven.build.cache.enabled=false -Dit.test=FailedReindexRecordsTest
 * </pre>
 */
public class FailedReindexRecordsTest {

    /** One character over Jackson's default maximum string length (#37269). */
    private static final int OVERSIZED_LENGTH = 20_000_001;

    private static ReindexQueueAPI reindexQueueAPI;
    private static ContentletIndexAPI indexAPI;
    private static ContentType contentType;
    private static User systemUser;

    /**
     * Holds the journal rows that existed before the class ran. Retry, clear and the wide delete
     * act on the whole journal, so those rows are set aside first and put back at the end instead
     * of being requeued or deleted by these tests.
     */
    private static final String JOURNAL_BACKUP = "failed_reindex_records_test_journal";

    /** Every contentlet the class created, destroyed at the end. */
    private static final List<Contentlet> createdContentlets = new ArrayList<>();

    /** Identifier of every row a test inserted, removed after each test. */
    private final List<String> inserted = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        reindexQueueAPI = APILocator.getReindexQueueAPI();
        indexAPI = APILocator.getContentletIndexAPI();
        systemUser = APILocator.systemUser();
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
        ReindexThread.pause();
        setJournalAside();
        contentType = new ContentTypeDataGen()
                .field(new FieldDataGen().type(TextAreaField.class).velocityVarName("body").next())
                .nextPersisted();
    }

    @AfterClass
    public static void restore() throws Exception {
        try {
            for (final Contentlet contentlet : createdContentlets) {
                // Silently: one of them had its versions deleted by hand.
                ContentletDataGen.destroy(contentlet, true);
            }
            createdContentlets.clear();
            if (contentType != null) {
                ContentTypeDataGen.remove(contentType, true);
            }
            putJournalBack();
        } finally {
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            ReindexThread.unpause();
        }
    }

    /** Moves every existing journal row to a backup table and empties the journal. */
    private static void setJournalAside() throws Exception {
        new DotConnect().executeStatement("DROP TABLE IF EXISTS " + JOURNAL_BACKUP);
        new DotConnect().executeStatement(
                "CREATE TABLE " + JOURNAL_BACKUP + " AS SELECT * FROM dist_reindex_journal");
        new DotConnect().executeStatement("DELETE FROM dist_reindex_journal");
    }

    /** Replaces what the tests left in the journal with the rows set aside before the class. */
    private static void putJournalBack() throws Exception {
        new DotConnect().executeStatement("DELETE FROM dist_reindex_journal");
        new DotConnect().executeStatement(
                "INSERT INTO dist_reindex_journal SELECT * FROM " + JOURNAL_BACKUP);
        new DotConnect().executeStatement("DROP TABLE " + JOURNAL_BACKUP);
    }

    @After
    public void removeInsertedRows() throws DotDataException {
        for (final String identifier : inserted) {
            new DotConnect().setSQL("DELETE FROM dist_reindex_journal WHERE ident_to_index = ?")
                    .addParam(identifier).loadResult();
        }
        inserted.clear();
    }

    /**
     * Given Scenario: Rows at every fresh band value (0, 100, 200, 300), rows still being retried
     * in the save, structure and reindex bands (101, 201, 301, 305), and parked rows (401, 505,
     * 705).
     * Expected Result: The failed count grows by 7 and the failed listing returns exactly those 7
     * rows; the parked count grows by 3 (AC-006).
     */
    @Test
    public void countAndListingReturnSameRows() throws Exception {
        final long countBefore = reindexQueueAPI.failedRecordCount();
        final long parkedBefore = reindexQueueAPI.parkedRecordCount();

        final Map<Integer, String> byPriority = new HashMap<>();
        for (final int priority : new int[]{0, 100, 101, 200, 201, 300, 301, 305, 401, 505, 705}) {
            byPriority.put(priority, insertRow(priority));
        }
        final Set<String> expectedFailed = Set.of(101, 201, 301, 305, 401, 505, 705).stream()
                .map(byPriority::get).collect(Collectors.toSet());

        final Set<String> listed = reindexQueueAPI.getFailedReindexRecords().stream()
                .map(ReindexEntry::getIdentToIndex)
                .filter(inserted::contains)
                .collect(Collectors.toSet());

        assertEquals("the listing must return every row that failed at least once",
                expectedFailed, listed);
        assertEquals("the count must describe the same rows as the listing",
                7, reindexQueueAPI.failedRecordCount() - countBefore);
        assertEquals("only rows at or above ERROR (400) are parked",
                3, reindexQueueAPI.parkedRecordCount() - parkedBefore);
    }

    /**
     * Given Scenario: Parked rows of existing content at 705 (reindex), 505 (save) and 401 (ASAP),
     * left by another server long ago, and a row still being retried at 302.
     * Expected Result: The retry moves each parked row back to its original band with a fresh
     * budget (300, 100, 0), keeps its row id and action, clears its server and stamps it now, so
     * the stale-row requeue does not take it for a stuck one; the retrying row is untouched
     * (AC-001).
     */
    @Test
    public void retryRequeuesAtOriginalBandWithFreshBudget() throws Exception {
        final Date longAgo = new Date(0);
        final long reindexRow = insertRow(newContentlet("a").getIdentifier(), 705, longAgo);
        final long saveRow = insertRow(newContentlet("b").getIdentifier(), 505, longAgo);
        final long asapRow = insertRow(newContentlet("c").getIdentifier(), 401, longAgo);
        final long retryingRow = insertRow(newContentlet("d").getIdentifier(), 302, longAgo);
        final Date beforeRetry = new Date(DbConnectionFactory.now().getTime() - 1_000);

        final ParkedRecordsRetry retried = reindexQueueAPI.retryParkedRecords(systemUser);

        assertTrue("at least the three parked rows are requeued: " + retried,
                retried.requeued() >= 3);
        assertRow(reindexRow, 300, beforeRetry);
        assertRow(saveRow, 100, beforeRetry);
        assertRow(asapRow, 0, beforeRetry);
        final Map<String, Object> untouched = row(retryingRow);
        assertEquals(302, ((Number) untouched.get("priority")).intValue());
        assertEquals(longAgo.getTime(), ((Date) untouched.get("time_entered")).getTime());
    }

    /**
     * Given Scenario: A parked reindex row whose content no longer exists, next to a parked row of
     * existing content.
     * Expected Result: The retry turns the first into a delete, so its stale document leaves the
     * index instead of the row being dropped silently; the second stays a reindex (AC-001).
     */
    @Test
    public void retryOfDeletedContentBecomesDelete() throws Exception {
        final long goneRow = insertRow(UUIDGenerator.generateUuid(), 705, new Date());
        final long existingRow = insertRow(newContentlet("e").getIdentifier(), 705, new Date());

        final ParkedRecordsRetry retried = reindexQueueAPI.retryParkedRecords(systemUser);

        assertTrue("the row of deleted content is requeued as a delete: " + retried,
                retried.requeuedAsDelete() >= 1);
        assertEquals(ReindexAction.DELETE.ordinal(),
                ((Number) row(goneRow).get("dist_action")).intValue());
        assertEquals(ReindexAction.REINDEX.ordinal(),
                ((Number) row(existingRow).get("dist_action")).intValue());
    }

    /**
     * Given Scenario: A saved content's journal row is parked (its cause has since been removed).
     * Expected Result: After the retry the next reindex iteration claims it, the content is in
     * the index and its rows leave the journal (AC-001).
     */
    @Test
    public void retriedRowIsIndexedEndToEnd() throws Exception {
        final Contentlet content = newContentlet("retried body");
        final String identifier = content.getIdentifier();
        inserted.add(identifier);
        parkAllRowsOf(identifier, 505);

        reindexQueueAPI.retryParkedRecords(systemUser);
        runOneReindexIteration(claimEntries(identifier));

        assertTrue("the retried content must be indexed: " + identifier,
                awaitIndexed(identifier));
        assertEquals("the retried rows must leave the journal", 0, rowsOf(identifier));
    }

    /**
     * Given Scenario: A content is indexed, then its versions disappear from the database while
     * its document stays in the index (a stale search hit), and its journal row is parked.
     * Expected Result: The retry requeues the row as a delete; the next iteration removes the
     * stale document and acknowledges the row, without failing the batch (AC-001).
     */
    @Test
    public void retriedDeleteRemovesStaleDocumentAndLeavesJournal() throws Exception {
        final Contentlet content = newContentlet("stale body");
        final String identifier = content.getIdentifier();
        inserted.add(identifier);
        runOneReindexIteration(claimEntries(identifier));
        assertTrue("the content must be indexed first: " + identifier, awaitIndexed(identifier));

        new DotConnect().setSQL("DELETE FROM contentlet_version_info WHERE identifier = ?")
                .addParam(identifier).loadResult();
        insertRow(identifier, 705, new Date());

        reindexQueueAPI.retryParkedRecords(systemUser);
        runOneReindexIteration(claimEntries(identifier));

        assertTrue("the stale document must leave the index: " + identifier,
                awaitAbsent(identifier));
        assertEquals("the retried delete must leave the journal", 0, rowsOf(identifier));
    }

    /**
     * Given Scenario: A parked row whose cause is still there: an oversized document the mapping
     * step withholds every time.
     * Expected Result: After the retry it fails again, parks again once its budget runs out, and
     * stays parked: nothing requeues it by itself (AC-002).
     */
    @Test
    public void retriedRowParksAgainWhenCauseRemains() throws Exception {
        final Contentlet content = newContentlet("x".repeat(OVERSIZED_LENGTH));
        final String identifier = content.getIdentifier();
        inserted.add(identifier);
        parkAllRowsOf(identifier, 505);

        reindexQueueAPI.retryParkedRecords(systemUser);
        assertTrue("the retry must send the row back to the queue",
                highestPriorityOf(identifier) < 400);
        for (int attempt = 0; attempt <= ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS
                && highestPriorityOf(identifier) < 400; attempt++) {
            runOneReindexIteration(claimEntries(identifier));
        }

        assertTrue("the row must park again", highestPriorityOf(identifier) >= 400);
        for (int poll = 0; poll < 3; poll++) {
            reindexQueueAPI.findContentToReindex(1_000);
        }
        assertTrue("a parked row must not be requeued by itself",
                highestPriorityOf(identifier) >= 400);
    }

    /**
     * Given Scenario: A parked row (705) and a row still being retried (302).
     * Expected Result: The operator's clear deletes the parked row only, returns its identifier
     * for the log, and leaves the retrying row to be retried (AC-003).
     */
    @Test
    public void clearDeletesParkedOnlyAndReturnsIdentifiers() throws Exception {
        final String parked = UUIDGenerator.generateUuid();
        final String retrying = UUIDGenerator.generateUuid();
        final long parkedRow = insertRow(parked, 705, new Date());
        final long retryingRow = insertRow(retrying, 302, new Date());

        final List<String> cleared = FactoryLocator.getReindexQueueFactory().clearParkedRecords();

        assertTrue("the parked identifier is returned for the log: " + cleared,
                cleared.contains(parked));
        assertTrue("a retrying row is not cleared", !cleared.contains(retrying));
        assertEquals("the parked row is gone", 0, rowsWithId(parkedRow));
        assertEquals("the retrying row stays", 1, rowsWithId(retryingRow));
    }

    /**
     * Given Scenario: A parked row (705), a retrying row (302) and a fresh reindex row (300).
     * Expected Result: {@code deleteFailedRecords()}, used when a full reindex starts and when the
     * server boots without indices, still deletes every row above 300: there every row is
     * requeued right after, so nothing is lost (AC-003).
     */
    @Test
    public void deleteFailedRecordsStillWide() throws Exception {
        final long parkedRow = insertRow(UUIDGenerator.generateUuid(), 705, new Date());
        final long retryingRow = insertRow(UUIDGenerator.generateUuid(), 302, new Date());
        final long freshRow = insertRow(UUIDGenerator.generateUuid(), 300, new Date());

        reindexQueueAPI.deleteFailedRecords();

        assertEquals(0, rowsWithId(parkedRow));
        assertEquals(0, rowsWithId(retryingRow));
        assertEquals(1, rowsWithId(freshRow));
    }

    /**
     * Inserts one journal row with the given priority under a fresh identifier.
     *
     * @return the identifier of the row
     */
    private String insertRow(final int priority) throws DotDataException {
        final String identifier = UUIDGenerator.generateUuid();
        insertRow(identifier, priority, DbConnectionFactory.now());
        return identifier;
    }

    /**
     * Inserts one reindex row for the identifier, owned by another server, and returns its id.
     */
    private long insertRow(final String identifier, final int priority, final Date timeEntered)
            throws DotDataException {
        inserted.add(identifier);
        final List<Map<String, Object>> rows = new DotConnect().setSQL(
                        "INSERT INTO dist_reindex_journal"
                        + " (inode_to_index, ident_to_index, priority, dist_action, time_entered,"
                        + " serverid) VALUES (?, ?, ?, ?, ?, ?) RETURNING id")
                .addParam(identifier).addParam(identifier).addParam(priority)
                .addParam(ReindexAction.REINDEX.ordinal()).addParam(timeEntered)
                .addParam("another-server")
                .loadObjectResults();
        return ((Number) rows.get(0).get("id")).longValue();
    }

    /** Reads one journal row by id. */
    private static Map<String, Object> row(final long id) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect().setSQL(
                        "SELECT priority, serverid, dist_action, time_entered"
                        + " FROM dist_reindex_journal WHERE id = ?")
                .addParam(id).loadObjectResults();
        assertEquals("row " + id + " must still exist", 1, rows.size());
        return rows.get(0);
    }

    /** Asserts a requeued row: its band, no server, and stamped after {@code notBefore}. */
    private static void assertRow(final long id, final int priority, final Date notBefore)
            throws DotDataException {
        final Map<String, Object> row = row(id);
        assertEquals(priority, ((Number) row.get("priority")).intValue());
        assertNull("the requeued row must not keep its old server", row.get("serverid"));
        assertEquals(ReindexAction.REINDEX.ordinal(), ((Number) row.get("dist_action")).intValue());
        assertTrue("the requeued row must be stamped now",
                !((Date) row.get("time_entered")).before(notBefore));
    }

    /** Parks every journal row of the identifier at the given priority. */
    private static void parkAllRowsOf(final String identifier, final int priority)
            throws DotDataException {
        new DotConnect().setSQL("UPDATE dist_reindex_journal SET priority = ?"
                        + " WHERE ident_to_index = ?")
                .addParam(priority).addParam(identifier).loadResult();
        assertTrue("the content must have a journal row to park", rowsOf(identifier) > 0);
    }

    private static int rowsWithId(final long id) throws DotDataException {
        return new DotConnect().setSQL("SELECT count(*) AS count FROM dist_reindex_journal"
                + " WHERE id = ?").addParam(id).getInt("count");
    }

    private static int rowsOf(final String identifier) throws DotDataException {
        return new DotConnect().setSQL("SELECT count(*) AS count FROM dist_reindex_journal"
                + " WHERE ident_to_index = ?").addParam(identifier).getInt("count");
    }

    private static int highestPriorityOf(final String identifier) throws DotDataException {
        return new DotConnect().setSQL("SELECT coalesce(max(priority), -1) AS count"
                + " FROM dist_reindex_journal WHERE ident_to_index = ?")
                .addParam(identifier).getInt("count");
    }

    private static Contentlet newContentlet(final String body) {
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty("body", body)
                .setPolicy(IndexPolicy.DEFER)
                .nextPersisted();
        createdContentlets.add(contentlet);
        return contentlet;
    }

    /**
     * Claims the identifier's journal entries. The queue serves claims from an in-memory buffer
     * that may hold stale entries from earlier tests, so this polls until the row comes back.
     */
    private static Map<String, ReindexEntry> claimEntries(final String identifier)
            throws Exception {
        ReindexQueueFactory.resetLastIdReindexed();
        final Map<String, ReindexEntry> entries = new HashMap<>();
        for (int i = 0; i < 20 && entries.isEmpty(); i++) {
            reindexQueueAPI.findContentToReindex(1_000).forEach((id, entry) -> {
                if (identifier.equals(id)) {
                    entries.put(id, entry);
                }
            });
        }
        assertEquals("the entry must be claimed from the queue",
                new HashSet<>(List.of(identifier)), entries.keySet());
        return entries;
    }

    /** Runs one iteration the way {@code ReindexThread.runReindexLoop} does, ack included. */
    private static void runOneReindexIteration(final Map<String, ReindexEntry> entries)
            throws Exception {
        final BulkProcessorListener listener = new BulkProcessorListener();
        listener.workingRecords.putAll(entries);
        try (final IndexBulkProcessor processor = indexAPI.createBulkProcessor(listener)) {
            indexAPI.appendToBulkProcessor(processor, entries.values());
        }
        final List<ReindexEntry> acknowledged = listener.entriesToAcknowledge();
        if (!acknowledged.isEmpty()) {
            reindexQueueAPI.deleteReindexEntry(acknowledged);
        }
    }

    /** Waits for the index to refresh and tells whether the identifier is no longer searchable. */
    private static boolean awaitAbsent(final String identifier) throws Exception {
        for (int i = 0; i < 20; i++) {
            CacheLocator.getESQueryCache().clearCache();
            CacheLocator.getOSQueryCache().clearCache();
            if (APILocator.getContentletAPI().indexCount("+identifier_dotraw:" + identifier,
                    systemUser, false) == 0) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }

    /** Waits for the index to refresh and tells whether the identifier is searchable. */
    private static boolean awaitIndexed(final String identifier) throws Exception {
        for (int i = 0; i < 20; i++) {
            CacheLocator.getESQueryCache().clearCache();
            CacheLocator.getOSQueryCache().clearCache();
            if (APILocator.getContentletAPI().indexCount("+identifier_dotraw:" + identifier,
                    systemUser, false) > 0) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }
}
