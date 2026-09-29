package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration tests for issue #37695: a drive-wide Content Drive search must return every matching
 * contentlet even when the candidate set is split into many Elasticsearch sub-queries.
 *
 * <p>Each sub-query restricts itself to its DB candidates with {@code +inode:(id1 OR id2 ...)}
 * inside a {@code query_string}. The index server rejects a {@code query_string} longer than
 * {@code search.query.max_query_string_length} (32,000 characters by default, OpenSearch
 * 2.19.4+/3.3.0+), so the batches must be sized by length as well as by clause count.</p>
 *
 * <p>Two kinds of test live here:</p>
 * <ul>
 *     <li><b>Lowered limit</b> ({@code BROWSER_ES_MAX_QUERY_STRING_LENGTH} set low): a small
 *     dataset is spread over several sub-queries and several pages, on any index engine. These
 *     guard batch and page boundaries; they pass before the fix too, since the old code ignores
 *     the key.</li>
 *     <li><b>Real limit</b>: seeds more candidates than one clause-sized batch (~876 inodes) can
 *     hold within 32,000 characters. It only fails without the fix against an engine that
 *     enforces the limit, e.g. {@code -Dopensearch.phase=3} (OpenSearch 3.8.0). The default
 *     integration engine (OpenSearch 1.3.6) has no such limit.</li>
 * </ul>
 *
 * @see <a href="https://github.com/dotCMS/core/issues/37695">#37695</a>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveLargeCandidateSetTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static final String BODY_VAR = "body";
    private static final String TAGLINE_VAR = "tagline";

    /** Low enough that each sub-query holds a few dozen inodes, high enough for any base query. */
    private static final int LOWERED_QUERY_LENGTH = 2_000;
    private static final int PAGE_SIZE = 10;
    /** Upper bound on pages fetched, so a broken cursor fails the test instead of hanging it. */
    private static final int MAX_PAGES = 500;

    private static final int TITLE_MATCHES = 30;
    private static final int BODY_ONLY_MATCHES = 30;
    private static final int NON_MATCHES = 60;

    /** More than one DB chunk's clause-sized ES batch (~876) inside the 900-row default chunk. */
    private static final int REAL_LIMIT_CANDIDATES = 950;
    private static final int REAL_LIMIT_MATCH_EVERY = 95;

    private static User systemUser;
    private static ContentType testType;

    private static Host smallSite;
    private static String term;
    private static final Set<String> titleMatchInodes = new HashSet<>();
    private static final Set<String> bodyOnlyMatchInodes = new HashSet<>();
    private static final Set<String> taglineMatchInodes = new HashSet<>();

    private static Host largeSite;
    private static String largeTerm;
    private static final Set<String> largeMatchInodes = new HashSet<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();
        // Failsafe re-runs a failing class (rerunFailingTestsCount), calling this again; start
        // from empty so the expectations only hold this run's seeds.
        titleMatchInodes.clear();
        bodyOnlyMatchInodes.clear();
        taglineMatchInodes.clear();
        largeMatchInodes.clear();

        final String uniqueId = System.currentTimeMillis() + "";
        term = "largeset" + uniqueId;
        largeTerm = "reallimit" + uniqueId;

        smallSite = new SiteDataGen().name("drive-large-small-" + uniqueId + ".local").nextPersisted();
        largeSite = new SiteDataGen().name("drive-large-real-" + uniqueId + ".local").nextPersisted();

        testType = new ContentTypeDataGen()
                .name("DriveLargeType_" + uniqueId).velocityVarName("driveLargeType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT).nextPersisted();
        new FieldDataGen().type(TextField.class).name(BODY_VAR).velocityVarName(BODY_VAR)
                .contentTypeId(testType.id()).searchable(true).indexed(true).nextPersisted();
        new FieldDataGen().type(TextField.class).name(TAGLINE_VAR).velocityVarName(TAGLINE_VAR)
                .contentTypeId(testType.id()).searchable(true).indexed(true).nextPersisted();

        // Small site: title matches, body-only matches and non-matches. Every third item of the
        // two matching groups also carries the term in its tagline, for the field-filter path.
        for (int i = 0; i < TITLE_MATCHES; i++) {
            final String tagline = i % 3 == 0 ? term : "no tagline";
            final String inode = seed(smallSite, term + " title " + i, "plain body " + i, tagline,
                    IndexPolicy.WAIT_FOR);
            titleMatchInodes.add(inode);
            if (term.equals(tagline)) {
                taglineMatchInodes.add(inode);
            }
        }
        for (int i = 0; i < BODY_ONLY_MATCHES; i++) {
            final String tagline = i % 3 == 0 ? term : "no tagline";
            final String inode = seed(smallSite, "plain heading " + i, "the body mentions " + term,
                    tagline, IndexPolicy.WAIT_FOR);
            bodyOnlyMatchInodes.add(inode);
            if (term.equals(tagline)) {
                taglineMatchInodes.add(inode);
            }
        }
        for (int i = 0; i < NON_MATCHES; i++) {
            seed(smallSite, "unrelated heading " + i, "unrelated body " + i, "no tagline",
                    IndexPolicy.WAIT_FOR);
        }

        // Large site: only the DB candidate count matters for the query length, so the
        // non-matching rows skip waiting for the index; the matches are indexed before the tests.
        for (int i = 0; i < REAL_LIMIT_CANDIDATES; i++) {
            final boolean match = i % REAL_LIMIT_MATCH_EVERY == REAL_LIMIT_MATCH_EVERY / 2;
            final String inode = seed(largeSite,
                    match ? largeTerm + " item " + i : "filler item " + i,
                    "filler body", "no tagline",
                    match ? IndexPolicy.WAIT_FOR : IndexPolicy.DEFER);
            if (match) {
                largeMatchInodes.add(inode);
            }
        }

        Logger.info(ContentDriveLargeCandidateSetTest.class, String.format(
                "Seeded %d candidates on %s and %d on %s",
                TITLE_MATCHES + BODY_ONLY_MATCHES + NON_MATCHES, smallSite.getHostname(),
                REAL_LIMIT_CANDIDATES, largeSite.getHostname()));
    }

    private static String seed(final Host site, final String title, final String body,
            final String tagline, final IndexPolicy policy) {
        return new ContentletDataGen(testType.id())
                .setProperty("title", title)
                .setProperty(BODY_VAR, body)
                .setProperty(TAGLINE_VAR, tagline)
                .host(site)
                .setPolicy(policy)
                .nextPersisted()
                .getInode();
    }

    @AfterClass
    public static void cleanup() {
        Config.setProperty(BrowserAPIImpl.BROWSER_ES_MAX_QUERY_STRING_LENGTH_KEY,
                BrowserAPIImpl.BROWSER_ES_MAX_QUERY_STRING_LENGTH_DEFAULT);
        try {
            if (null != testType) {
                APILocator.getContentTypeAPI(systemUser).delete(testType);
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveLargeCandidateSetTest.class, "type cleanup: " + e.getMessage());
        }
        for (final Host site : List.of(smallSite, largeSite)) {
            try {
                APILocator.getHostAPI().archive(site, systemUser, false);
                APILocator.getHostAPI().delete(site, systemUser, false);
            } catch (final Exception e) {
                Logger.warn(ContentDriveLargeCandidateSetTest.class, "site cleanup: " + e.getMessage());
            }
        }
    }

    /**
     * A drive-wide request (the site's All Site Content, System Host excluded) for the given site,
     * ready to receive a text filter or field criteria.
     */
    private static DriveRequestForm.Builder siteWideRequest(final Host site) {
        return DriveRequestForm.builder()
                .assetPath("//" + site.getHostname() + "/")
                .browseScope(BrowseScope.ALL)
                .includeSystemHost(false)
                .showFolders(false)
                .live(false)
                .archived(false)
                .offset(0)
                .maxResults(PAGE_SIZE);
    }

    /**
     * Pages through a search by feeding each response's {@code nextContentCursor} back as the next
     * request's {@code contentCursor}, collecting every contentlet inode in page order. Fails on a
     * duplicate (a row repeated across pages) or when the cursor never ends.
     *
     * @return the inodes of every page, with the number of pages fetched
     */
    private static PagedResult pageThrough(final DriveRequestForm.Builder request)
            throws DotDataException, DotSecurityException {
        final List<String> inodes = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        int cursor = 0;
        int pages = 0;
        while (true) {
            pages++;
            if (pages > MAX_PAGES) {
                fail("Paging did not end after " + MAX_PAGES + " pages; last cursor " + cursor);
            }
            final PaginatedContents page =
                    contentDriveHelper.driveSearch(request.contentCursor(cursor).build(), systemUser);
            for (final Map<String, Object> item : page.list) {
                final String inode = (String) item.get("inode");
                if (!seen.add(inode)) {
                    fail("Inode " + inode + " was returned twice (page " + pages + ")");
                }
                inodes.add(inode);
            }
            if (!page.hasMoreContent) {
                return new PagedResult(inodes, pages);
            }
            cursor = page.nextContentCursor;
        }
    }

    private static final class PagedResult {
        final List<String> inodes;
        final int pages;

        PagedResult(final List<String> inodes, final int pages) {
            this.inodes = inodes;
            this.pages = pages;
        }
    }

    private static void withLoweredQueryLength(final ThrowingRunnable test) throws Exception {
        Config.setProperty(BrowserAPIImpl.BROWSER_ES_MAX_QUERY_STRING_LENGTH_KEY, LOWERED_QUERY_LENGTH);
        try {
            test.run();
        } finally {
            Config.setProperty(BrowserAPIImpl.BROWSER_ES_MAX_QUERY_STRING_LENGTH_KEY,
                    BrowserAPIImpl.BROWSER_ES_MAX_QUERY_STRING_LENGTH_DEFAULT);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Title scope with the limit lowered so the candidates spread over several sub-queries: every
     * title match comes back exactly once, across more than one page, and nothing else does.
     */
    @Test
    public void titleScope_loweredLimit_returnsEveryMatchAcrossSubQueriesAndPages() throws Exception {
        withLoweredQueryLength(() -> {
            final PagedResult result = pageThrough(siteWideRequest(smallSite)
                    .filters(QueryFilters.builder().text(term).searchScope(SearchScope.TITLE).build()));
            assertEquals(titleMatchInodes, new HashSet<>(result.inodes));
            assertTrue("Expected more than one page, got " + result.pages, result.pages > 1);
        });
    }

    /**
     * All Fields scope with the limit lowered: title matches and body-only matches all come back
     * exactly once, across pages.
     */
    @Test
    public void allFieldsScope_loweredLimit_returnsEveryMatchAcrossSubQueriesAndPages() throws Exception {
        withLoweredQueryLength(() -> {
            final PagedResult result = pageThrough(siteWideRequest(smallSite)
                    .filters(QueryFilters.builder().text(term).searchScope(SearchScope.ALL_FIELDS).build()));
            final Set<String> expected = new HashSet<>(titleMatchInodes);
            expected.addAll(bodyOnlyMatchInodes);
            assertEquals(expected, new HashSet<>(result.inodes));
            assertTrue("Expected more than one page, got " + result.pages, result.pages > 1);
        });
    }

    /**
     * An index-routed field filter (one content type, so the single-pass path from #37184) with the
     * limit lowered: exactly the items whose tagline carries the term, each once.
     */
    @Test
    public void fieldFilter_loweredLimit_returnsEveryMatchAcrossSubQueriesAndPages() throws Exception {
        withLoweredQueryLength(() -> {
            final PagedResult result = pageThrough(siteWideRequest(smallSite)
                    .contentTypes(List.of(testType.variable()))
                    .userSearchable(Map.of(TAGLINE_VAR, term)));
            assertEquals(taglineMatchInodes, new HashSet<>(result.inodes));
            assertTrue("Expected more than one page, got " + result.pages, result.pages > 1);
        });
    }

    /**
     * With the default limit, more candidates than one clause-sized batch can fit in 32,000
     * characters: every match comes back. Without the fix, an engine that enforces
     * {@code search.query.max_query_string_length} rejects the oversized sub-query and the
     * matches it held go missing.
     */
    @Test
    public void titleScope_realLimit_returnsEveryMatchOnALargeSite() throws Exception {
        final PagedResult result = pageThrough(siteWideRequest(largeSite)
                .filters(QueryFilters.builder().text(largeTerm).searchScope(SearchScope.TITLE).build()));
        assertEquals(largeMatchInodes, new HashSet<>(result.inodes));
    }
}
