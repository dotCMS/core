package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.ContentletIndexOperations;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link ContentletIndexAPIImpl#handleOsBootstrapFailure(String, String, Exception)}
 * — the phase-aware outcome policy for a hard OpenSearch index-bootstrap failure (issue #36222).
 *
 * <h2>What regressed</h2>
 * <p>A restricted OS user whose role grants index permissions on {@code cluster_&lt;customer&gt;*} is
 * rejected with {@code HTTP 403} when {@code DOT_DOTCMS_CLUSTER_ID} produces a different prefix.
 * The rejection arrives as an unchecked {@link DotStateException} <em>after</em> the connection gate
 * has passed (the gate only probes {@code GET /}, which the user is allowed to call), so it escaped
 * the {@code catch (IOException)} in {@code bootstrapAndPointOS} and aborted index initialisation
 * for <em>both</em> providers — including ES, the authoritative store in a dual-write phase.</p>
 *
 * <h2>Contract under test</h2>
 * <ul>
 *   <li>Phases 1 and 2 (OS is a shadow store): the failure is absorbed — the migration is halted
 *       (phase reset to 0, ES-only) and the caller is told to skip {@code pointOS}.</li>
 *   <li>Phase 3 (OS is the primary store): the failure is <em>not</em> absorbed — there is no ES to
 *       fall back to, so the caller must propagate it and the phase must stay untouched.</li>
 *   <li>The degradation is not specific to authorization errors: any hard failure in a shadow phase
 *       degrades the same way (only the logged remediation differs, driven by the classifier).</li>
 * </ul>
 *
 * @author Fabrizzio Araya
 */
public class ContentletIndexAPIImplOsBootstrapFailureTest {

    private static final String WORKING = "working_T0";
    private static final String LIVE = "live_T0";

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    /**
     * Builds an instance solely so the package-private handler can be invoked. Only
     * {@code operationsOS.toPhysicalName()} is consulted by the handler (to name the offending
     * indices in the log); every other collaborator is irrelevant to the decision.
     */
    private static ContentletIndexAPIImpl newApi() {
        final ContentletIndexOperations operationsOS = mock(ContentletIndexOperations.class);
        when(operationsOS.toPhysicalName(anyString()))
                .thenAnswer(invocation -> "cluster_acme-test."
                        + invocation.getArgument(0, String.class) + ".os");

        return new ContentletIndexAPIImpl(
                mock(ContentletIndexOperations.class),
                operationsOS,
                mock(IndexAPI.class),
                mock(IndiciesAPI.class),
                mock(VersionedIndicesAPI.class));
    }

    /** The 403 an index-scoped OS role raises on a create outside its index pattern. */
    private static DotStateException forbiddenCreate() {
        return new DotStateException(
                "Failed to create index: cluster_acme-test.working_T0.os",
                new RuntimeException("OpenSearch exception [type=security_exception,"
                        + " reason=no permissions for [indices:admin/create] and User"
                        + " [name=dotcms-es-user, roles=[dotcms-role]]] status: 403"));
    }

    private static void setPhase(final MigrationPhase phase) {
        Config.setProperty(FLAG_KEY, String.valueOf(phase.ordinal()));
    }

    /**
     * Given : Phase 1 (dual-write, ES reads) and an OS create rejected with 403.
     * When  : the bootstrap failure is handled.
     * Then  : it is absorbed (caller skips pointOS) and the migration is halted so dotCMS keeps
     *         running on ES only.
     */
    @Test
    public void phase1_forbiddenCreate_isAbsorbed_andMigrationHalted() {
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);

        final boolean absorbed = newApi()
                .handleOsBootstrapFailure(WORKING, LIVE, forbiddenCreate());

        assertTrue("A shadow-phase OS bootstrap failure must be absorbed, not propagated",
                absorbed);
        assertEquals("The migration must be halted so dotCMS falls back to ES-only",
                MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }

    /**
     * Given : Phase 2 (dual-write, OS reads) and an OS create rejected with 403.
     * When  : the bootstrap failure is handled.
     * Then  : same as Phase 1 — ES is still active, so the failure is absorbed and the migration
     *         halted (reads revert to ES together with the phase reset).
     */
    @Test
    public void phase2_forbiddenCreate_isAbsorbed_andMigrationHalted() {
        setPhase(MigrationPhase.PHASE_2_DUAL_WRITE_OS_READS);

        final boolean absorbed = newApi()
                .handleOsBootstrapFailure(WORKING, LIVE, forbiddenCreate());

        assertTrue("A shadow-phase OS bootstrap failure must be absorbed, not propagated",
                absorbed);
        assertEquals("The migration must be halted so dotCMS falls back to ES-only",
                MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }

    /**
     * Given : Phase 3 (OS only — ES decommissioned) and an OS create rejected with 403.
     * When  : the bootstrap failure is handled.
     * Then  : it is NOT absorbed (the caller must fail loudly) and the phase is left untouched —
     *         silently rolling back to ES here would serve a stale or empty ES index.
     */
    @Test
    public void phase3_forbiddenCreate_isNotAbsorbed_andPhaseUnchanged() {
        setPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);

        final boolean absorbed = newApi()
                .handleOsBootstrapFailure(WORKING, LIVE, forbiddenCreate());

        assertFalse("In Phase 3 OS is the primary store — the failure must propagate", absorbed);
        assertEquals("Phase 3 must never be auto-rolled back to ES",
                MigrationPhase.PHASE_3_OPENSEARCH_ONLY, MigrationPhase.current());
    }

    /**
     * Given : Phase 1 and an OS create that fails for a non-authorization reason (I/O timeout).
     * When  : the bootstrap failure is handled.
     * Then  : it is absorbed just the same — the degradation is driven by the phase, not by the
     *         failure kind (which only shapes the logged remediation).
     */
    @Test
    public void phase1_nonAuthFailure_isAlsoAbsorbed() {
        setPhase(MigrationPhase.PHASE_1_DUAL_WRITE_ES_READS);

        final boolean absorbed = newApi().handleOsBootstrapFailure(WORKING, LIVE,
                new IOException("OS index creation timed out for: cluster_acme-test.working_T0.os"));

        assertTrue("Any hard OS bootstrap failure must degrade to ES-only in a shadow phase",
                absorbed);
        assertEquals(MigrationPhase.PHASE_0_MIGRATION_NOT_STARTED, MigrationPhase.current());
    }
}