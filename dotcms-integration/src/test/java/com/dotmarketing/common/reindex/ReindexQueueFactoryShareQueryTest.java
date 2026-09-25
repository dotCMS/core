package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.common.reindex.StarvedSliceDetector.SliceSnapshot;
import com.dotmarketing.exception.DotDataException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Runs the two journal queries behind the reindex share takeover against the real database
 * (issue #36482): the per-share summary an idle server uses to spot a share nobody is draining,
 * and the read of that share's entries.
 *
 * <p>These are the parts a unit test cannot cover. The first version of the summary query grouped
 * by a bound {@code MOD(id, ?)}, which PostgreSQL rejects; the error escaped into the reindex
 * thread on every idle poll.</p>
 */
public class ReindexQueueFactoryShareQueryTest {

    /** Explicit ids, so which share each row falls in is known. */
    private static final long BASE_ID = 9_000_000_000L;

    private final ReindexQueueFactory factory = new ReindexQueueFactory();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /** The thread is paused so nothing drains the journal mid-test, which starts empty. */
    @Before
    public void pauseAndClear() throws DotDataException {
        ReindexThread.pause();
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
    }

    @After
    public void clearAndResume() throws DotDataException {
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
        ReindexThread.unpause();
    }

    /**
     * Given rows in two shares of a two-way split, the summary reports each share's row count and
     * lowest id, and leaves out rows parked past the eligible priority.
     */
    @Test
    public void loadShareSnapshot_summarisesEachShare() throws DotDataException {
        insert(BASE_ID, Priority.REINDEX.dbValue());         // share 0
        insert(BASE_ID + 2, Priority.REINDEX.dbValue());     // share 0
        insert(BASE_ID + 1, Priority.REINDEX.dbValue());     // share 1
        insert(BASE_ID + 3, Priority.ERROR.dbValue() + 500); // share 1, parked: not eligible

        final Map<Integer, SliceSnapshot> snapshot = factory.loadShareSnapshot(2,
                Priority.ERROR.dbValue());

        assertEquals(new SliceSnapshot(2, BASE_ID), snapshot.get(0));
        assertEquals(new SliceSnapshot(1, BASE_ID + 1), snapshot.get(1));
    }

    /** Reading one share returns that share's eligible entries only. */
    @Test
    public void loadShareEntries_readsOneShare() throws DotDataException {
        insert(BASE_ID, Priority.REINDEX.dbValue());
        insert(BASE_ID + 1, Priority.REINDEX.dbValue());
        insert(BASE_ID + 2, Priority.REINDEX.dbValue());

        final List<Long> ids = factory.loadShareEntries(2, 0, Priority.ERROR.dbValue()).stream()
                .map(ReindexEntry::getId)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(List.of(BASE_ID, BASE_ID + 2), ids);
    }

    /** Nothing eligible: the summary is empty rather than failing. */
    @Test
    public void loadShareSnapshot_emptyJournal_isEmpty() throws DotDataException {
        assertTrue(factory.loadShareSnapshot(3, Priority.ERROR.dbValue()).isEmpty());
    }

    private static void insert(final long id, final int priority) throws DotDataException {
        new DotConnect().setSQL("insert into dist_reindex_journal"
                        + " (id, inode_to_index, ident_to_index, priority, dist_action, time_entered)"
                        + " values (?, ?, ?, ?, 1, now())")
                .addParam(id)
                .addParam("inode-" + id)
                .addParam("ident-" + id)
                .addParam(priority)
                .loadResult();
    }
}
