package com.dotmarketing.common.reindex;

import static com.dotmarketing.common.reindex.ReindexQueueFactory.REINDEX_MAX_FAILURE_ATTEMPTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.common.reindex.ReindexQueueFactory.ReindexAction;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the durable record of a content deletion in
 * {@code dist_reindex_journal}.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/37276">#37276</a>:</p>
 * <ul>
 *   <li><b>AC-001</b> — the pending removal survives as a durable record rather than being lost
 *       with the in-memory commit listener.</li>
 *   <li><b>AC-007</b> — a removal that exhausts its retry attempts stays discoverable.</li>
 * </ul>
 *
 * <p>Destroying content deletes the database rows inside a transaction and then defers the index
 * removal to a post-commit listener that records nothing durable. These tests assert the journal
 * row that makes that removal survivable — which is the testable substance of AC-001, since the
 * JVM-restart path itself cannot be exercised from a test.</p>
 */
public class ReindexDeleteJournalTest {

    private static final ReindexQueueFactory factory = new ReindexQueueFactory();

    private static User systemUser;
    private static ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    @Before
    public void pauseReindexAndClearJournal() throws DotDataException {
        // The journal must not drain underneath the assertions.
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
        ReindexThread.pause();
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
    }

    @After
    public void resume() {
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
        ReindexThread.unpause();
    }

    /** Reads the journal rows for one identifier, newest first. */
    private List<Map<String, Object>> journalRowsFor(final String identifier)
            throws DotDataException {
        return new DotConnect()
                .setSQL("select id, ident_to_index, priority, dist_action, index_val "
                        + "from dist_reindex_journal where ident_to_index = ? order by id desc")
                .addParam(identifier)
                .loadObjectResults();
    }

    /**
     * Reads only the DELETE rows for one identifier.
     *
     * <p>A destroy also enqueues REINDEX entries for the content it touches on the way out —
     * relationships, categories and permission-driven reindexes all land in the same journal for
     * the same identifier. Those are legitimate and unrelated to the removal contract, so the
     * assertions here filter to {@code dist_action = DELETE} rather than counting every row.
     * (That REINDEX-beside-DELETE pair for one identifier is precisely the batch collision fixed
     * alongside this work — see ReindexQueueFactoryBatchKeyTest.)</p>
     */
    private List<Map<String, Object>> deleteRowsFor(final String identifier)
            throws DotDataException {
        return new DotConnect()
                .setSQL("select id, ident_to_index, priority, dist_action, index_val "
                        + "from dist_reindex_journal where ident_to_index = ? and dist_action = ? "
                        + "order by id desc")
                .addParam(identifier)
                .addParam(ReindexAction.DELETE.ordinal())
                .loadObjectResults();
    }

    private static int intOf(final Map<String, Object> row, final String column) {
        return ((Number) row.get(column)).intValue();
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#destroy}
     * Given Scenario: A contentlet is created and then destroyed.
     * Expected Result: A dist_reindex_journal row exists for its identifier carrying
     *                  dist_action = DELETE, so the removal is owed durably and will be retried
     *                  even if the in-memory commit listener never runs.
     */
    @Test
    public void test_destroy_writesDurableDeleteEntryToJournal() throws Exception {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.DEFER).nextPersisted();
        final String identifier = contentlet.getIdentifier();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        APILocator.getContentletAPI().destroy(contentlet, systemUser, false);

        final List<Map<String, Object>> deletes = deleteRowsFor(identifier);
        assertEquals("Destroy must leave exactly one durable removal record. All journal rows for "
                        + "this identifier: " + journalRowsFor(identifier),
                1, deletes.size());
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#destroy}
     * Given Scenario: A destroy is performed inside a transaction that is then rolled back.
     * Expected Result: No journal row survives. The removal record must share the fate of the row
     *                  deletion it describes — enqueuing outside the transaction would leave the
     *                  index owing a removal for content that was never actually deleted.
     */
    @Test
    public void test_rolledBackDestroy_leavesNoJournalEntry() throws Exception {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.DEFER).nextPersisted();
        final String identifier = contentlet.getIdentifier();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        try {
            HibernateUtil.startTransaction();
            APILocator.getContentletAPI().destroy(contentlet, systemUser, false);
            HibernateUtil.rollbackTransaction();
        } finally {
            HibernateUtil.closeSessionSilently();
        }

        assertTrue("A rolled-back destroy must leave no removal record",
                deleteRowsFor(identifier).isEmpty());
    }

    /**
     * Method to test: {@link ReindexQueueFactory#markAsFailed(ReindexEntry, String)}
     * Given Scenario: A pending removal fails REINDEX_MAX_FAILURE_ATTEMPTS times.
     * Expected Result: The row is still in the journal, parked above ERROR priority, still marked
     *                  as a DELETE, and carrying the last failure cause — so the set of removals
     *                  still owed to the index can be enumerated with one query.
     *
     * <p>AC-007. The acceptance here is <b>enumerability</b>, not the retry count: asserting the
     * number of attempts would pin the test to a configurable value, while what an operator needs
     * is that the residue can be found at all. Exhaustion must not delete the record.</p>
     */
    @Test
    public void test_exhaustedRemoval_staysDiscoverableInJournal() throws Exception {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.DEFER).nextPersisted();
        final String identifier = contentlet.getIdentifier();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        APILocator.getContentletAPI().destroy(contentlet, systemUser, false);

        final List<Map<String, Object>> initial = deleteRowsFor(identifier);
        assertEquals("Precondition: the destroy left a removal record. All journal rows: "
                        + journalRowsFor(identifier), 1, initial.size());

        // Drive the entry through its retry budget.
        ReindexEntry entry = ReindexEntry.builder()
                .id(((Number) initial.get(0).get("id")).longValue())
                .identToIndex(identifier)
                .priority(intOf(initial.get(0), "priority"))
                .isDelete(true)
                .build();

        for (int attempt = 0; attempt <= REINDEX_MAX_FAILURE_ATTEMPTS; attempt++) {
            factory.markAsFailed(entry, "forced failure " + attempt);
            final Map<String, Object> row = deleteRowsFor(identifier).get(0);
            entry = ReindexEntry.builder()
                    .id(((Number) row.get("id")).longValue())
                    .identToIndex(identifier)
                    .priority(intOf(row, "priority"))
                    .isDelete(true)
                    .build();
        }

        final List<Map<String, Object>> after = deleteRowsFor(identifier);
        assertEquals("Exhaustion must not delete the record", 1, after.size());

        final Map<String, Object> parked = after.get(0);
        assertTrue("An exhausted removal is parked above ERROR priority, where the drain query "
                        + "no longer reaches it — that is what makes it enumerable residue",
                intOf(parked, "priority") > Priority.ERROR.dbValue());
        assertEquals("It must still be a DELETE — the pending work is a removal, not a reindex",
                ReindexAction.DELETE.ordinal(), intOf(parked, "dist_action"));
        assertNotNull("The last failure cause must be recorded for the operator",
                parked.get("index_val"));
    }
}
