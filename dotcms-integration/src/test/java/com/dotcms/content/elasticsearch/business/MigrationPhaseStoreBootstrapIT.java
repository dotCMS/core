package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.MigrationPhaseStoreBootstrap;
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
 * Integration tests for {@link MigrationPhaseStoreBootstrap} — the phase-aware bootstrap that lets
 * the <em>whole</em> indexing flow run under a chosen ES&rarr;OS {@link MigrationPhase} (issue
 * #36266).
 *
 * <h2>What this verifies</h2>
 * <p>That, once the active migration phase is set and the stores are bootstrapped via
 * {@link MigrationPhaseStoreBootstrap}, a contentlet created through the <strong>ordinary</strong>
 * {@link ContentletDataGen} (no per-checkin override) is routed into the search store(s) the phase
 * dictates — exactly as production routes every index operation. No data-gen instrumentation is
 * involved: the phase is global config and the phase router does the rest.</p>
 *
 * <h2>How it asserts placement</h2>
 * <p>Both stores are bootstrapped once in {@link #setUp()} via
 * {@link MigrationPhaseStoreBootstrap#ensureStoresForCurrentPhase()} under phase 1, so an ES
 * <em>and</em> an OS working index always exist (and the bootstrap fails hard if the OS service is
 * unreachable). Each test then sets the phase, persists a single contentlet, and checks whether
 * <strong>that exact document</strong> ({@code identifier_languageId_variantId}) exists in each
 * provider's working index, queried directly by {@code _id}: {@link RestHighLevelClientProvider}
 * for ES and the OpenSearch client for OS. Targeting the document by id on a known physical index
 * (rather than the high-level read path) is noise-free and sidesteps the {@code inferIndexToHit}
 * bootstrap gap that affects the suite.</p>
 *
 * <h2>Single-cluster vs two-cluster</h2>
 * <p>Scenarios that write content through the ES leg (the phase-1 dual-write test) require a
 * <strong>separate</strong> ES cluster: the legacy ES {@code RestHighLevelClient} cannot parse an
 * OpenSearch 3.x content bulk-write response, so it cannot index content against an OS backend.
 * In the single-cluster {@code opensearch-upgrade} profile ({@code DOT_ES_ENDPOINTS == OS_ENDPOINTS})
 * that test skips itself via {@link org.junit.Assume}. The phase-3 OpenSearch-only placement test
 * runs in both single- and two-cluster setups.</p>
 *
 * <h2>Run command</h2>
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration \
 *       -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true \
 *       -Dit.test=MigrationPhaseStoreBootstrapIT
 * </pre>
 *
 * @author Fabrizzio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class MigrationPhaseStoreBootstrapIT extends IntegrationTestBase {

    private static final String SEPARATE_CLUSTERS_REQUIRED =
            "ES content writes require a separate ES cluster — the legacy ES client cannot index "
            + "content against an OpenSearch 3.x backend (single-cluster opensearch-upgrade profile)";

    /** Poll budget for asynchronously-applied shadow writes to become visible. */
    private static final int  AWAIT_ATTEMPTS = 50;
    private static final long AWAIT_SLEEP_MS = 100L;

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
        // Phase 1 (dual-write) makes the bootstrap create + register the OS index set alongside
        // the already-present ES one. ensureStoresForCurrentPhase() additionally fails hard, with a
        // clear message, when the phase needs OpenSearch but the OS service is unreachable.
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
        MigrationPhaseStoreBootstrap.ensureStoresForCurrentPhase();

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
    // Store placement driven purely by the active migration phase
    // =========================================================================

    /**
     * Given: the active phase is PHASE_3_OPENSEARCH_ONLY and both stores are bootstrapped.
     * When : a contentlet is persisted through the ordinary data-gen.
     * Then : the document is present in the OS working index and absent from the ES one — the
     *        phase router sent the write to OpenSearch only, with no data-gen instrumentation.
     */
    @Test
    public void test_phase3_indexesIntoOpenSearchOnly() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        final String docId = docId(newContentlet());

        assertTrue("phase 3: document must be present in the OS working index",
                awaitDoc(() -> osHasDoc(docId)));
        refreshAll();
        assertFalse("phase 3: document must be absent from the ES working index",
                esHasDoc(docId));
        Logger.info(this, "✅ phase 3 — ordinary flow indexed into OpenSearch only");
    }

    /**
     * Given: the active phase is PHASE_1_DUAL_WRITE_ES_READS and both stores are bootstrapped.
     * When : a contentlet is persisted through the ordinary data-gen.
     * Then : the document fans out to BOTH stores. Requires two separate clusters — skipped in the
     *        single-cluster profile (the legacy ES client cannot write content to an OS 3.x backend).
     */
    @Test
    public void test_phase1_dualWriteIndexesIntoBothStores() {
        assumeFalse(SEPARATE_CLUSTERS_REQUIRED, esSameAsOs());
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);

        final String docId = docId(newContentlet());

        assertTrue("phase 1: document must be present in the ES working index",
                awaitDoc(() -> esHasDoc(docId)));
        assertTrue("phase 1: document must be present in the OS working index",
                awaitDoc(() -> osHasDoc(docId)));
        Logger.info(this, "✅ phase 1 — ordinary flow dual-wrote to ES and OS");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Persists a contentlet of the dedicated test type through the ordinary {@link ContentletDataGen}.
     * {@code skipValidation} avoids required-field failures for the bare generated content type;
     * {@code WAIT_FOR} makes the primary write synchronous so the document is observable right after
     * persist.
     */
    private Contentlet newContentlet() {
        return new ContentletDataGen(contentType.id())
                .skipValidation(true)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
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
}
