package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.index.IndexConfigHelper.MigrationPhase.FLAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for what a skipped index provider means on the removal path.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/37276">#37276</a> loss point L2.</p>
 *
 * <h2>Why this was invisible</h2>
 * <p>{@code loadProviderIndicesQuietly} turns any failure to resolve a provider's index pointers
 * into {@code null} plus a warning, and the loop moved on. No delete operation was added for that
 * provider, and {@code putToIndex} early-returns on an empty batch — so a removal that never
 * happened was indistinguishable from one that did.</p>
 *
 * <p>The distinction that matters is primary versus shadow. {@code writeProviders()} is ordered
 * primary-first in every phase (0 → [ES], 1/2 → [ES, OS], 3 → [OS]), so element 0 is the provider
 * whose outcome the caller is entitled to. A shadow keeps warn-and-continue, which is what
 * ADR-0009 requires of the OpenSearch leg during dual-write.</p>
 *
 * <h2>Why this is an integration test and not a unit test</h2>
 * <p>The removal path takes a {@link Contentlet}, whose construction pulls in enough of the
 * container that a plain unit test cannot reach it — the existing unit tests in this area
 * deliberately exercise only the index-name overloads for that reason.</p>
 */
public class ContentletIndexProviderSkipTest {

    private static User systemUser;
    private static ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    @After
    public void clearPhase() {
        Config.setProperty(FLAG_KEY, null);
    }

    private static void setPhase(final int ordinal) {
        Config.setProperty(FLAG_KEY, String.valueOf(ordinal));
    }

    /**
     * Method to test: {@code ContentletIndexAPI#removeContentFromIndex(Contentlet)}
     * Given Scenario: The index pointers for the primary provider cannot be resolved while a
     *                 removal is attempted. The pointer record is emptied to force it.
     * Expected Result: The caller is told the removal did not happen, rather than the operation
     *                  completing silently with the document still in the index.
     *
     * <p>Before the fix this logged a warning and returned normally, so a destroy would commit
     * with the index document still in place and nothing recording that it was still owed.</p>
     */
    @Test
    public void test_primaryProviderPointersUnavailable_isNotReportedAsRemoved() throws Exception {
        setPhase(0);

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE).nextPersisted();

        // Force the primary to resolve no active index. Deleting the rows is not enough on its
        // own — IndiciesAPI reads through IndiciesCache, so the cache must be flushed too or the
        // pointers stay visible and the removal proceeds normally.
        final IndiciesInfo backup = APILocator.getIndiciesAPI().loadIndicies();
        try {
            new DotConnect().setSQL("delete from indicies").loadResult();
            CacheLocator.getIndiciesCache().clearCache();

            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            final RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> APILocator.getContentletIndexAPI().removeContentFromIndex(contentlet));

            assertTrue("The failure must say the removal was not performed, not merely that a "
                            + "provider was skipped — a warning is what made L2 invisible",
                    thrown.getMessage() != null && thrown.getMessage().contains("NOT"));
        } finally {
            // Restore the environment: other tests in the suite depend on these pointers.
            APILocator.getIndiciesAPI().point(backup);
            CacheLocator.getIndiciesCache().clearCache();
        }
    }

    /**
     * Method to test: {@code ContentletIndexAPI#removeContentFromIndex(Contentlet)}
     * Given Scenario: A dual-write phase where the primary resolves normally.
     * Expected Result: The removal succeeds. A shadow provider that cannot resolve its pointers
     *                  must keep warn-and-continue — ADR-0009 — so only the primary can fail a
     *                  removal for the caller.
     */
    @Test
    public void test_shadowProviderSkip_stillCompletesTheRemoval() throws Exception {
        setPhase(1);

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE).nextPersisted();
        final String identifier = contentlet.getIdentifier();

        APILocator.getContentletIndexAPI().removeContentFromIndex(contentlet);

        assertEquals("With the primary healthy, the removal completes regardless of the shadow",
                0, APILocator.getContentletAPI()
                        .indexCount("+identifier:" + identifier, systemUser, false));
    }
}
