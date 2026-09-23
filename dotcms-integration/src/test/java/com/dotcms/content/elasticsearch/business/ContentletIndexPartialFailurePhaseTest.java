package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests guarding the blast radius of the partial-bulk-failure escalation
 * (<a href="https://github.com/dotCMS/core/issues/37276">#37276</a>, AC-003 and AC-005).
 *
 * <h2>What is at stake</h2>
 * <p>Making a partial bulk failure reach the caller affects <b>every</b> index write, not just
 * removals. Two things must stay true:</p>
 * <ul>
 *   <li><b>ADR-0009.</b> In dual-write phases the OpenSearch leg is a shadow: its failures are
 *       "logged but do not impact operations". The escalation lives in the providers while the
 *       isolation lives in the router ({@code ContentletIndexAPIImpl#putToIndex}), so a shadow
 *       failure must still be swallowed. If this test fails, the escalation was put in the router
 *       by mistake.</li>
 *   <li><b>AC-005.</b> Ordinary add, publish and reindex traffic must be unaffected. The
 *       escalation only changes what happens on a failed bulk, never on a healthy one.</li>
 * </ul>
 */
public class ContentletIndexPartialFailurePhaseTest {

    private static User systemUser;
    private static ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    /** The phase the suite was running under before this test changed it. */
    private String originalPhase;

    /**
     * Remembers the phase the suite is running under, so {@link #restorePhase()} can put it
     * back. The weekly phase sweep sets it through {@code DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE}.
     */
    @Before
    public void savePhase() {
        originalPhase = Config.getStringProperty(FLAG_KEY, null);
    }

    /**
     * Puts back the phase saved by {@link #savePhase()}. Clearing the flag instead would drop
     * the sweep's phase and leave every later test in the suite running at phase 0.
     */
    @After
    public void restorePhase() {
        Config.setProperty(FLAG_KEY, originalPhase);
    }

    /**
     * Switches the phase for this test. Below phase 3 the test relies on Elasticsearch, so it is
     * skipped when Elasticsearch has no active content index.
     */
    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
        if (ordinal < MigrationPhase.PHASE_3_OPENSEARCH_ONLY.ordinal()) {
            assumeElasticsearchContentIndices();
        }
    }

    /**
     * Skips the calling test when Elasticsearch has no active content index to read from. The
     * phase 3 CI job starts with OpenSearch indices only, so a test that forces an earlier phase
     * and reads Elasticsearch would fail on the missing index, not on the behavior it checks.
     */
    private static void assumeElasticsearchContentIndices() {
        final IndiciesInfo info;
        try {
            info = APILocator.getIndiciesAPI().loadIndicies();
        } catch (final DotDataException e) {
            throw new DotRuntimeException(e);
        }
        assumeTrue("No active Elasticsearch content index in this environment",
                info != null && UtilMethods.isSet(info.getWorking())
                        && UtilMethods.isSet(info.getLive()));
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin}
     * Given Scenario: Ordinary content is saved and published in the default phase.
     * Expected Result: It succeeds and is searchable. The escalation must not turn healthy writes
     *                  into failures — this is the AC-005 blast-radius check for the change that
     *                  makes partial bulk failures raise.
     */
    @Test
    public void test_ordinaryWrites_areUnaffectedByTheEscalation() throws Exception {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersistedAndPublish();

        assertEquals("A healthy write must remain a healthy write", 1,
                APILocator.getContentletAPI().indexCount(
                        "+identifier:" + contentlet.getIdentifier(), systemUser, false));

        // And the working copy round-trips through the reindex path unchanged.
        APILocator.getContentletIndexAPI().addContentToIndex(contentlet, false);
        assertTrue("Reindexing existing content must not raise",
                APILocator.getContentletAPI().indexCount(
                        "+identifier:" + contentlet.getIdentifier(), systemUser, false) >= 1);
    }

    /**
     * Method to test: {@code ContentletIndexAPIImpl#putToIndex(IndexBulkRequest)}
     * Given Scenario: A dual-write phase (1), where Elasticsearch is primary and OpenSearch is the
     *                 shadow leg.
     * Expected Result: A content write succeeds from the caller's point of view even though the
     *                  shadow leg may diverge. ADR-0009: "write failures to 3.x logged but do not
     *                  impact operations."
     *
     * <p>This is the test that stops a well-meaning refactor from moving the escalation up into
     * the router, which would make every shadow hiccup fail a user-facing save.</p>
     */
    @Test
    public void test_dualWritePhase_shadowFailureDoesNotReachCaller() throws Exception {
        setPhase(1);

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();

        assertEquals("In a dual-write phase the primary decides the caller's outcome; a shadow "
                        + "divergence is logged, never raised", 1,
                APILocator.getContentletAPI().indexCount(
                        "+identifier:" + contentlet.getIdentifier(), systemUser, false));
    }
}
