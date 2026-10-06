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
import com.dotcms.datagen.FileAssetDataGen;
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
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for issue #36688 — Content Drive keyword/title search.
 *
 * <p>Per the team decision (ADR-0018: text search stays on the index), the fix makes Content Drive's
 * keyword search build the <b>same</b> Elasticsearch query as the Content Search portlet — by reusing
 * {@code GlobalSearchAttributeStrategy}. This replaces the previous broad {@code catchall:*kw*}
 * leading-wildcard (which returned unrelated body matches and scanned slowly on large indexed
 * datasets) with a selective {@code +catchall:kw*} prefix plus tokenized, escaped title boosts.</p>
 *
 * <p>Covers: (1) the reported scenario — the FileAsset {@code IMG_1004.jpeg} is found by its name and
 * case variants; (2) composition of a text keyword with a {@code userSearchable} field filter. The
 * per-term log documents the exact matching behavior (now prefix-based, consistent with Content
 * Search).</p>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveKeywordSearchTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();

    private static User systemUser;
    private static String assetPath;

    /** The exact file name reported in the issue/screencast. */
    private static final String FILE_NAME = "IMG_1004.jpeg";
    private static String fileInode;

    // Compose scenario: a content type with a searchable/indexed text field.
    private static ContentType composeType;
    private static final String TOPIC_VAR = "topic";
    private static String composeInode;
    private static final String COMPOSE_TERM = "angularcompose";

    // Kept for @AfterClass cleanup so the suite doesn't accumulate sites/types/temp dirs.
    private static Host testSite;
    private static File tmpDir;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();
        final long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final String uniqueId = System.currentTimeMillis() + "";
        testSite = new SiteDataGen().name("kw-search-" + uniqueId + ".local").nextPersisted();
        final Folder testFolder = new FolderDataGen().name("kwFolder_" + uniqueId).site(testSite).nextPersisted();
        assetPath = "//" + testSite.getHostname() + testFolder.getPath();

        // FileAsset with the EXACT name IMG_1004.jpeg (File.createTempFile would inject random
        // digits, so build the file inside a temp dir to control the name precisely).
        tmpDir = Files.createTempDirectory("kw-" + uniqueId).toFile();
        final File imgFile = new File(tmpDir, FILE_NAME);
        Files.writeString(imgFile.toPath(), "keyword search reproduction test content");
        final Contentlet fileAsset = new FileAssetDataGen(testFolder, imgFile)
                .languageId(defaultLanguageId)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        fileInode = fileAsset.getInode();

        // Content type + item for the text + field-filter composition test.
        composeType = new ContentTypeDataGen()
                .name("KwComposeType_" + uniqueId)
                .velocityVarName("kwComposeType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();
        new FieldDataGen().type(TextField.class).name(TOPIC_VAR).velocityVarName(TOPIC_VAR)
                .contentTypeId(composeType.id()).searchable(true).indexed(true).nextPersisted();
        final Contentlet composeItem = new ContentletDataGen(composeType.id())
                .setProperty("title", COMPOSE_TERM + " report " + uniqueId)
                .setProperty(TOPIC_VAR, COMPOSE_TERM)
                .folder(testFolder)
                .languageId(defaultLanguageId)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        composeInode = composeItem.getInode();

        Logger.info(ContentDriveKeywordSearchTest.class, String.format(
                "Seeded FileAsset '%s' (inode %s) and compose item (inode %s) under %s",
                FILE_NAME, fileInode, composeInode, assetPath));
    }

    /**
     * Removes the fixtures this class created so the rest of the suite runs against a clean state.
     * Failures here must not fail the test run — they are logged and swallowed.
     */
    @AfterClass
    public static void cleanup() {
        try {
            if (null != composeType) {
                APILocator.getContentTypeAPI(systemUser).delete(composeType);
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveKeywordSearchTest.class,
                    "Could not delete test content type: " + e.getMessage());
        }
        try {
            if (null != testSite) {
                APILocator.getHostAPI().archive(testSite, systemUser, false);
                APILocator.getHostAPI().delete(testSite, systemUser, false);
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveKeywordSearchTest.class,
                    "Could not delete test site: " + e.getMessage());
        }
        try {
            if (null != tmpDir) {
                FileUtil.deleteDir(tmpDir.getAbsolutePath());
            }
        } catch (final Exception e) {
            Logger.warn(ContentDriveKeywordSearchTest.class,
                    "Could not delete temp dir: " + e.getMessage());
        }
    }

    /** Runs a plain keyword search through the Content Drive endpoint path. */
    private PaginatedContents search(final String term) throws DotDataException, DotSecurityException {
        return contentDriveHelper.driveSearch(baseRequest()
                .filters(QueryFilters.builder().text(term).build())
                .build(), systemUser);
    }

    private DriveRequestForm.Builder baseRequest() {
        return DriveRequestForm.builder()
                .assetPath(assetPath)
                .showFolders(false)
                .live(false)      // working content
                .archived(false)
                .offset(0)
                .maxResults(100);
    }

    private static boolean contains(final PaginatedContents results, final String inode) {
        return results.list.stream()
                .map(item -> (String) item.get("inode"))
                .anyMatch(inode::equals);
    }

    private static List<String> names(final PaginatedContents results) {
        return results.list.stream()
                .map(item -> String.valueOf(item.get("title")))
                .collect(Collectors.toList());
    }

    /**
     * Terms that must return {@code IMG_1004.jpeg} — each one is a genuine token prefix of the
     * indexed name (tokens: {@code img_1004}, {@code jpeg}), in assorted casing.
     * <p>
     * Mid-token ({@code 1004}), boundary-spanning ({@code 1004.jpeg}) and exact-full-name
     * ({@code IMG_1004.jpeg}) terms are deliberately absent: prefix matching — the behavior this PR
     * aligns with the Content Search portlet — cannot match them. That shared limitation is fixed
     * separately in #36791, and covered there by {@code GlobalSearchAttributeStrategyMatchingTest}.
     */
    @DataProvider
    public static Object[] matchingKeywordTerms() {
        return new String[]{"IMG", "img", "Img", "IMG_1004", "jpeg"};
    }

    /**
     * The reported scenario: keyword search finds {@code IMG_1004.jpeg} by its name. Run per term so
     * a failure names the exact keyword instead of collapsing every term into one assertion.
     */
    @Test
    @UseDataProvider("matchingKeywordTerms")
    public void keywordSearch_findsFileAsset_byName(final String term)
            throws DotDataException, DotSecurityException {
        final PaginatedContents results = search(term);
        Logger.info(this.getClass(), String.format("term='%s' → %d result(s): %s",
                term, results.list.size(), names(results)));
        assertTrue("Searching '" + term + "' must return " + FILE_NAME,
                contains(results, fileInode));
    }

    /**
     * Composition: a text keyword and a {@code userSearchable} field filter combine — the item is
     * returned only when both match (AND semantics). Both resolve through Elasticsearch.
     */
    @Test
    public void keywordText_composesWith_userSearchableFieldFilter()
            throws DotDataException, DotSecurityException {

        final PaginatedContents match = contentDriveHelper.driveSearch(
                baseRequest()
                        .contentTypes(List.of(composeType.variable()))
                        .filters(QueryFilters.builder().text(COMPOSE_TERM).build())
                        .userSearchable(java.util.Map.of(TOPIC_VAR, COMPOSE_TERM))
                        .build(),
                systemUser);
        assertTrue("Text + matching field filter must return the item", contains(match, composeInode));

        final PaginatedContents noMatch = contentDriveHelper.driveSearch(
                baseRequest()
                        .contentTypes(List.of(composeType.variable()))
                        .filters(QueryFilters.builder().text(COMPOSE_TERM).build())
                        .userSearchable(java.util.Map.of(TOPIC_VAR, "reactnomatch"))
                        .build(),
                systemUser);
        assertFalse("A non-matching field filter must exclude the item even when the text matches",
                contains(noMatch, composeInode));
    }
}
