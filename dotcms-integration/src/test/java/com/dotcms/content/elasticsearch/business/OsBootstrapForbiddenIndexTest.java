package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexAPIImpl;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.MigrationHaltReport;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.IndexStartResult;
import com.dotcms.content.index.opensearch.IndexStartupValidator;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl.ConnectionFailureKind;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression test for issue #36222 (QA-G16, TC-056): an OpenSearch index-creation rejection must
 * degrade gracefully in a dual-write phase instead of aborting index initialisation.
 *
 * <h2>The bug</h2>
 * <p>In managed cloud, dotCMS reaches OS as a restricted user whose role grants index permissions on
 * {@code cluster_&lt;customer&gt;*}. dotCMS names its indices
 * {@code cluster_&lt;DOT_DOTCMS_CLUSTER_ID&gt;.working_&lt;ts&gt;.os}, so a cluster id that does
 * not start with the provisioned customer name makes OS reject every create with {@code HTTP 403}.</p>
 *
 * <p>That rejection arrives as an unchecked {@link DotStateException} raised by
 * {@code OSIndexAPIImpl.createIndex} — <em>after</em> the OS connection gate has passed, because the
 * gate only probes {@code GET /}, which the restricted user is allowed to call. It escaped the
 * {@code catch (IOException)} in {@code bootstrapAndPointOS} and propagated out of
 * {@code checkAndInitializeIndex()} / {@code fullReindexStart()}, so in Phase 1 an OS-only
 * permission problem took down index initialisation for ES as well (QA saw a 400 from the reindex
 * endpoint and no indices at all).</p>
 *
 * <h2>Expected behaviour</h2>
 * <ul>
 *   <li><strong>Phase 1 / 2</strong> — OS is a shadow store: absorb the failure, halt the migration
 *       (phase reset to 0, ES-only), and leave the OS index store untouched (no pointer to indices
 *       that were never created).</li>
 *   <li><strong>Phase 3</strong> — OS is the primary store: fail loudly as {@link DotDataException}
 *       and never auto-roll back the phase.</li>
 * </ul>
 *
 * <h2>Environment</h2>
 * <p>The OS provider is a stub that rejects creates the way a permission-scoped OS role does, so no
 * security-enabled cluster is required and the test is deterministic in the single-cluster
 * {@code opensearch-upgrade} profile. Everything else is real: the DB-backed
 * {@code VersionedIndices} store, the ES provider and the {@code IndexAPI} router come from
 * {@link APILocator}. {@code OS_ENDPOINTS} is pointed at an unused address for the duration of the
 * test so the config-only endpoint-separation gate is genuinely satisfied (the stub never opens a
 * connection); the assertion on that gate keeps the test from passing for the wrong reason.</p>
 *
 * <pre>
 *   ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
 *       -Dopensearch.upgrade.test=true -Dit.test=OsBootstrapForbiddenIndexTest
 * </pre>
 *
 * @author Fabrizzio Araya
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class OsBootstrapForbiddenIndexTest extends IntegrationTestBase {

    private static final String OS_ENDPOINTS_KEY = "OS_ENDPOINTS";

    /** Address that is never connected to — the stub OS provider rejects before any I/O. */
    private static final String UNUSED_OS_ENDPOINT = "https://127.0.0.1:19201";

    private String originalPhase;
    private String[] originalOsEndpoints;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void rememberConfig() {
        originalPhase = Config.getStringProperty(FLAG_KEY, null);
        originalOsEndpoints = Config.getStringArrayProperty(OS_ENDPOINTS_KEY, null);
    }

    @After
    public void restoreConfig() {
        Config.setProperty(FLAG_KEY, originalPhase);
        Config.setProperty(OS_ENDPOINTS_KEY, originalOsEndpoints);
    }

    /**
     * Given : Phase 1 (dual-write, ES reads) against an OS provider that rejects index creation with
     *         a 403, exactly as a role scoped to another cluster-id prefix does.
     * When  : the OS working/live bootstrap runs.
     * Then  : it completes without throwing, the migration is halted (ES-only) and no OS store
     *         pointer is written for the indices that were never created.
     */
    @Test
    public void phase1_forbiddenOsCreate_doesNotAbortBootstrap_andHaltsMigration()
            throws DotDataException {

        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
        warmUpOsClient();
        Config.setProperty(OS_ENDPOINTS_KEY, new String[]{UNUSED_OS_ENDPOINT});
        assertTrue("The endpoint-separation gate must pass, otherwise the OS bootstrap is skipped"
                        + " before reaching the create call and this test proves nothing",
                IndexStartupValidator.endpointsAreSeparate());

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        final String workingIndex = IndexType.WORKING.getPrefix() + "_" + timeStamp;
        final String liveIndex = IndexType.LIVE.getPrefix() + "_" + timeStamp;

        // Before the fix this threw DotStateException("Failed to create index: ...").
        final boolean osPointed =
                newApiWithForbiddenOs().bootstrapAndPointOS(workingIndex, liveIndex);

        assertFalse("The bootstrap must report that no OS index was registered, so callers do not"
                        + " advertise an index that does not exist",
                osPointed);
        assertEquals("A shadow-phase OS bootstrap failure must halt the migration (ES-only),"
                        + " not abort index initialisation",
                MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
        assertOsStoreDoesNotPointAt(timeStamp);

        Logger.info(this, "✅ Phase 1: forbidden OS create degraded to ES-only for " + workingIndex);
    }

    /**
     * Given : Phase 3 (OS only — ES decommissioned) against the same rejecting OS provider.
     * When  : the OS working/live bootstrap runs.
     * Then  : it fails loudly as a {@link DotDataException} and the phase is left untouched — there
     *         is no ES to silently fall back to.
     */
    @Test
    public void phase3_forbiddenOsCreate_failsLoudly_andKeepsPhase() {

        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        final String workingIndex = IndexType.WORKING.getPrefix() + "_" + timeStamp;
        final String liveIndex = IndexType.LIVE.getPrefix() + "_" + timeStamp;

        try {
            newApiWithForbiddenOs().bootstrapAndPointOS(workingIndex, liveIndex);
            fail("In Phase 3 OS is the primary store — a failed OS bootstrap must propagate");
        } catch (final DotDataException expected) {
            Logger.info(this, "✅ Phase 3: forbidden OS create failed loudly: "
                    + expected.getMessage());
        }

        assertEquals("Phase 3 must never be auto-rolled back to ES",
                MigrationPhase.PHASE_3_OPENSEARCH_ONLY, MigrationPhase.current());
    }

    /**
     * Given : Phase 1 and a full reindex whose OS reindex indices are rejected with 403.
     * When  : the reindex slots are created and registered.
     * Then  : the ES reindex slots are registered as usual, and the OS store is left WITHOUT reindex
     *         pointers — pointing it at indices the provider rejected would make the reindex write to
     *         names that do not exist, so OpenSearch would auto-create a dynamically-mapped twin that
     *         the switchover later promotes to active.
     */
    @Test
    public void phase1_forbiddenOsCreate_doesNotRegisterOsReindexSlots() throws DotDataException {

        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
        warmUpOsClient();
        Config.setProperty(OS_ENDPOINTS_KEY, new String[]{UNUSED_OS_ENDPOINT});

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        final IndiciesInfo originalEsIndices = APILocator.getIndiciesAPI().loadIndicies();

        try {
            newApiWithForbiddenOs().initAndPointReindex(timeStamp);

            final IndiciesInfo esAfter = APILocator.getIndiciesAPI().loadIndicies();
            assertTrue("The ES reindex slots must still be registered: the Elasticsearch reindex is"
                            + " unaffected by an OpenSearch rejection. Got: " + esAfter.getReindexWorking(),
                    esAfter.getReindexWorking() != null
                            && esAfter.getReindexWorking().contains(timeStamp));

            assertOsStoreDoesNotPointAt(timeStamp);

            Logger.info(this, "✅ Phase 1: OS reindex slots left unregistered after a forbidden create");
        } finally {
            // Restore the ES store and drop the reindex indices this test created.
            Try.run(() -> APILocator.getIndiciesAPI().point(originalEsIndices));
            Try.run(() -> APILocator.getContentletIndexAPI()
                    .delete(IndexType.REINDEX_WORKING.getPrefix() + "_" + timeStamp));
            Try.run(() -> APILocator.getContentletIndexAPI()
                    .delete(IndexType.REINDEX_LIVE.getPrefix() + "_" + timeStamp));
        }
    }

    /**
     * Given : Phase 1, a full reindex requested through {@code fullReindexStart()}, and an OS store
     *         pointing at indices the restricted user may not even probe — so {@code indexReady()} is
     *         false and the reindex branch is skipped.
     * When  : the reindex is triggered.
     * Then  : the OS failure is absorbed, the migration is halted, and the full reindex the caller
     *         asked for still runs on Elasticsearch — new reindex slots and a real ES suffix.
     *
     * <p>Before the fix this returned a bootstrap result with an empty ES suffix and created no index
     * at all: the UI showed a progress bar that completed, no error, and nothing had happened. The
     * operator had to press Reindex a second time — which worked only because the first press had
     * silently switched the migration off (issue #36222, QA round 3).</p>
     */
    @Test
    public void phase1_forbiddenOsCreate_stillRunsTheFullReindexOnElasticsearch()
            throws DotDataException {

        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);
        warmUpOsClient();
        Config.setProperty(OS_ENDPOINTS_KEY, new String[]{UNUSED_OS_ENDPOINT});

        final IndiciesInfo originalEsIndices = APILocator.getIndiciesAPI().loadIndicies();
        final Optional<VersionedIndices> originalOsStore =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();
        final String missingSuffix = "os_missing_" + System.currentTimeMillis();

        try {
            // An OS store that points at indices which do not exist makes indexReadyOS() false, which
            // is exactly what a 403 on the exists-probe produces: unreadable is indistinguishable from
            // absent (OSIndexAPIImpl.indexExists swallows and returns false).
            APILocator.getVersionedIndicesAPI().saveIndices(VersionedIndicesImpl.builder()
                    .working(IndexType.WORKING.getPrefix() + "_" + missingSuffix)
                    .live(IndexType.LIVE.getPrefix() + "_" + missingSuffix)
                    .version(VersionedIndices.OPENSEARCH_3X)
                    .build());

            final IndexStartResult result = newApiWithForbiddenOs().fullReindexStart();

            assertTrue("The full reindex must still run on Elasticsearch: an empty ES suffix means"
                            + " the request was silently downgraded to a bootstrap that did nothing",
                    UtilMethods.isSet(result.indexSuffixES()));
            assertEquals("With the migration halted there is no OpenSearch slot to advertise",
                    "", result.indexSuffixOS());

            final IndiciesInfo esAfter = APILocator.getIndiciesAPI().loadIndicies();
            assertTrue("The ES reindex slots must be registered for the suffix the reindex reported."
                            + " Got: " + esAfter.getReindexWorking(),
                    esAfter.getReindexWorking() != null
                            && esAfter.getReindexWorking().contains(result.indexSuffixES()));

            assertEquals("The shadow-phase OS failure must still halt the migration (ES-only)",
                    MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());

            final Optional<MigrationHaltReport> halt = MigrationHaltReport.last();
            assertTrue("The halt must leave a reason behind: without it the REST layer can say that"
                            + " the migration stopped but never why", halt.isPresent());
            assertEquals("A 403 on create must be classified as an authorization problem, so the"
                            + " operator is told to fix the role instead of reindexing again",
                    ConnectionFailureKind.AUTH_FORBIDDEN.name(), halt.get().cause());

            Logger.info(this, "✅ Phase 1: forbidden OS create still produced an ES full reindex ("
                    + result.indexSuffixES() + ") and reported " + halt.get().cause());
        } finally {
            Try.run(() -> APILocator.getIndiciesAPI().point(originalEsIndices));
            Try.run(() -> APILocator.getContentletIndexAPI()
                    .delete(IndexType.REINDEX_WORKING.getPrefix() + "_" + missingSuffix));
            if (originalOsStore.isPresent()) {
                Try.run(() -> APILocator.getVersionedIndicesAPI()
                        .saveIndices(originalOsStore.get()));
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Real ES provider + a stub OS provider that reports the cluster as reachable (mirroring the
     * connection gate a restricted user passes) and then rejects the create with a 403.
     */
    private ContentletIndexAPIImpl newApiWithForbiddenOs() {
        final IndexAPI reachableOsIndexAPI = mock(IndexAPI.class);
        when(reachableOsIndexAPI.waitUtilIndexReady()).thenReturn(true);

        final ContentletIndexOperations forbiddenOs = mock(ContentletIndexOperations.class);
        when(forbiddenOs.indexAPI()).thenReturn(reachableOsIndexAPI);
        when(forbiddenOs.toPhysicalName(anyString()))
                .thenAnswer(invocation -> "cluster_acme-test."
                        + invocation.getArgument(0, String.class) + ".os");
        try {
            when(forbiddenOs.createContentIndex(anyString(), anyInt()))
                    .thenThrow(forbiddenCreate());
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to stub the OS provider", e);
        }

        return new ContentletIndexAPIImpl(new ContentletIndexOperationsES(), forbiddenOs);
    }

    /** The 403 an index-scoped OS role raises on a create outside its index pattern. */
    private static DotStateException forbiddenCreate() {
        return new DotStateException(
                "Failed to create index: cluster_acme-test.working.os",
                new RuntimeException("OpenSearch exception [type=security_exception,"
                        + " reason=no permissions for [indices:admin/create] and User"
                        + " [name=dotcms-es-user, roles=[dotcms-role]]] status: 403"));
    }

    private void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    /**
     * Forces the shared OS client to be built with the profile's real endpoints <em>before</em>
     * {@code OS_ENDPOINTS} is redirected for the separation gate, so a lazily-created client can
     * never capture the unused address and leak a broken singleton into the rest of the suite.
     */
    private void warmUpOsClient() {
        final IndexAPIImpl indexAPI = (IndexAPIImpl) APILocator.getESIndexAPI();
        Try.run(() -> indexAPI.osImpl().indexExists("os_client_warmup_probe"))
                .onFailure(e -> Logger.debug(this,
                        "OS client warm-up probe failed (harmless): " + e.getMessage()));
    }

    /**
     * Asserts the OS index store carries no pointer to the indices whose creation was rejected —
     * a pointer to a non-existent index is what {@code initOSCatchup} would later treat as
     * authoritative.
     */
    private void assertOsStoreDoesNotPointAt(final String timeStamp) throws DotDataException {
        final Optional<VersionedIndices> stored =
                APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices();

        stored.ifPresent(indices -> {
            assertFalse("The OS store must not point at a working index that was never created",
                    indices.working().filter(name -> name.contains(timeStamp)).isPresent());
            assertFalse("The OS store must not point at a live index that was never created",
                    indices.live().filter(name -> name.contains(timeStamp)).isPresent());
            assertFalse("The OS store must not point at a reindex-working index that was never created",
                    indices.reindexWorking().filter(name -> name.contains(timeStamp)).isPresent());
            assertFalse("The OS store must not point at a reindex-live index that was never created",
                    indices.reindexLive().filter(name -> name.contains(timeStamp)).isPresent());
        });
    }
}