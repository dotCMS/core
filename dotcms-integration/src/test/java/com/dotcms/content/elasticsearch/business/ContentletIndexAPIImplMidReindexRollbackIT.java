package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.IndexAPIImpl;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression tests for <a href="https://github.com/dotCMS/core/issues/36471">#36471</a> —
 * rolling the migration phase back to 0 while a dual-write full reindex is in flight must not
 * wipe the OS index store, and must not strand a partial OS reindex pair that a later boot
 * catchup could adopt as active.
 *
 * <h2>Scenario under test</h2>
 * <p>Phase 1, full reindex running: reindex slots exist on both engines. The operator flips
 * {@code FEATURE_FLAG_OPEN_SEARCH_PHASE} back to 0 mid-journal-drain, then the ES switchover
 * (or an abort) completes in Phase 0. Before the fix:</p>
 * <ul>
 *   <li>{@code IndiciesFactory.point()} deleted rows by {@code index_type} across ALL
 *       {@code index_version} values, wiping the active OS working/live rows (4 rows → 2);</li>
 *   <li>the partial OS reindex pair stayed on the cluster unknown to the DB, and a later boot
 *       catchup mirror-adopted it as active — silently serving a fraction of the content.</li>
 * </ul>
 *
 * <h2>Fixed contract (asserted here)</h2>
 * <ul>
 *   <li>The OS working/live rows survive a Phase-0 switchover and abort untouched.</li>
 *   <li>The OS reindex slots are cleared (a stranded slot would make {@code isInFullReindex()}
 *       true again on a later flip to Phase 2 and trigger a switchover over null ES pointers).</li>
 *   <li>The partial physical OS reindex indices are deleted from the cluster — nothing left
 *       for a boot catchup to adopt.</li>
 * </ul>
 *
 * <h2>Run command</h2>
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true \
 *       -Dit.test=ContentletIndexAPIImplMidReindexRollbackIT
 * </pre>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletIndexAPIImplMidReindexRollbackIT extends IntegrationTestBase {

    // ── Unique run suffix prevents cross-run index name collisions ─────────────
    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /**
     * Timestamps embedded in the reindex index names. They must be (a) parseable as the
     * trailing {@code _yyyyMMddHHmmss} suffix — {@code fullReindexSwitchover}'s minimum-runtime
     * guard parses them — and (b) far in the past so the guard ({@code
     * REINDEX_THREAD_MINIMUM_RUNTIME_IN_SEC}, default 30s) does not defer the switchover.
     */
    private static final String OLD_TS     = "20200101000000";
    private static final String REINDEX_TS = "20200102000000";

    // ── ES names (logical; stored cluster-prefixed) ─────────────────────────────
    private static final String ES_WORKING    = "working_rbk" + RUN_ID + "_" + OLD_TS;
    private static final String ES_LIVE       = "live_rbk"    + RUN_ID + "_" + OLD_TS;
    private static final String ES_REINDEX_WK = "working_rbk" + RUN_ID + "_" + REINDEX_TS;
    private static final String ES_REINDEX_LV = "live_rbk"    + RUN_ID + "_" + REINDEX_TS;

    // ── OS names (logical; stored cluster-prefixed + .os-tagged) ────────────────
    private static final String OS_WORKING    = IndexTag.OS.tag("working_rbk" + RUN_ID + "_" + OLD_TS);
    private static final String OS_LIVE       = IndexTag.OS.tag("live_rbk"    + RUN_ID + "_" + OLD_TS);
    private static final String OS_REINDEX_WK = IndexTag.OS.tag("working_rbk" + RUN_ID + "_" + REINDEX_TS);
    private static final String OS_REINDEX_LV = IndexTag.OS.tag("live_rbk"    + RUN_ID + "_" + REINDEX_TS);

    @Inject
    private OSIndexAPIImpl osIndexAPI;

    // ── Saved DB state — restored in @After ────────────────────────────────────
    @SuppressWarnings("deprecation")
    private IndiciesInfo savedEsInfo;
    private Optional<VersionedIndices> savedOsIndices;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws Exception {
        savedEsInfo    = APILocator.getIndiciesAPI().loadIndicies();
        savedOsIndices = APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        cleanupTestIndices();
    }

    @After
    public void tearDown() {
        Config.setProperty(FLAG_KEY, null);
        cleanupTestIndices();

        Try.run(() -> APILocator.getIndiciesAPI().point(savedEsInfo))
           .onFailure(e -> Logger.warn(this,
                   "tearDown: could not restore ES pointers: " + e.getMessage()));
        savedOsIndices.ifPresent(v ->
                Try.run(() -> APILocator.getVersionedIndicesAPI().saveIndices(v))
                   .onFailure(e -> Logger.warn(this,
                           "tearDown: could not restore OS pointers: " + e.getMessage())));
    }

    // =========================================================================
    // Root fix — IndiciesFactory.point() must only manage NULL-version rows
    // =========================================================================

    /**
     * Given: the OS store holds working/live rows (version {@code os-3.x}) alongside the
     *        legacy ES rows (NULL version) in the shared {@code indicies} table.
     * When:  the legacy ES store is re-pointed via {@code IndiciesAPI.point()} — the exact
     *        store update every ES switchover and abort performs.
     * Then:  the OS rows are untouched. Before the fix the unscoped delete-by-type inside
     *        {@code point()} wiped them (#36471, step "the .os rows die").
     */
    @Test
    public void test_legacyPoint_preservesOsVersionedRows() throws DotDataException {
        setPhase(0);
        pointOsStore(OS_WORKING, OS_LIVE, null, null);
        pointEsStore(ES_WORKING, ES_LIVE, null, null);

        // Re-point ES to a new pair — this is what the Phase-0 switchover does.
        pointEsStore(ES_REINDEX_WK, ES_REINDEX_LV, null, null);

        final Optional<VersionedIndices> os =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertTrue("OS store record must survive an ES point()", os.isPresent());
        assertEquals("OS working row must survive an ES point()",
                Optional.of(osPhysical(OS_WORKING)), os.get().working());
        assertEquals("OS live row must survive an ES point()",
                Optional.of(osPhysical(OS_LIVE)), os.get().live());

        Logger.info(this, "✅ point() preserved the OS versioned rows");
    }

    // =========================================================================
    // Phase-0 switchover after a mid-reindex rollback
    // =========================================================================

    /**
     * Given: the state a mid-drain rollback leaves behind — Phase 0, ES store with active
     *        working/live plus reindex slots, OS store with active working/live plus reindex
     *        slots pointing at a PARTIAL physical pair on the OS cluster.
     * When:  the ES switchover completes in Phase 0.
     * Then:  ES promotes its reindex pair as usual; the OS working/live rows survive; the OS
     *        reindex slots are cleared; and the partial physical OS pair is deleted from the
     *        cluster so no later boot catchup can adopt it.
     */
    @Test
    public void test_phase0Switchover_midReindexRollback_preservesOsStoreAndAbortsOsReindex()
            throws Exception {
        seedMidReindexRollbackState();

        final boolean switched =
                APILocator.getContentletIndexAPI().fullReindexSwitchover(true);
        assertTrue("Phase-0 switchover must complete", switched);

        // ES: reindex pair promoted, slots cleared — the normal Phase-0 contract.
        final IndiciesInfo esInfo = APILocator.getIndiciesAPI().loadIndicies();
        assertEquals("ES working must be the promoted reindex-working index",
                esPhysical(ES_REINDEX_WK), esInfo.getWorking());
        assertEquals("ES live must be the promoted reindex-live index",
                esPhysical(ES_REINDEX_LV), esInfo.getLive());

        assertOsReindexAborted();
        Logger.info(this, "✅ Phase-0 switchover preserved the OS store and aborted the OS reindex");
    }

    // =========================================================================
    // Phase-0 abort after a mid-reindex rollback
    // =========================================================================

    /**
     * Same rollback state as the switchover test, but the operator aborts the reindex instead
     * of letting the journal drain. The ES store keeps its active pair with the reindex slots
     * cleared, and the OS side is aborted identically.
     */
    @Test
    public void test_phase0Abort_midReindexRollback_preservesOsStoreAndAbortsOsReindex()
            throws Exception {
        seedMidReindexRollbackState();

        APILocator.getContentletIndexAPI().fullReindexAbort();

        // ES: active pair preserved, reindex slots cleared — the normal abort contract.
        final IndiciesInfo esInfo = APILocator.getIndiciesAPI().loadIndicies();
        assertEquals("ES working must stay the active index after abort",
                esPhysical(ES_WORKING), esInfo.getWorking());
        assertEquals("ES live must stay the active index after abort",
                esPhysical(ES_LIVE), esInfo.getLive());

        assertOsReindexAborted();
        Logger.info(this, "✅ Phase-0 abort preserved the OS store and aborted the OS reindex");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds the exact store + cluster state a mid-journal-drain rollback leaves behind:
     * both stores fully populated with active and reindex slots, the partial OS reindex pair
     * physically present on the OS cluster, and the phase flag already rolled back to 0.
     */
    private void seedMidReindexRollbackState() throws Exception {
        setPhase(1);

        // Physical indices: the ES reindex pair (promoted by the switchover) and the partial
        // OS reindex pair (the orphans-to-be). The old active pairs are pointer-only — the
        // switchover never touches them physically.
        esImpl().createIndex(ES_REINDEX_WK, 1);
        esImpl().createIndex(ES_REINDEX_LV, 1);
        osIndexAPI.createIndex(OS_REINDEX_WK, 1);
        osIndexAPI.createIndex(OS_REINDEX_LV, 1);

        pointEsStore(ES_WORKING, ES_LIVE, ES_REINDEX_WK, ES_REINDEX_LV);
        pointOsStore(OS_WORKING, OS_LIVE, OS_REINDEX_WK, OS_REINDEX_LV);

        // The rollback: operator flips the phase flag back to 0 mid-drain.
        setPhase(0);
    }

    /** The fixed post-rollback OS contract shared by the switchover and abort tests. */
    private void assertOsReindexAborted() throws Exception {
        final Optional<VersionedIndices> os =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        assertTrue("OS store record must survive the Phase-0 rollback", os.isPresent());
        assertEquals("OS working row must survive untouched",
                Optional.of(osPhysical(OS_WORKING)), os.get().working());
        assertEquals("OS live row must survive untouched",
                Optional.of(osPhysical(OS_LIVE)), os.get().live());
        assertTrue("OS reindex-working slot must be cleared",
                os.get().reindexWorking().isEmpty());
        assertTrue("OS reindex-live slot must be cleared",
                os.get().reindexLive().isEmpty());

        assertFalse("partial OS reindex-working index must be deleted from the cluster",
                osIndexAPI.indexExists(OS_REINDEX_WK));
        assertFalse("partial OS reindex-live index must be deleted from the cluster",
                osIndexAPI.indexExists(OS_REINDEX_LV));
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    private static ESIndexAPI esImpl() {
        return ((IndexAPIImpl) APILocator.getESIndexAPI()).esImpl();
    }

    /** Cluster-prefixed physical form of an ES logical name (what the ES store persists). */
    private static String esPhysical(final String logicalName) {
        return esImpl().getNameWithClusterIDPrefix(logicalName);
    }

    /** Cluster-prefixed physical form of an OS logical name (what the OS store persists). */
    private String osPhysical(final String logicalName) {
        return osIndexAPI.getNameWithClusterIDPrefix(logicalName);
    }

    /** Points the legacy ES store, preserving nothing — deterministic slot state. */
    private static void pointEsStore(final String working, final String live,
            final String reindexWorking, final String reindexLive) throws DotDataException {
        final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
        builder.setWorking(esPhysical(working));
        builder.setLive(esPhysical(live));
        if (reindexWorking != null) {
            builder.setReindexWorking(esPhysical(reindexWorking));
        }
        if (reindexLive != null) {
            builder.setReindexLive(esPhysical(reindexLive));
        }
        APILocator.getIndiciesAPI().point(builder.build());
    }

    /** Points the OS versioned store directly (names are already {@code .os}-tagged). */
    private void pointOsStore(final String working, final String live,
            final String reindexWorking, final String reindexLive) throws DotDataException {
        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        builder.working(osPhysical(working));
        builder.live(osPhysical(live));
        if (reindexWorking != null) {
            builder.reindexWorking(osPhysical(reindexWorking));
        }
        if (reindexLive != null) {
            builder.reindexLive(osPhysical(reindexLive));
        }
        APILocator.getVersionedIndicesAPI().saveIndices(builder.build());
    }

    private void cleanupTestIndices() {
        for (final String name : List.of(ES_WORKING, ES_LIVE, ES_REINDEX_WK, ES_REINDEX_LV)) {
            Try.run(() -> {
                if (esImpl().indexExists(name)) {
                    esImpl().delete(name);
                }
            }).onFailure(e -> Logger.warn(this,
                    "Cleanup ES index '" + name + "': " + e.getMessage()));
        }
        for (final String name : List.of(OS_WORKING, OS_LIVE, OS_REINDEX_WK, OS_REINDEX_LV)) {
            Try.run(() -> {
                if (osIndexAPI.indexExists(name)) {
                    osIndexAPI.delete(name);
                }
            }).onFailure(e -> Logger.warn(this,
                    "Cleanup OS index '" + name + "': " + e.getMessage()));
        }
    }
}
