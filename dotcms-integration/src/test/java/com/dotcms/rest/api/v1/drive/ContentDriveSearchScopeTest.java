package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the Content Drive search scope (issue #37479): the two-option control that
 * says which fields a search term is matched against.
 *
 * <p>These live here rather than in a unit test on purpose. {@code buildBaseESQuery} is
 * package-private, but asserting on it needs a {@code BrowserAPIImpl} instance and a
 * {@code BrowserQuery}, and constructing either initialises the database layer — the plan's original
 * claim that this was unit-testable did not survive contact with the code.</p>
 *
 * <p>The seeded fixture is the shape the user story describes: one document whose <b>title</b>
 * carries the term, and one whose title does not but whose <b>body</b> does.</p>
 *
 * @see <a href="https://github.com/dotCMS/core/issues/37479">#37479</a>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveSearchScopeTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static final String BODY_VAR = "body";

    private static User systemUser;
    private static String assetPath;
    private static Host testSite;
    private static ContentType testType;

    /** The search term. Unique per run so nothing else in the index can satisfy an assertion. */
    private static String term;
    /** Title contains the term. Must be returned in BOTH scopes. */
    private static String titleMatchInode;
    /** Title does NOT contain the term; the body does. Returned in All Fields only. */
    private static String bodyOnlyMatchInode;
    /** A folder whose NAME contains the term — scope-independent, must appear in both. */
    private static String folderName;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final String uniqueId = System.currentTimeMillis() + "";
        term = "scopeterm" + uniqueId;

        testSite = new SiteDataGen().name("scope-" + uniqueId + ".local").nextPersisted();
        final Folder root =
                new FolderDataGen().name("scopeRoot_" + uniqueId).site(testSite).nextPersisted();
        assetPath = "//" + testSite.getHostname() + root.getPath();

        // A folder whose own name carries the term. Folders never reach the index — they are
        // narrowed in Java on their name — so this must behave identically in both scopes.
        folderName = term + "folder";
        // A child folder takes its site from its parent; passing .site() as well would create it
        // at the site root instead, where this search would never see it.
        new FolderDataGen().name(folderName).parent(root).nextPersisted();

        testType = new ContentTypeDataGen()
                .name("ScopeType_" + uniqueId).velocityVarName("scopeType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT).host(testSite).nextPersisted();
        new FieldDataGen().type(TextField.class).name(BODY_VAR).velocityVarName(BODY_VAR)
                .contentTypeId(testType.id()).searchable(true).indexed(true).nextPersisted();

        titleMatchInode = seed(term + " in the title", "unrelated body copy", root, languageId);
        bodyOnlyMatchInode = seed("a plain heading " + uniqueId, "the body mentions " + term,
                root, languageId);

        Logger.info(ContentDriveSearchScopeTest.class, String.format(
                "Seeded term=%s titleMatch=%s bodyOnly=%s under %s",
                term, titleMatchInode, bodyOnlyMatchInode, assetPath));
    }

    private static String seed(final String title, final String body, final Folder folder,
            final long languageId) {
        final Contentlet item = new ContentletDataGen(testType.id())
                .setProperty("title", title)
                .setProperty(BODY_VAR, body)
                .folder(folder)
                .languageId(languageId)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        return item.getInode();
    }

    @AfterClass
    public static void cleanup() {
        try {
            if (null != testType) {
                APILocator.getContentTypeAPI(systemUser).delete(testType);
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveSearchScopeTest.class, "type cleanup: " + e.getMessage());
        }
        try {
            if (null != testSite) {
                APILocator.getHostAPI().archive(testSite, systemUser, false);
                APILocator.getHostAPI().delete(testSite, systemUser, false);
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveSearchScopeTest.class, "site cleanup: " + e.getMessage());
        }
    }

    /** Runs a Content Drive search. A null scope means the field is omitted entirely. */
    private PaginatedContents search(final SearchScope scope, final boolean showFolders)
            throws DotDataException, DotSecurityException {
        final QueryFilters.Builder filters = QueryFilters.builder().text(term);
        if (null != scope) {
            filters.searchScope(scope);
        }
        return contentDriveHelper.driveSearch(DriveRequestForm.builder()
                .assetPath(assetPath)
                .showFolders(showFolders)
                .live(false).archived(false).offset(0).maxResults(100)
                .filters(filters.build())
                .build(), systemUser);
    }

    private static boolean contains(final PaginatedContents results, final String inode) {
        return results.list.stream()
                .map(item -> (String) item.get("inode"))
                .anyMatch(inode::equals);
    }

    // -----------------------------------------------------------------------------------------
    // FR-008 / FR-009 — what each scope returns.
    // -----------------------------------------------------------------------------------------

    /** FR-008: Title scope returns a title match and excludes a body-only match. */
    @Test
    public void titleScope_returnsTitleMatch_andExcludesBodyOnlyMatch() throws Exception {
        final PaginatedContents results = search(SearchScope.TITLE, false);

        assertTrue("The document whose TITLE carries the term must be returned in Title scope",
                contains(results, titleMatchInode));
        assertFalse("A document whose term appears only in the body must NOT be returned in "
                + "Title scope — this is the narrowing the feature exists for",
                contains(results, bodyOnlyMatchInode));
    }

    /** FR-009: All Fields returns both, exactly as the drive does today. */
    @Test
    public void allFieldsScope_returnsBothTitleAndBodyMatches() throws Exception {
        final PaginatedContents results = search(SearchScope.ALL_FIELDS, false);

        assertTrue("All Fields must return the title match", contains(results, titleMatchInode));
        assertTrue("All Fields must return the body-only match — that is today's behaviour",
                contains(results, bodyOnlyMatchInode));
    }

    /**
     * FR-017 / SC-005: a request that OMITS the scope must behave exactly like one that names
     * All Fields. This is the requirement that protects the Asset Picker, which never sends it.
     */
    @Test
    public void omittedScope_behavesExactlyLikeAllFields() throws Exception {
        final PaginatedContents omitted = search(null, false);
        final PaginatedContents explicit = search(SearchScope.ALL_FIELDS, false);

        assertEquals("An omitted scope must return the same number of results as an explicit "
                + "ALL_FIELDS request", explicit.list.size(), omitted.list.size());
        assertTrue(contains(omitted, titleMatchInode));
        assertTrue(contains(omitted, bodyOnlyMatchInode));
    }

    /**
     * FR-011: folders never reach the search index — they are narrowed in Java on their own name —
     * so a folder whose name matches must appear in BOTH scopes, identically.
     */
    @Test
    public void folderNameMatching_isIdenticalInBothScopes() throws Exception {
        // Prove the fixture before comparing scopes: an unfiltered listing must show the folder,
        // otherwise a "both scopes agree" assertion would pass on two empty sets.
        final PaginatedContents unfiltered = contentDriveHelper.driveSearch(
                DriveRequestForm.builder().assetPath(assetPath).showFolders(true)
                        .live(false).archived(false).offset(0).maxResults(100).build(),
                systemUser);
        final java.util.List<String> unfilteredNames = unfiltered.list.stream()
                .map(String::valueOf).collect(java.util.stream.Collectors.toList());
        Logger.info(ContentDriveSearchScopeTest.class, "SCOPE unfiltered listing: " + unfilteredNames);
        assertTrue("Fixture check: the seeded folder must be visible in an unfiltered listing, "
                        + "otherwise this test proves nothing. Saw: " + unfilteredNames,
                unfilteredNames.stream().anyMatch(row -> row.contains(folderName)));

        final PaginatedContents titleScoped = search(SearchScope.TITLE, true);
        final PaginatedContents allFields = search(SearchScope.ALL_FIELDS, true);

        final java.util.List<String> titleNames = titleScoped.list.stream()
                .map(String::valueOf).collect(java.util.stream.Collectors.toList());
        final java.util.List<String> allNames = allFields.list.stream()
                .map(String::valueOf).collect(java.util.stream.Collectors.toList());
        Logger.info(ContentDriveSearchScopeTest.class,
                "SCOPE folders — title=" + titleNames + " allFields=" + allNames);

        final long inTitle = titleNames.stream().filter(row -> row.contains(folderName)).count();
        final long inAllFields = allNames.stream().filter(row -> row.contains(folderName)).count();

        assertEquals("Folder name matching is scope-independent and must not change",
                inAllFields, inTitle);
        assertTrue("The folder whose name carries the term must be listed in both scopes. "
                        + "Title scope returned: " + titleNames, inTitle > 0);
    }

    // -----------------------------------------------------------------------------------------
    // FR-018 / FR-025 — the contract rejects nonsense instead of guessing.
    // -----------------------------------------------------------------------------------------

    /** FR-025: the scope qualifies the text and is meaningless without it. */
    @Test
    public void scopeWithoutText_isRejected() throws Exception {
        try {
            contentDriveHelper.driveSearch(DriveRequestForm.builder()
                    .assetPath(assetPath)
                    .showFolders(false).live(false).archived(false).offset(0).maxResults(100)
                    .filters(QueryFilters.builder().text("").searchScope(SearchScope.TITLE).build())
                    .build(), systemUser);
            org.junit.Assert.fail(
                    "A search scope with no text is a contract error and must be rejected");
        } catch (final com.dotcms.rest.exception.BadRequestException e) {
            // The explanation travels in the HTTP response, not in getMessage(), which is the
            // generic status line. That the request is refused at all is what this layer asserts;
            // the wording of the message is checked at the endpoint layer (Postman).
            assertEquals("The refusal must be a 400, not a 500",
                    javax.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(),
                    e.getResponse().getStatus());
        }
    }

    /**
     * SC-009 carried into Title scope: the ticket 39185 headline must be findable here too. FR-027
     * applies to both scopes, so the clause Title introduces must escape exactly as the other does.
     */
    @Test
    public void titleScope_alsoMatchesTermsWithReservedCharacters() throws Exception {
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(assetPath.substring(assetPath.indexOf('/', 2)), testSite,
                        systemUser, false);
        final String punctuated = "ABC (XETRA: DB) / scope" + System.nanoTime();
        final String inode = seed(punctuated, "unrelated", folder, languageId);

        final PaginatedContents results = contentDriveHelper.driveSearch(DriveRequestForm.builder()
                .assetPath(assetPath)
                .showFolders(false).live(false).archived(false).offset(0).maxResults(100)
                .filters(QueryFilters.builder().text(punctuated)
                        .searchScope(SearchScope.TITLE).build())
                .build(), systemUser);

        assertTrue("Title scope must match reserved characters literally, exactly as All Fields "
                + "does — FR-027 applies to both", contains(results, inode));
    }
}
