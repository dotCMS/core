package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.exception.DotDataException;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the batch assembly in {@link ReindexQueueFactory#findContentToReindex(int)}.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/37276">#37276</a> AC-008: a pending
 * removal and a pending reindex for the same identifier must resolve deterministically to the
 * newer of the two, and the older must not be applied afterwards.</p>
 *
 * <p><b>Why this matters.</b> The batch is a {@code Map} keyed by identifier alone, while
 * {@link ReindexEntry} equality includes the delete flag — so the two entries are not equal (the
 * duplicate-drain loop does not collapse them) yet they collide on the key, and the later
 * {@code poll()} silently overwrites the earlier. Nothing depends on that today because no
 * production code enqueues removals; once {@code destroyContentlets} does, the pair becomes the
 * normal sequence (content saved, then destroyed).</p>
 *
 * <p><b>Two ways the pair can arrive, and both are broken.</b> {@code loadUpLocalQueue} reads
 * {@code ORDER BY priority ASC} with <em>no secondary sort key</em>:</p>
 *
 * <ul>
 *   <li><b>Equal priority — the common case.</b> A save enqueues via
 *       {@code addIdentifierReindex}, which defaults to {@link Priority#NORMAL} (100); a destroy
 *       enqueues via {@code addIdentifierDelete}, also at {@link Priority#NORMAL}. With equal
 *       priorities and no tiebreaker, the order the database returns is <b>undefined by
 *       contract</b>. The same content can be removed correctly in one batch and silently
 *       re-added in the next, with nothing about the system having changed.</li>
 *   <li><b>Unequal priority — deterministically wrong.</b> A full reindex enqueues at
 *       {@link Priority#REINDEX} (300) and a content-type reindex at {@link Priority#STRUCTURE}
 *       (200). A removal at {@link Priority#NORMAL} (100) is polled <em>first</em>, so the
 *       higher-priority reindex is polled second and overwrites it — every time.</li>
 * </ul>
 *
 * <p>The tests below pin the unequal-priority case (deterministic, so it can be asserted) and
 * the equal-priority case (asserted by id rather than by arrival order, which is the whole
 * point — after the fix, arrival order stops mattering).</p>
 *
 * <p>These are pure unit tests: seeding the local queue means {@code findContentToReindex} never
 * reaches {@code loadUpLocalQueue}, so no database is touched.</p>
 */
public class ReindexQueueFactoryBatchKeyTest {

    private static final String IDENTIFIER = "a1b2c3d4-0000-0000-0000-00000000cafe";
    private static final String OTHER_IDENTIFIER = "a1b2c3d4-0000-0000-0000-00000000beef";

    private ReindexQueueFactory factory;

    @Before
    public void setUp() {
        factory = new ReindexQueueFactory();
        factory.getLocalQueue().clear();
        ReindexQueueFactory.resetLastIdReindexed();
    }

    @After
    public void tearDown() {
        factory.getLocalQueue().clear();
        ReindexQueueFactory.resetLastIdReindexed();
    }

    private static ReindexEntry entry(final long id, final String identifier,
            final int priority, final boolean delete) {
        return ReindexEntry.builder()
                .id(id)
                .identToIndex(identifier)
                .priority(priority)
                .isDelete(delete)
                .build();
    }

    /**
     * Given Scenario: <b>unequal priority.</b> A destroy queues a DELETE at NORMAL (100) while a
     *                 full reindex has an older REINDEX pending at REINDEX (300).
     *                 {@code ORDER BY priority ASC} polls the DELETE first, the REINDEX second.
     * When : findContentToReindex assembles the batch.
     * Then : the DELETE survives, because it is the newer statement about what the index should
     *        hold. Today the REINDEX wins simply by being polled last — deterministically, on
     *        every run, for as long as a full reindex overlaps a destroy.
     */
    @Test
    public void test_newerDelete_survives_olderReindex_forSameIdentifier() throws DotDataException {
        factory.getLocalQueue().add(entry(20L, IDENTIFIER, Priority.NORMAL.dbValue(), true));
        factory.getLocalQueue().add(entry(10L, IDENTIFIER, Priority.REINDEX.dbValue(), false));

        final Map<String, ReindexEntry> batch = factory.findContentToReindex(50);

        assertEquals("One outcome per identifier per batch", 1, batch.size());
        final ReindexEntry winner = batch.get(IDENTIFIER);
        assertTrue("The newer entry (id 20) is the DELETE; it must win", winner.isDelete());
        assertEquals(20L, winner.getId());
    }

    /**
     * Given Scenario: the reverse — a REINDEX is the newer entry (id 20) and a DELETE the older
     *                 (id 10). An identifier destroyed and later reused is a reindex, not a
     *                 removal.
     * When : findContentToReindex assembles the batch.
     * Then : the REINDEX survives. Resolution is by id, not by a rule that deletes always win.
     *
     * <p>This one passes today by coincidence — arrival order happens to agree with id here.
     * It is kept because after the fix it must pass for the right reason, and because it is what
     * stops the fix from being implemented as "a delete always wins".</p>
     */
    @Test
    public void test_newerReindex_survives_olderDelete_forSameIdentifier() throws DotDataException {
        factory.getLocalQueue().add(entry(10L, IDENTIFIER, Priority.NORMAL.dbValue(), true));
        factory.getLocalQueue().add(entry(20L, IDENTIFIER, Priority.REINDEX.dbValue(), false));

        final Map<String, ReindexEntry> batch = factory.findContentToReindex(50);

        assertEquals(1, batch.size());
        final ReindexEntry winner = batch.get(IDENTIFIER);
        assertFalse("The newer entry (id 20) is the REINDEX; it must win", winner.isDelete());
        assertEquals(20L, winner.getId());
    }

    /**
     * Given Scenario: <b>equal priority — the case a customer actually hits.</b> A save queues a
     *                 REINDEX at NORMAL (100) and the subsequent destroy queues a DELETE at
     *                 NORMAL (100) too. {@code ORDER BY priority ASC} has no tiebreaker, so the
     *                 order the database returns is undefined; this test pins <em>both</em>
     *                 arrival orders and requires the same outcome from each.
     * When : findContentToReindex assembles the batch.
     * Then : the newer entry by id wins regardless of the order the entries arrived in.
     *
     * <p>This is the test that matters most. The other collision cases are deterministic and
     * therefore at least debuggable; this one is not. Today the same content can be removed
     * correctly in one batch and silently re-added in the next, with nothing about the system
     * having changed — which is exactly the "unexplained drift" shape the field report had.</p>
     */
    @Test
    public void test_equalPriority_newestWins_regardlessOfArrivalOrder() throws DotDataException {
        // Arrival order A: the DELETE (newer) is polled first.
        factory.getLocalQueue().add(entry(20L, IDENTIFIER, Priority.NORMAL.dbValue(), true));
        factory.getLocalQueue().add(entry(10L, IDENTIFIER, Priority.NORMAL.dbValue(), false));

        Map<String, ReindexEntry> batch = factory.findContentToReindex(50);

        assertEquals(1, batch.size());
        assertTrue("Arrival order A: the newer entry (id 20) is the DELETE and must win",
                batch.get(IDENTIFIER).isDelete());
        assertEquals(20L, batch.get(IDENTIFIER).getId());

        // Arrival order B: identical entries, opposite order. The outcome must not change.
        factory.getLocalQueue().clear();
        ReindexQueueFactory.resetLastIdReindexed();
        factory.getLocalQueue().add(entry(10L, IDENTIFIER, Priority.NORMAL.dbValue(), false));
        factory.getLocalQueue().add(entry(20L, IDENTIFIER, Priority.NORMAL.dbValue(), true));

        batch = factory.findContentToReindex(50);

        assertEquals(1, batch.size());
        assertTrue("Arrival order B: same entries, same winner — order must not decide this",
                batch.get(IDENTIFIER).isDelete());
        assertEquals(20L, batch.get(IDENTIFIER).getId());
    }

    /**
     * Given Scenario: a colliding pair for one identifier plus an unrelated identifier.
     * When : findContentToReindex assembles the batch.
     * Then : the losing entry is absent from the batch and unrelated identifiers are untouched.
     *
     * <p>{@code findContentToReindex} performs no database write at all — dropping the loser here
     * is an in-memory decision only. The row itself is removed later, by {@code deleteReindexEntry}
     * acknowledging the winner: that sweep covers rows for the same identifier up to the winner's
     * id, and the loser is one of them. That is correct, not incidental — the loser has been
     * superseded, so re-applying it could only undo the outcome just applied. The same id bound is
     * what stops the sweep from reaching a row queued after this batch was loaded, which is a
     * different case entirely and is covered in ReindexDeleteJournalTest.</p>
     */
    @Test
    public void test_losingEntry_isDroppedFromBatch() throws DotDataException {
        factory.getLocalQueue().add(entry(20L, IDENTIFIER, Priority.NORMAL.dbValue(), true));
        factory.getLocalQueue().add(entry(10L, IDENTIFIER, Priority.REINDEX.dbValue(), false));
        factory.getLocalQueue().add(entry(30L, OTHER_IDENTIFIER, Priority.NORMAL.dbValue(), false));

        final Map<String, ReindexEntry> batch = factory.findContentToReindex(50);

        assertEquals("Two identifiers in, two outcomes out", 2, batch.size());
        assertTrue(batch.get(IDENTIFIER).isDelete());
        assertFalse(batch.get(OTHER_IDENTIFIER).isDelete());
        assertEquals("The unrelated identifier is unaffected by the collision",
                30L, batch.get(OTHER_IDENTIFIER).getId());
    }

    /**
     * Given Scenario: the same identifier queued three times as identical REINDEX entries, as a
     *                 hot identifier produces during a full reindex.
     * When : findContentToReindex assembles the batch.
     * Then : they collapse to a single entry.
     *
     * <p>This guards the throughput regression rejected in {@code data-model.md}: keying the
     * batch by id instead of identifier would turn every redundant entry into its own bulk
     * operation on the highest-volume path in the pipeline.</p>
     */
    @Test
    public void test_identicalRepeatedReindexEntries_areStillDeduplicated() throws DotDataException {
        factory.getLocalQueue().add(entry(10L, IDENTIFIER, Priority.NORMAL.dbValue(), false));
        factory.getLocalQueue().add(entry(11L, IDENTIFIER, Priority.NORMAL.dbValue(), false));
        factory.getLocalQueue().add(entry(12L, IDENTIFIER, Priority.NORMAL.dbValue(), false));

        final Map<String, ReindexEntry> batch = factory.findContentToReindex(50);

        assertEquals("Repeated entries for one identifier collapse to one", 1, batch.size());
        assertFalse(batch.get(IDENTIFIER).isDelete());
    }
}
