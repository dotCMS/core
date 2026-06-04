package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.IndexAPIImpl;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.IndexTag;
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
 * Phase-by-phase contract tests for {@link ContentletIndexAPIImpl#getCurrentIndex()}.
 *
 * <h2>Contract under test</h2>
 * <p>{@code getCurrentIndex()} is a <strong>display/status</strong> getter (it drives the
 * "active" flag in the maintenance UI and the index status endpoints), <em>not</em> the read
 * path. Per product decision it reports the ES pointers until the migration is complete:</p>
 * <ul>
 *   <li><strong>Phases 0, 1 &amp; 2</strong> — {@code getCurrentIndex()} reads the active working
 *       and live slot from {@code legacyIndiciesAPI} (ES pointer store) and strips the cluster
 *       prefix. Even though OS serves reads in Phase&nbsp;2, these status getters keep reporting
 *       ES so the dashboard's "active" flag stays aligned with the indices it shows (OS indices
 *       are hidden before Phase&nbsp;3). OS pointers are irrelevant here.</li>
 *   <li><strong>Phase 3</strong> — {@code getCurrentIndex()} reads from
 *       {@code versionedIndicesAPI} (OS pointer store). ES is decommissioned.</li>
 * </ul>
 *
 * <h2>Full initialization cycle</h2>
 * <p>One test walks the complete ES→OS migration path: activates ES indices in Phase 0 then
 * follows the phase progression through 1→2→3, verifying at each step that
 * {@code getCurrentIndex()} reports the indices the maintenance UI marks active (ES through
 * Phase&nbsp;2, then OS in Phase&nbsp;3).</p>
 *
 * <h2>Run command</h2>
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true \
 *       -Dit.test=ContentletIndexAPIImplGetCurrentIndexIT
 * </pre>
 *
 * @author Fabrizzio Araya
 * @see ContentletIndexAPIImpl#getCurrentIndex()
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletIndexAPIImplPhaseSwitchIT extends IntegrationTestBase {

    // ── Unique run suffix prevents cross-run index name collisions ─────────────
    private static final String RUN_ID =
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /**
     * ES index names — represent indices created during Phase 0 (before OS existed).
     * Using the {@code working_} / {@code live_} prefixes so {@link IndexType} recognises them.
     */
    private static final String ES_WORKING = "working_ges_" + RUN_ID;
    private static final String ES_LIVE    = "live_ges_"    + RUN_ID;

    /**
     * OS index names — represent indices created during migration catch-up.
     * Different name suffix from ES to simulate the timestamp-divergence scenario.
     */
    private static final String OS_WORKING = "working_gos_" + RUN_ID;
    private static final String OS_LIVE    = "live_gos_"    + RUN_ID;

    /**
     * The canonical names {@code getCurrentIndex()} returns for the OS pointers. The {@code .os}
     * tag is part of the name identity end-to-end and is never stripped on return — so the values
     * persisted by {@code activateIndex}/{@code saveIndices} (tagged) are what the getters hand
     * back, minus the cluster prefix. Built via {@link IndexTag} (never {@code + ".os"}).
     * See "The tag is part of the name identity" in {@code docs/backend/OPENSEARCH_MIGRATION.md}.
     */
    private static final String OS_WORKING_TAGGED = IndexTag.OS.tag(OS_WORKING);
    private static final String OS_LIVE_TAGGED     = IndexTag.OS.tag(OS_LIVE);

    // ── Direct OS handle (bypasses the phase router) ───────────────────────────
    @Inject
    private OSIndexAPIImpl osIndexAPI;

    // ── Saved DB state — restored in @After ───────────────────────────────────
    @SuppressWarnings("deprecation")
    private IndiciesInfo savedEsInfo;
    private Optional<VersionedIndices> savedOsIndices;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws Exception {
        savedEsInfo    = APILocator.getIndiciesAPI().loadIndicies();
        savedOsIndices = APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();

        cleanupTestIndices();

        esImpl().createIndex(ES_WORKING, 1);
        esImpl().createIndex(ES_LIVE,    1);

        osIndexAPI.createIndex(OS_WORKING, 1);
        osIndexAPI.createIndex(OS_LIVE,    1);
    }

    @After
    public void tearDown() throws Exception {
        Config.setProperty(FLAG_KEY, null);
        cleanupTestIndices();

        Try.run(() -> APILocator.getIndiciesAPI().point(savedEsInfo))
           .onFailure(e -> Logger.warn(this,
                   "tearDown: could not restore ES pointer: " + e.getMessage()));
        savedOsIndices.ifPresent(v ->
                Try.run(() -> APILocator.getVersionedIndicesAPI().saveIndices(v))
                   .onFailure(e -> Logger.warn(this,
                           "tearDown: could not restore OS pointer: " + e.getMessage())));
    }

    // =========================================================================
    // getCurrentIndex — Phase 0 (ES only)
    // =========================================================================

    /**
     * Given: Phase 0. ES working/live slots are pointed at ES_WORKING / ES_LIVE.
     * When:  getCurrentIndex() is called.
     * Then:  Returns [ES_WORKING, ES_LIVE] — both stripped of their cluster prefix.
     *        OS pointers are absent and must not appear in the result.
     */
    @Test
    public void test_getCurrentIndex_phase0_returnsEsPointers() throws DotDataException {
        setPhase(0);

        // In Phase 0 activateIndex only touches ES DB
        contentletIndexAPI().activateIndex(ES_WORKING);
        contentletIndexAPI().activateIndex(ES_LIVE);

        final List<String> current = contentletIndexAPI().getCurrentIndex();

        assertTrue("working slot must appear (Phase 0 → ES store)",
                current.contains(ES_WORKING));
        assertTrue("live slot must appear (Phase 0 → ES store)",
                current.contains(ES_LIVE));
        assertFalse("OS index must NOT appear in Phase 0", current.contains(OS_WORKING_TAGGED));
        assertFalse("OS live must NOT appear in Phase 0",  current.contains(OS_LIVE_TAGGED));

        Logger.info(this, "✅ getCurrentIndex Phase 0 — ES pointers: " + current);
    }

    // =========================================================================
    // getCurrentIndex — Phase 1 (dual-write, ES reads)
    // =========================================================================

    /**
     * Given: Phase 1 (dual-write, ES reads). ES is pointed at ES_WORKING / ES_LIVE.
     *        OS is pointed at OS_WORKING / OS_LIVE (independent from ES — simulates catchup).
     * When:  getCurrentIndex() is called.
     * Then:  Returns [ES_WORKING, ES_LIVE] — Phase 1 reads are served by ES, not OS.
     *        OS pointers exist in the DB but are irrelevant to getCurrentIndex in this phase.
     */
    @Test
    public void test_getCurrentIndex_phase1_returnsEsPointers() throws DotDataException {
        // Point ES DB to ES indices
        setPhase(0);
        contentletIndexAPI().activateIndex(ES_WORKING);
        contentletIndexAPI().activateIndex(ES_LIVE);

        // Point OS DB to OS indices (independent values, simulating catchup divergence)
        pointOsDirectly(OS_WORKING, OS_LIVE);

        setPhase(1);

        final List<String> current = contentletIndexAPI().getCurrentIndex();

        assertTrue("working slot must be the ES index (Phase 1 reads from ES)",
                current.contains(ES_WORKING));
        assertTrue("live slot must be the ES index (Phase 1 reads from ES)",
                current.contains(ES_LIVE));
        assertFalse("OS working must NOT be returned in Phase 1", current.contains(OS_WORKING_TAGGED));
        assertFalse("OS live must NOT be returned in Phase 1",    current.contains(OS_LIVE_TAGGED));

        Logger.info(this, "✅ getCurrentIndex Phase 1 — ES pointers despite OS being present: "
                + current);
    }

    // =========================================================================
    // getCurrentIndex — Phase 2 (dual-write, OS reads — but display still reports ES)
    // =========================================================================

    /**
     * Given: Phase 2 (dual-write, OS reads). ES is pointed at ES_WORKING / ES_LIVE.
     *        OS is pointed at OS_WORKING / OS_LIVE (separate names — catchup scenario).
     * When:  getCurrentIndex() is called.
     * Then:  Returns [ES_WORKING, ES_LIVE]. Although Phase 2 routes <em>reads</em> to OS, the
     *        status getters keep reporting ES until the migration completes (Phase 3), so the
     *        maintenance dashboard's "active" flag stays aligned with the visible ES indices.
     *        The OS pointers exist in the DB but must NOT appear here.
     */
    @Test
    public void test_getCurrentIndex_phase2_returnsEsPointers() throws DotDataException {
        // Point ES DB independently
        setPhase(0);
        contentletIndexAPI().activateIndex(ES_WORKING);
        contentletIndexAPI().activateIndex(ES_LIVE);

        // Point OS DB to OS indices directly (bypasses the phase mirror)
        pointOsDirectly(OS_WORKING, OS_LIVE);

        setPhase(2);

        final List<String> current = contentletIndexAPI().getCurrentIndex();

        assertTrue("working slot must stay the ES index (Phase 2 display reports ES)",
                current.contains(ES_WORKING));
        assertTrue("live slot must stay the ES index (Phase 2 display reports ES)",
                current.contains(ES_LIVE));
        assertFalse("OS working must NOT be returned in Phase 2 display",
                current.contains(OS_WORKING_TAGGED));
        assertFalse("OS live must NOT be returned in Phase 2 display",
                current.contains(OS_LIVE_TAGGED));

        Logger.info(this, "✅ getCurrentIndex Phase 2 — ES pointers reported (OS serves reads): "
                + current);
    }

    // =========================================================================
    // getCurrentIndex — Phase 3 (OS only)
    // =========================================================================

    /**
     * Given: Phase 3 (OS only). OS is pointed at OS_WORKING / OS_LIVE.
     *        ES DB is not consulted (ES decommissioned).
     * When:  getCurrentIndex() is called.
     * Then:  Returns [OS_WORKING, OS_LIVE] — only OS pointers are relevant.
     */
    @Test
    public void test_getCurrentIndex_phase3_returnsOsPointers() throws DotDataException {
        // Point OS DB first (in Phase 3 activateIndex only touches OS)
        setPhase(3);
        contentletIndexAPI().activateIndex(OS_WORKING);
        contentletIndexAPI().activateIndex(OS_LIVE);

        final List<String> current = contentletIndexAPI().getCurrentIndex();

        assertTrue("working slot must be the OS index (Phase 3 — OS only)",
                current.contains(OS_WORKING_TAGGED));
        assertTrue("live slot must be the OS index (Phase 3 — OS only)",
                current.contains(OS_LIVE_TAGGED));

        Logger.info(this, "✅ getCurrentIndex Phase 3 — OS pointers returned: " + current);
    }

    // =========================================================================
    // Full initialization cycle — Phase 0 → 1 → 2 → 3
    // =========================================================================

    /**
     * Full ES→OS migration cycle: activates ES indices in Phase 0 then walks through all
     * phases, verifying that {@code getCurrentIndex()} returns pointers from the correct
     * read provider at each step.
     *
     * <pre>
     * Phase 0: activate ES_WORKING + ES_LIVE → getCurrentIndex returns [ES_WORKING, ES_LIVE]
     * Phase 1: same ES pointers, OS DB independent → getCurrentIndex still returns ES names
     * Phase 2: OS DB set to OS_WORKING + OS_LIVE → getCurrentIndex STILL returns ES names
     *          (display reports ES until migration completes, even though OS serves reads)
     * Phase 3: ES decommissioned, OS pointers unchanged → getCurrentIndex returns OS names
     * </pre>
     *
     * <p>This is the canonical "happy path" of the migration — no rollback, no errors.</p>
     */
    @Test
    public void test_fullInitializationCycle_getCurrentIndex_followsReadProvider()
            throws DotDataException {

        // ── Phase 0: activate ES indices ──────────────────────────────────────
        setPhase(0);
        contentletIndexAPI().activateIndex(ES_WORKING);
        contentletIndexAPI().activateIndex(ES_LIVE);

        List<String> current = contentletIndexAPI().getCurrentIndex();
        assertTrue("Phase 0: working slot must be ES index",  current.contains(ES_WORKING));
        assertTrue("Phase 0: live slot must be ES index",     current.contains(ES_LIVE));
        assertEquals("Phase 0: result must have exactly 2 elements", 2, current.size());
        Logger.info(this, "Phase 0 ✅ getCurrentIndex: " + current);

        // ── Phase 1: dual-write starts, reads still from ES ───────────────────
        // We also set OS DB to OS indices to simulate a real catchup scenario
        // (OS indices were created with a later timestamp and different name).
        pointOsDirectly(OS_WORKING, OS_LIVE);
        setPhase(1);

        current = contentletIndexAPI().getCurrentIndex();
        assertTrue("Phase 1: working slot must still be ES index (ES reads)",
                current.contains(ES_WORKING));
        assertTrue("Phase 1: live slot must still be ES index (ES reads)",
                current.contains(ES_LIVE));
        assertFalse("Phase 1: OS working must NOT appear (reads still served by ES)",
                current.contains(OS_WORKING_TAGGED));
        Logger.info(this, "Phase 1 ✅ getCurrentIndex: " + current);

        // ── Phase 2: reads switch to OS, but the display getter still reports ES ──
        setPhase(2);

        current = contentletIndexAPI().getCurrentIndex();
        assertTrue("Phase 2: working slot must still be ES index (display reports ES)",
                current.contains(ES_WORKING));
        assertTrue("Phase 2: live slot must still be ES index (display reports ES)",
                current.contains(ES_LIVE));
        assertFalse("Phase 2: OS working must NOT appear in the display getter",
                current.contains(OS_WORKING_TAGGED));
        Logger.info(this, "Phase 2 ✅ getCurrentIndex: " + current);

        // ── Phase 3: ES decommissioned, OS remains primary ────────────────────
        setPhase(3);

        current = contentletIndexAPI().getCurrentIndex();
        assertTrue("Phase 3: working slot must still be OS index", current.contains(OS_WORKING_TAGGED));
        assertTrue("Phase 3: live slot must still be OS index",    current.contains(OS_LIVE_TAGGED));
        assertFalse("Phase 3: ES working must NOT appear (ES decommissioned)",
                current.contains(ES_WORKING));
        Logger.info(this, "Phase 3 ✅ getCurrentIndex: " + current);
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    /**
     * Given: Phase 0. Only the working slot is populated in the ES store (live is null).
     * When:  getCurrentIndex() is called.
     * Then:  Returns only [ES_WORKING]. The null live slot is silently omitted — no NPE.
     */
    @Test
    public void test_getCurrentIndex_phase0_nullLiveSlot_returnsWorkingOnly()
            throws DotDataException {
        setPhase(0);
        contentletIndexAPI().activateIndex(ES_WORKING);
        // Intentionally do NOT activate the live slot — leave it null (or whatever
        // the app state is after setUp).

        // Directly clear the live slot in ES DB so we can assert on a clean state.
        final IndiciesInfo esInfo = APILocator.getIndiciesAPI().loadIndicies();
        final IndiciesInfo withNullLive = IndiciesInfo.Builder.copy(esInfo)
                .setLive(null)
                .build();
        APILocator.getIndiciesAPI().point(withNullLive);

        final List<String> current = contentletIndexAPI().getCurrentIndex();

        assertTrue("Working slot must be present", current.contains(ES_WORKING));
        assertFalse("Null live slot must not add an empty string",
                current.contains("") || current.contains(null));

        Logger.info(this, "✅ getCurrentIndex Phase 0 null-live — result: " + current);
    }

    /**
     * Given: Phase 2, ES pointers set. The OS store is whatever the environment left it —
     *        this test deliberately does NOT point it.
     * When:  getCurrentIndex() is called.
     * Then:  Returns the ES pointers regardless of OS store state. Because the Phase 2 display
     *        contract reads the ES store (see {@code ContentletIndexAPIImpl#displayUsesOsStore}),
     *        the OS store is never consulted here — there is no NPE and no dependency on OS
     *        pointers being present, even though OS already serves reads in Phase 2.
     */
    @Test
    public void test_getCurrentIndex_phase2_reportsEsRegardlessOfOsStore()
            throws DotDataException {
        setPhase(0);
        contentletIndexAPI().activateIndex(ES_WORKING);
        contentletIndexAPI().activateIndex(ES_LIVE);

        // Intentionally do NOT point the OS store: the Phase 2 display getter must report ES
        // independently of whatever the OS store contains (set, empty, or stale).
        setPhase(2);

        final List<String> current = contentletIndexAPI().getCurrentIndex();

        assertTrue("Phase 2 display must report the ES working slot regardless of OS store",
                current.contains(ES_WORKING));
        assertTrue("Phase 2 display must report the ES live slot regardless of OS store",
                current.contains(ES_LIVE));
        assertFalse("Phase 2 display must not contain empty/null entries",
                current.contains("") || current.contains(null));

        Logger.info(this, "✅ getCurrentIndex Phase 2 — reports ES regardless of OS store: "
                + current);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    private static ContentletIndexAPI contentletIndexAPI() {
        return APILocator.getContentletIndexAPI();
    }

    private static ESIndexAPI esImpl() {
        return ((IndexAPIImpl) APILocator.getESIndexAPI()).esImpl();
    }

    /**
     * Directly points the OS versioned-indices store to the given logical index names,
     * bypassing the phase router and the mirror logic in {@code activateIndex}.
     * Used to set up independent OS pointers that differ from the ES pointers —
     * simulating the timestamp-divergence scenario that occurs in a real migration catchup.
     */
    private void pointOsDirectly(final String workingLogical, final String liveLogical) {
        final String workingPhysical = ((IndexAPIImpl) APILocator.getESIndexAPI())
                .osImpl().getNameWithClusterIDPrefix(workingLogical);
        final String livePhysical = ((IndexAPIImpl) APILocator.getESIndexAPI())
                .osImpl().getNameWithClusterIDPrefix(liveLogical);

        final VersionedIndices record = VersionedIndicesImpl.builder()
                .working(workingPhysical)
                .live(livePhysical)
                .build();
        Try.run(() -> APILocator.getVersionedIndicesAPI().saveIndices(record))
           .onFailure(e -> {
               throw new RuntimeException("pointOsDirectly failed: " + e.getMessage(), e);
           });
    }

    private void cleanupTestIndices() {
        final ESIndexAPI esIndex = esImpl();
        for (final String name : List.of(ES_WORKING, ES_LIVE)) {
            Try.run(() -> {
                if (esIndex.indexExists(name)) esIndex.delete(name);
            }).onFailure(e -> Logger.warn(this,
                    "Cleanup ES index '" + name + "': " + e.getMessage()));
        }
        for (final String name : List.of(OS_WORKING, OS_LIVE)) {
            Try.run(() -> {
                if (osIndexAPI.indexExists(name)) osIndexAPI.delete(name);
            }).onFailure(e -> Logger.warn(this,
                    "Cleanup OS index '" + name + "': " + e.getMessage()));
        }
    }
}