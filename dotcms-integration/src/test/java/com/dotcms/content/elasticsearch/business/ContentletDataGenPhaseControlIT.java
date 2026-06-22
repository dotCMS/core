package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOS;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ContentletDataGen.TargetStore;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.client.RequestOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.indices.RefreshRequest;

/**
 * Integration tests for the migration-phase / store control added to {@link ContentletDataGen}
 * (issue #36266).
 *
 * <h2>What this verifies</h2>
 * <p>That a contentlet created through {@link ContentletDataGen} lands in the search store(s)
 * dictated by the per-checkin phase override — ElasticSearch only, OpenSearch only, or both —
 * and that the override restores the previously-configured migration phase afterward.</p>
 *
 * <h2>How it asserts placement</h2>
 * <p>Both stores are bootstrapped once in {@link #setUp()} (phase 1 +
 * {@link ContentletIndexAPI#checkAndInitializeIndex()}), so an ES <em>and</em> an OS working
 * index always exist. Each test then creates a single contentlet with a per-checkin override and
 * checks whether <strong>that exact document</strong> ({@code identifier_languageId_variantId})
 * exists in each provider's working index, queried directly by {@code _id}:
 * {@link RestHighLevelClientProvider} for ES and the OpenSearch client for OS. Targeting the
 * document by id on a known physical index (rather than the high-level read path) is noise-free
 * and sidesteps the {@code inferIndexToHit} bootstrap gap that affects the suite.</p>
 *
 * <h2>Single-cluster vs two-cluster</h2>
 * <p>Scenarios that write content through the ES leg (ES-only and dual-write) require a
 * <strong>separate</strong> ES cluster: the legacy ES {@code RestHighLevelClient} cannot parse an
 * OpenSearch 3.x content bulk-write response, so it cannot index content against an OS backend.
 * In the single-cluster {@code opensearch-upgrade} profile ({@code DOT_ES_ENDPOINTS == OS_ENDPOINTS})
 * those tests skip themselves via {@link org.junit.Assume}. The OS-only placement test and the
 * per-checkin restore test run in both single- and two-cluster setups.</p>
 *
 * <h2>Run command</h2>
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true \
 *       -Dit.test=ContentletDataGenPhaseControlIT
 * </pre>
 *
 * @author Fabrizzio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletDataGenPhaseControlIT extends IntegrationTestBase {

    private static final String SEPARATE_CLUSTERS_REQUIRED =
            "ES content writes require a separate ES cluster — the legacy ES client cannot index "
            + "content against an OpenSearch 3.x backend (single-cluster opensearch-upgrade profile)";

    /** Poll budget for asynchronously-applied shadow writes to become visible. */
    private static final int  AWAIT_ATTEMPTS = 50;
    private static final long AWAIT_SLEEP_MS = 100L;

    @Inject
    private ContentletIndexOperationsOS opsOS;

    @Inject
    private OSClientProvider clientProvider;

    // ── Resolved active working-index physical names (per provider) ──────────
    private String esPhysicalWorking;
    private String osPhysicalWorking;

    // ── Dedicated content type so generated contentlets are isolated ─────────
    private ContentType contentType;

    // ── Saved state restored in @After ───────────────────────────────────────
    @SuppressWarnings("deprecation")
    private IndiciesInfo savedEsInfo;
    private Optional<VersionedIndices> savedOsIndices;
    private String savedPhase;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws Exception {
        savedPhase     = Config.getStringProperty(FLAG_KEY, null);
        savedEsInfo    = APILocator.getIndiciesAPI().loadIndicies();
        savedOsIndices = APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();

        // Bootstrap BOTH stores so an ES and an OS working index exist for the whole test.
        // Phase 1 (dual-write) makes checkAndInitializeIndex() create + register the OS index set
        // alongside the already-present ES one.
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
        contentletIndexAPI().checkAndInitializeIndex();
        APILocator.getVersionedIndicesAPI().clearCache();

        esPhysicalWorking = APILocator.getIndiciesAPI().loadIndicies().getWorking();
        osPhysicalWorking = APILocator.getVersionedIndicesAPI()
                .loadDefaultVersionedIndices()
                .flatMap(VersionedIndices::working)
                .orElseThrow(() -> new IllegalStateException(
                        "OS working index not bootstrapped — cannot verify store placement"));

        contentType = new ContentTypeDataGen().nextPersisted();

        Logger.info(this, "setUp — ES working: " + esPhysicalWorking
                + " | OS working: " + osPhysicalWorking);
    }

    @After
    public void tearDown() {
        Try.run(() -> ContentTypeDataGen.remove(contentType));
        Config.setProperty(FLAG_KEY, savedPhase);
        Try.run(() -> APILocator.getIndiciesAPI().point(savedEsInfo))
           .onFailure(e -> Logger.warn(this, "tearDown: restore ES indices info failed: " + e.getMessage()));
        savedOsIndices.ifPresent(v ->
                Try.run(() -> APILocator.getVersionedIndicesAPI().saveIndices(v))
                   .onFailure(e -> Logger.warn(this, "tearDown: restore OS versioned indices failed: " + e.getMessage())));
        APILocator.getVersionedIndicesAPI().clearCache();
    }

    // =========================================================================
    // Store-placement — store-oriented API (targetStore)
    // =========================================================================

    /**
     * Given: a data-gen with {@code targetStore(BOTH)} (dual-write).
     * When : a contentlet is persisted.
     * Then : the document is present in BOTH the ES and OS working indices.
     */
    @Test
    public void test_targetStoreBoth_indexesInBothStores() {
        assumeFalse(SEPARATE_CLUSTERS_REQUIRED, esSameAsOs());

        final String docId = docId(newGen().targetStore(TargetStore.BOTH).nextPersisted());

        assertTrue("BOTH: document must be present in the ES working index",
                awaitDoc(() -> esHasDoc(docId)));
        assertTrue("BOTH: document must be present in the OS working index",
                awaitDoc(() -> osHasDoc(docId)));
        Logger.info(this, "✅ targetStore(BOTH) — indexed into ES and OS");
    }

    /**
     * Given: a data-gen with {@code targetStore(OS)} (OpenSearch only, phase 3).
     * When : a contentlet is persisted.
     * Then : the document is present in the OS working index and absent from the ES one.
     */
    @Test
    public void test_targetStoreOs_indexesInOsOnly() {
        final String docId = docId(newGen().targetStore(TargetStore.OS).nextPersisted());

        assertTrue("OS-only: document must be present in the OS working index",
                awaitDoc(() -> osHasDoc(docId)));
        refreshAll();
        assertFalse("OS-only: document must be absent from the ES working index",
                esHasDoc(docId));
        Logger.info(this, "✅ targetStore(OS) — indexed into OS only");
    }

    /**
     * Given: a data-gen with {@code targetStore(ES)} (ElasticSearch only, phase 0).
     * When : a contentlet is persisted.
     * Then : the document is present in the ES working index and absent from the OS one.
     */
    @Test
    public void test_targetStoreEs_indexesInEsOnly() {
        assumeFalse(SEPARATE_CLUSTERS_REQUIRED, esSameAsOs());

        final String docId = docId(newGen().targetStore(TargetStore.ES).nextPersisted());

        assertTrue("ES-only: document must be present in the ES working index",
                awaitDoc(() -> esHasDoc(docId)));
        refreshAll();
        assertFalse("ES-only: document must be absent from the OS working index",
                osHasDoc(docId));
        Logger.info(this, "✅ targetStore(ES) — indexed into ES only");
    }

    // =========================================================================
    // Store-placement — phase-oriented API (migrationPhase, the primary surface)
    // =========================================================================

    /**
     * Given: a data-gen with {@code migrationPhase(PHASE_1_DUAL_WRITE_ES_READS)}.
     * When : a contentlet is persisted.
     * Then : the document fans out to BOTH stores — exercising the real phase router.
     */
    @Test
    public void test_migrationPhaseDualWrite_indexesInBothStores() {
        assumeFalse(SEPARATE_CLUSTERS_REQUIRED, esSameAsOs());

        final String docId = docId(
                newGen().migrationPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS).nextPersisted());

        assertTrue("phase 1: document must be present in the ES working index",
                awaitDoc(() -> esHasDoc(docId)));
        assertTrue("phase 1: document must be present in the OS working index",
                awaitDoc(() -> osHasDoc(docId)));
        Logger.info(this, "✅ migrationPhase(PHASE_1) — dual-write fan-out");
    }

    // =========================================================================
    // Per-checkin scope — the override restores the prior phase
    // =========================================================================

    /**
     * Given: the global migration phase is set to PHASE_2 and the data-gen overrides it to
     *        PHASE_3 for a single persist.
     * When : the contentlet is persisted.
     * Then : after persistence the global phase is back to PHASE_2 — the override is scoped to
     *        the checkin and restores the previously-configured value (not blindly cleared).
     */
    @Test
    public void test_override_restoresPriorPhaseAfterPersist() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        newGen().migrationPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY).nextPersisted();

        assertEquals("Override must restore the prior phase after the checkin",
                MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS, MigrationPhase.current());
        Logger.info(this, "✅ per-checkin override restored prior phase (PHASE_2)");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ContentletDataGen newGen() {
        // skipValidation avoids required-field failures for the bare generated content type;
        // WAIT_FOR makes the primary write synchronous so the document is observable right after persist.
        return new ContentletDataGen(contentType.id())
                .skipValidation(true)
                .setPolicy(IndexPolicy.WAIT_FOR);
    }

    /** The ES/OS document {@code _id} dotCMS assigns to an indexed contentlet version. */
    private static String docId(final Contentlet contentlet) {
        return contentlet.getIdentifier() + "_" + contentlet.getLanguageId()
                + "_" + contentlet.getVariantId();
    }

    /** {@code true} when the ES working index holds a document with the given {@code _id}. */
    private boolean esHasDoc(final String docId) {
        return Try.of(() -> RestHighLevelClientProvider.getInstance().getClient()
                        .exists(new GetRequest(esPhysicalWorking, docId), RequestOptions.DEFAULT))
                  .getOrElse(false);
    }

    /** {@code true} when the OS working index holds a document with the given {@code _id}. */
    private boolean osHasDoc(final String docId) {
        return Try.of(() -> clientProvider.getClient().count(CountRequest.of(
                        r -> r.index(osPhysicalWorking).query(q -> q.ids(i -> i.values(docId)))))
                        .count() > 0L)
                  .getOrElse(false);
    }

    /** Refreshes the cluster, then polls {@code present} until it holds or the budget is exhausted. */
    private boolean awaitDoc(final BooleanSupplier present) {
        for (int i = 0; i < AWAIT_ATTEMPTS; i++) {
            refreshAll();
            if (present.getAsBoolean()) {
                return true;
            }
            Try.run(() -> Thread.sleep(AWAIT_SLEEP_MS));
        }
        return present.getAsBoolean();
    }

    /** Refresh the whole cluster so freshly-written docs are searchable (single-cluster profile). */
    private void refreshAll() {
        Try.run(() -> clientProvider.getClient().indices().refresh(RefreshRequest.of(r -> r)))
           .onFailure(e -> Logger.warn(this, "refreshAll failed: " + e.getMessage()));
    }

    private static void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    /**
     * {@code true} when the ES and OS clients point at the same cluster endpoint — the
     * single-cluster {@code opensearch-upgrade} profile. Tests requiring a real, separate ES
     * cluster skip themselves via {@link org.junit.Assume#assumeFalse}.
     */
    private static boolean esSameAsOs() {
        final String esEndpoint = Config.getStringProperty("DOT_ES_ENDPOINTS", "http://localhost:9207");
        final String osEndpoint = Config.getStringProperty("OS_ENDPOINTS", "http://localhost:9201");
        return esEndpoint.trim().equalsIgnoreCase(osEndpoint.trim());
    }

    private static ContentletIndexAPI contentletIndexAPI() {
        return APILocator.getContentletIndexAPI();
    }
}
