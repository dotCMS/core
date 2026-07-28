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
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
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
import static org.junit.Assert.fail;

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

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();

        final String uniqueId = System.currentTimeMillis() + "";
        final Host testSite = new SiteDataGen().name("kw-search-" + uniqueId + ".local").nextPersisted();
        final Folder testFolder = new FolderDataGen().name("kwFolder_" + uniqueId).site(testSite).nextPersisted();
        assetPath = "//" + testSite.getHostname() + testFolder.getPath();

        // FileAsset with the EXACT name IMG_1004.jpeg (File.createTempFile would inject random
        // digits, so build the file inside a temp dir to control the name precisely).
        final File tmpDir = Files.createTempDirectory("kw-" + uniqueId).toFile();
        final File imgFile = new File(tmpDir, FILE_NAME);
        Files.writeString(imgFile.toPath(), "keyword search reproduction test content");
        final Contentlet fileAsset = new FileAssetDataGen(testFolder, imgFile)
                .languageId(1)
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
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        composeInode = composeItem.getInode();

        Logger.info(ContentDriveKeywordSearchTest.class, String.format(
                "Seeded FileAsset '%s' (inode %s) and compose item (inode %s) under %s",
                FILE_NAME, fileInode, composeInode, assetPath));
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
     * The reported scenario: keyword search finds {@code IMG_1004.jpeg} by its name (and case
     * variants). Matching is prefix-based per {@code GlobalSearchAttributeStrategy}, consistent with
     * the Content Search portlet. The per-term log documents the full behavior for the record.
     */
    @Test
    public void keywordSearch_findsFileAsset_byName() throws DotDataException, DotSecurityException {

        // Prefix-style terms that must find the file (a token in title/catchall starts with them).
        final List<String> mustFind = List.of("IMG", "img", "Img", "IMG_1004", "jpeg");
        // Characterization only (logged, not asserted): mid-token / boundary-spanning terms whose
        // matching depends on prefix semantics — documents how the search now behaves.
        final List<String> characterize = List.of("1004", "1004.jpeg", "IMG_1004.jpeg");

        for (final String term : characterize) {
            final PaginatedContents r = search(term);
            Logger.info(this.getClass(), String.format("[characterize] term='%s' → found=%b, %d result(s): %s",
                    term, contains(r, fileInode), r.list.size(), names(r)));
        }

        final StringBuilder failures = new StringBuilder();
        for (final String term : mustFind) {
            final PaginatedContents r = search(term);
            final boolean found = contains(r, fileInode);
            Logger.info(this.getClass(), String.format("[mustFind] term='%s' → found=%b, %d result(s): %s",
                    term, found, r.list.size(), names(r)));
            if (!found) {
                failures.append(term).append(' ');
            }
        }
        if (failures.length() > 0) {
            fail("Keyword search did not return " + FILE_NAME + " for term(s): " + failures.toString().trim());
        }
    }

    /** The exact reported input, asserted on its own for a precise failure message. */
    @Test
    public void keywordSearch_uppercaseIMG_findsFile() throws DotDataException, DotSecurityException {
        assertTrue("Searching 'IMG' must return " + FILE_NAME, contains(search("IMG"), fileInode));
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
