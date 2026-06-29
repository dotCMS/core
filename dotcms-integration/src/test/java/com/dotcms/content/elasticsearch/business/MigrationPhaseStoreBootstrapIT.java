package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.MigrationPhaseStoreBootstrap;
import com.dotcms.util.MigrationPhaseStoreBootstrap.OpenSearchPhaseStore;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for {@link MigrationPhaseStoreBootstrap}: verifies that a contentlet created
 * through the ordinary {@link ContentletDataGen}, under an OpenSearch-only migration phase, is
 * indexed into OpenSearch and is retrievable through the high-level query path (issue #36266).
 *
 * <h2>What this verifies</h2>
 * <p>End-to-end, with no data-gen instrumentation: open an OpenSearch store under
 * {@link MigrationPhase#PHASE_3_OPENSEARCH_ONLY}, create a contentlet with the normal data-gen, and
 * confirm it is (1) present in the OpenSearch working index (checked by document {@code _id}) and
 * (2) retrievable via {@code ContentletAPI.search} — whose read path, under phase 3, hits
 * OpenSearch. Together that proves the write was routed to OS and the read resolves it.</p>
 *
 * <h2>Single-cluster friendly</h2>
 * <p>Runs in the single-cluster {@code opensearch-upgrade} profile (this suite's environment). All
 * the bootstrap, phase switching, state restore, and placement checks live in
 * {@link OpenSearchPhaseStore}, so this test carries no such boilerplate. The OS-only path never
 * writes through the legacy ES client, so the single-cluster ES-against-OS-3.x limitation does not
 * apply.</p>
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

    private OpenSearchPhaseStore store;
    private ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() {
        // One call: assert OS reachable, capture prior phase + VersionedIndices, bootstrap the OS
        // index set, and switch to OpenSearch-only. Restored in tearDown via store.close().
        store = MigrationPhaseStoreBootstrap.openForPhase(MigrationPhase.PHASE_3_OPENSEARCH_ONLY);
        contentType = new ContentTypeDataGen().nextPersisted();
        Logger.info(this, "setUp — OS working: " + store.workingIndex());
    }

    @After
    public void tearDown() {
        Try.run(() -> ContentTypeDataGen.remove(contentType));
        if (store != null) {
            store.close();
        }
    }

    /**
     * Given: PHASE_3_OPENSEARCH_ONLY with a bootstrapped OS index set.
     * When : a contentlet is persisted through the ordinary data-gen.
     * Then : it is present in the OS working index and retrievable via a high-level query (the
     *        OpenSearch read path) — proving the create routed to OS and the read resolves it.
     */
    @Test
    public void test_dataGenContentlet_indexedAndQueryableInOpenSearch() throws Exception {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .skipValidation(true)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // (1) Present in the OS working index (queried directly by _id, noise-free).
        assertTrue("contentlet must be present in the OS working index",
                store.isIndexed(contentlet));

        // (2) Retrievable through the high-level search path (phase-routed; under PHASE_3 it reads OS).
        final List<Contentlet> hits = APILocator.getContentletAPI().search(
                "+identifier:" + contentlet.getIdentifier(), 10, 0, null,
                APILocator.systemUser(), false);
        assertFalse("contentlet must be retrievable via a high-level query against OpenSearch",
                hits.isEmpty());

        // (3) Retrievable through the OpenSearch-specific search API (never the phase router) —
        //     proves the read went through the OpenSearch path specifically, not via ES fallback.
        assertTrue("contentlet must be retrievable via the OpenSearch-specific search API (OSSearchAPIImpl)",
                store.osSearchFindsByIdentifier(contentlet.getIdentifier()));

        Logger.info(this, "✅ data-gen contentlet indexed into OpenSearch and retrieved via query");
    }
}
