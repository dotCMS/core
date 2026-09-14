package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Regression test for issue #37532 — a Content Drive search term must be matched as <b>literal
 * text</b>, never as Lucene {@code query_string} syntax.
 *
 * <p>Reported through customer ticket 39185: filtering on a value containing {@code :}, {@code (}
 * or {@code /} returns "No results found" for content the user is looking at. The term is read as
 * query syntax, the query fails to parse, the failure is logged and discarded, and the caller
 * receives an empty result set indistinguishable from a genuine miss.</p>
 *
 * <p>The issue was raised against the Content Search portlet, but the same defect reaches Content
 * Drive: {@code GlobalSearchAttributeStrategy} escapes only its final clause, leaving the
 * <b>mandatory gate</b> — the clause that decides whether a document matches at all — built from
 * raw user input.</p>
 *
 * <p><b>These tests must FAIL before the fix.</b> A passing run against unmodified code would mean
 * the defect does not reach Content Drive, and the premise of the spec would need revisiting.</p>
 *
 * @see <a href="https://github.com/dotCMS/core/issues/37532">#37532</a>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveLiteralTextSearchTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();

    /** The exact headline reported on customer ticket 39185. */
    private static final String TICKET_39185_HEADLINE =
            "ABC Bank (XETRA: DBKGn.DB / NYSE: DB) and PSL Launch independent European CLO "
                    + "Total Return Indices";

    /**
     * The Lucene {@code query_string} reserved set. Each character gets its own seeded title so a
     * failure names exactly which one is still being read as syntax (SC-010).
     */
    private static final char[] RESERVED = {
            '\\', '+', '-', '!', '(', ')', ':', '^', '[', ']', '"', '{', '}', '~', '*', '?', '|',
            '&', '/'
    };

    /** A searchable text field, so the same reserved set can be exercised through field filters. */
    private static final String TOPIC_VAR = "topic";

    private static User systemUser;
    private static String assetPath;
    private static String fieldFilterInode;
    private static Host testSite;
    private static ContentType testType;

    /** Seeded title → inode, so an assertion can prove the right row came back. */
    private static final Map<String, String> seeded = new LinkedHashMap<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final String uniqueId = System.currentTimeMillis() + "";
        testSite = new SiteDataGen().name("literal-text-" + uniqueId + ".local").nextPersisted();
        final Folder folder =
                new FolderDataGen().name("literalFolder_" + uniqueId).site(testSite).nextPersisted();
        assetPath = "//" + testSite.getHostname() + folder.getPath();

        testType = new ContentTypeDataGen()
                .name("LiteralTextType_" + uniqueId)
                .velocityVarName("literalTextType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();

        new FieldDataGen().type(TextField.class).name(TOPIC_VAR).velocityVarName(TOPIC_VAR)
                .contentTypeId(testType.id()).searchable(true).indexed(true).nextPersisted();

        seed(TICKET_39185_HEADLINE, folder, languageId);

        // For the field-filter half of #37532: a searchable field value carrying reserved
        // characters, filtered through userSearchable rather than the search box.
        fieldFilterInode = new ContentletDataGen(testType.id())
                .setProperty("title", "fieldfilter" + uniqueId)
                .setProperty(TOPIC_VAR, "ABC (XETRA: DB) / topic" + uniqueId)
                .folder(folder)
                .languageId(languageId)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted()
                .getInode();
        for (final char c : RESERVED) {
            // A distinct, searchable title per reserved character. The marker keeps the titles
            // unique to this run so a stray match from elsewhere cannot make the test pass.
            seed("reserved" + uniqueId + c + "marker", folder, languageId);
        }

        Logger.info(ContentDriveLiteralTextSearchTest.class,
                String.format("Seeded %d titles under %s", seeded.size(), assetPath));
    }

    private static void seed(final String title, final Folder folder, final long languageId) {
        final Contentlet item = new ContentletDataGen(testType.id())
                .setProperty("title", title)
                .folder(folder)
                .languageId(languageId)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        seeded.put(title, item.getInode());
    }

    @AfterClass
    public static void cleanup() {
        try {
            if (null != testType) {
                APILocator.getContentTypeAPI(systemUser).delete(testType);
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveLiteralTextSearchTest.class,
                    "Could not delete test content type: " + e.getMessage());
        }
        try {
            if (null != testSite) {
                APILocator.getHostAPI().archive(testSite, systemUser, false);
                APILocator.getHostAPI().delete(testSite, systemUser, false);
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveLiteralTextSearchTest.class,
                    "Could not delete test site: " + e.getMessage());
        }
    }

    private PaginatedContents search(final String term)
            throws DotDataException, DotSecurityException {
        return contentDriveHelper.driveSearch(DriveRequestForm.builder()
                .assetPath(assetPath)
                .showFolders(false)
                .live(false)
                .archived(false)
                .offset(0)
                .maxResults(100)
                .filters(QueryFilters.builder().text(term).build())
                .build(), systemUser);
    }

    private static boolean contains(final PaginatedContents results, final String inode) {
        return results.list.stream()
                .map(item -> (String) item.get("inode"))
                .anyMatch(inode::equals);
    }

    /**
     * The other half of #37532: a term that once produced a query Elasticsearch could not parse
     * must now simply run. This is the case that used to fail, get logged, and reach the user as
     * "No results found".
     *
     * <p>Note what this test does <b>not</b> claim. An earlier attempt made the browsing service
     * raise query failures instead of swallowing them; it was reverted. Once the term is escaped,
     * no user input can break the query, so what remained was infrastructure failure — and raising
     * it broke the guarantee that a Lucene-injection attempt is escaped, matches nothing, and does
     * not produce a 500 ({@code ContentDriveFieldFilterTest#testMalformedDateBoundIsSafe}).
     * Failures the front end can observe still surface there as an error banner rather than an
     * empty grid.</p>
     */
    @Test
    public void injectionShapedTerm_runsSafely_andMatchesNothing() throws Exception {
        final PaginatedContents results = search("not-a-title\"] OR title:*");

        assertTrue("An injection-shaped term must be escaped and simply match nothing, without "
                        + "breaking the query or leaking other content",
                results.list.stream()
                        .noneMatch(item -> seeded.containsValue((String) item.get("inode"))));
    }

    /**
     * #37532 also asks for equivalent behavior in Content Drive's <b>field</b> filters. Those route
     * through {@code TextFieldStrategy}, which already escapes via {@code LuceneQueryUtils} and
     * already drops empty tokens — so this test is expected to pass <b>before</b> the search-box fix
     * as well as after. It exists so a regression there would be caught, and so the issue's
     * corresponding criterion is met by evidence rather than by inspection.
     */
    @Test
    public void fieldFilterValue_withReservedCharacters_matchesLiterally() throws Exception {
        final String uniquePart = assetPath.substring(assetPath.indexOf("literalFolder_") + 14)
                .replace("/", "");
        final PaginatedContents results = contentDriveHelper.driveSearch(DriveRequestForm.builder()
                .assetPath(assetPath)
                .contentTypes(java.util.List.of(testType.variable()))
                .showFolders(false).live(false).archived(false).offset(0).maxResults(100)
                .userSearchable(java.util.Map.of(TOPIC_VAR, "ABC (XETRA: DB) / topic" + uniquePart))
                .build(), systemUser);

        assertTrue("A field-filter value containing reserved characters must match literally",
                contains(results, fieldFilterInode));
    }

    /**
     * The customer's case, end to end. Searching for the exact headline of a piece of content must
     * return that content.
     */
    @Test
    public void ticket39185Headline_isFoundBySearchingItVerbatim() throws Exception {
        final PaginatedContents results = search(TICKET_39185_HEADLINE);
        assertTrue(
                "The ticket 39185 headline was not returned when searched verbatim. This is the "
                        + "customer-reported defect: the colons, parentheses and slash are being "
                        + "read as query syntax instead of as part of the name.",
                contains(results, seeded.get(TICKET_39185_HEADLINE)));
    }

    /**
     * Every character of the reserved set, one seeded title each, so a failure names the offending
     * character rather than reporting a generic miss (SC-010).
     */
    @Test
    public void everyReservedCharacterInATitle_isFoundBySearchingItVerbatim() throws Exception {
        final StringBuilder failures = new StringBuilder();
        for (final Map.Entry<String, String> entry : seeded.entrySet()) {
            if (entry.getKey().equals(TICKET_39185_HEADLINE)) {
                continue;
            }
            final PaginatedContents results = search(entry.getKey());
            if (!contains(results, entry.getValue())) {
                failures.append("\n  - not found: '").append(entry.getKey()).append('\'');
            }
        }
        assertTrue("Titles containing reserved characters were not findable by their own text:"
                + failures, failures.length() == 0);
    }
}
