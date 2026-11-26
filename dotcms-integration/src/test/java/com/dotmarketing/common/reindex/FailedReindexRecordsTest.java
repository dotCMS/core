package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueFactory.ReindexAction;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import java.util.ArrayList;
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

    private static ReindexQueueAPI reindexQueueAPI;

    /** Identifier of every row a test inserted, removed after each test. */
    private final List<String> inserted = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        reindexQueueAPI = APILocator.getReindexQueueAPI();
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
        ReindexThread.pause();
    }

    @AfterClass
    public static void restore() {
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
        ReindexThread.unpause();
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
     * Inserts one journal row with the given priority under a fresh identifier.
     *
     * @return the identifier of the row
     */
    private String insertRow(final int priority) throws DotDataException {
        final String identifier = UUIDGenerator.generateUuid();
        new DotConnect().setSQL("INSERT INTO dist_reindex_journal"
                        + " (inode_to_index, ident_to_index, priority, dist_action, time_entered)"
                        + " VALUES (?, ?, ?, ?, ?)")
                .addParam(identifier).addParam(identifier).addParam(priority)
                .addParam(ReindexAction.REINDEX.ordinal()).addParam(DbConnectionFactory.now())
                .loadResult();
        inserted.add(identifier);
        return identifier;
    }
}
