package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for index removal on content destruction.
 *
 * <p>Covers <a href="https://github.com/dotCMS/core/issues/37276">#37276</a>:</p>
 * <ul>
 *   <li><b>AC-001 / AC-002</b> — a removal that could not be applied when the content was
 *       destroyed is still applied afterwards, and the index count stops disagreeing with the
 *       number of contentlets that actually resolve.</li>
 *   <li><b>AC-006</b> — the unpublish/archive path is unchanged: no language that should remain
 *       live is removed. This is the spec's primary non-goal and the reason the durable-removal
 *       mechanism must not be extended there.</li>
 * </ul>
 */
public class ContentletDestroyIndexRemovalTest {

    private static User systemUser;
    private static ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    @Before
    public void pauseReindex() throws DotDataException {
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
        ReindexThread.pause();
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
    }

    @After
    public void resume() {
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
        ReindexThread.unpause();
    }

    private long indexCountFor(final String identifier) throws Exception {
        return APILocator.getContentletAPI()
                .indexCount("+identifier:" + identifier, systemUser, false);
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#destroy}
     * Given Scenario: Content is destroyed while the reindex journal is paused, standing in for
     *                 an index that cannot accept the write at the moment of the destroy — the
     *                 shape of every reproduction path in the spec (write rejection, process
     *                 stop, provider unavailable).
     * Expected Result: The database rows are gone immediately, the pending removal is still owed,
     *                  and once the journal drains the index document is removed. Before the fix
     *                  the removal is lost with the paused in-memory listener and the document
     *                  remains forever.
     *
     * <p>Pausing the journal is a deliberate substitution for forcing a bulk rejection: it
     * reproduces the property under test — the removal cannot be applied now — without depending
     * on index-cluster tuning that would make the test environment-sensitive.</p>
     */
    @Test
    public void test_destroyWithUnavailableIndexWrite_removalIsAppliedOnceItRecovers()
            throws Exception {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        final String identifier = contentlet.getIdentifier();

        assertEquals("Precondition: the contentlet is in the index", 1, indexCountFor(identifier));

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();
        APILocator.getContentletAPI().destroy(contentlet, systemUser, false);

        assertTrue("The database rows must be gone immediately",
                APILocator.getContentletAPI()
                        .findAllVersions(APILocator.getIdentifierAPI().find(identifier),
                                systemUser, false).isEmpty());

        // Let the durable record drive the removal.
        Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
        ReindexThread.unpause();
        ReindexThread.startThread();

        boolean removed = false;
        for (int attempt = 0; attempt < 60 && !removed; attempt++) {
            removed = indexCountFor(identifier) == 0;
            if (!removed) {
                Thread.sleep(500);
            }
        }

        assertTrue("The index document must be removed once the write path recovers — "
                + "an inflated count over content that no longer resolves is the field symptom",
                removed);
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#unpublish}
     * Given Scenario: A contentlet with live versions in two languages; one language is
     *                 unpublished.
     * Expected Result: Only that language's live document leaves the index. The other stays live.
     *
     * <p>AC-006, the regression guard for the spec's primary non-goal. A journal entry is
     * identifier-wide, so reusing the durable-removal mechanism on this path would remove every
     * language of the identifier. This test is what makes that mistake fail loudly.</p>
     */
    @Test
    public void test_unpublishOneLanguage_leavesOtherLanguagesLive() throws Exception {
        final Language secondLanguage = new LanguageDataGen().nextPersisted();

        final Contentlet defaultLang = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersistedAndPublish();
        final String identifier = defaultLang.getIdentifier();

        final Contentlet otherLang = new ContentletDataGen(contentType.id())
                .languageId(secondLanguage.getId())
                .setProperty("identifier", identifier)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersistedAndPublish();

        assertEquals("Precondition: both languages are live in the index", 2,
                APILocator.getContentletAPI()
                        .indexCount("+identifier:" + identifier + " +live:true",
                                systemUser, false));

        APILocator.getContentletAPI().unpublish(defaultLang, systemUser, false);

        assertEquals("Unpublishing one language must leave the other live — a removal here is "
                        + "per language, never identifier-wide", 1,
                APILocator.getContentletAPI()
                        .indexCount("+identifier:" + identifier + " +live:true",
                                systemUser, false));
        assertEquals("The surviving live document must be the language that was not unpublished",
                1,
                APILocator.getContentletAPI()
                        .indexCount("+identifier:" + identifier + " +live:true +languageId:"
                                + otherLang.getLanguageId(), systemUser, false));
    }
}
