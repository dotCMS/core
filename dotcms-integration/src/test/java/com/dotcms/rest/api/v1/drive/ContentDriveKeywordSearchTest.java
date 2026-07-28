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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression test for issue #36688 — Content Drive keyword/title search.
 *
 * <p>The fix routes the toolbar keyword/title search to the database (case-insensitive, tokenized
 * {@code ILIKE}) instead of Elasticsearch, so a just-saved item is findable by name immediately
 * (read-your-writes, ADR-0018). Elasticsearch is still used for index-routed {@code userSearchable}
 * field filters, and the two compose.</p>
 *
 * <p>Covers: (1) the reported scenario — a FileAsset named {@code IMG_1004.jpeg} found by any
 * case/substring of its name (including the boundary-spanning {@code 1004.jpeg} that failed before);
 * (2) read-your-writes — an item absent from the ES index is still found; (3) composition of a text
 * keyword (DB) with a {@code userSearchable} field filter (ES).</p>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveKeywordSearchTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();

    private static User systemUser;
    private static Host testSite;
    private static Folder testFolder;
    private static String assetPath;

    /** The exact file name reported in the issue/screencast. */
    private static final String FILE_NAME = "IMG_1004.jpeg";
    private static String fileInode;

    // Compose scenario: a content type with a searchable/indexed text field.
    private static ContentType composeType;
    private static final String TOPIC_VAR = "topic";
    private static String composeInode;
    // A distinctive value stored in the topic field; the DB keyword search matches it via the
    // contentlet JSON, and the userSearchable field filter matches it in ES.
    private static final String COMPOSE_TERM = "angularcompose";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();

        final String uniqueId = System.currentTimeMillis() + "";
        testSite = new SiteDataGen().name("kw-search-" + uniqueId + ".local").nextPersisted();
        testFolder = new FolderDataGen().name("kwFolder_" + uniqueId).site(testSite).nextPersisted();
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

        // Content type + item for the text(DB)+field(ES) composition test.
        composeType = new ContentTypeDataGen()
                .name("KwComposeType_" + uniqueId)
                .velocityVarName("kwComposeType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();
        new FieldDataGen().type(TextField.class).name(TOPIC_VAR).velocityVarName(TOPIC_VAR)
                .contentTypeId(composeType.id()).searchable(true).indexed(true).nextPersisted();
        final Contentlet composeItem = new ContentletDataGen(composeType.id())
                .setProperty("title", "Compose item " + uniqueId)
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
        final DriveRequestForm request = baseRequest()
                .filters(QueryFilters.builder().text(term).build())
                .build();
        return contentDriveHelper.driveSearch(request, systemUser);
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
     * The reported scenario: keyword search finds {@code IMG_1004.jpeg} for any case and any
     * distinctive substring of the name, including the boundary-spanning {@code 1004.jpeg} and
     * multi-word queries that the previous ES path could not match.
     */
    @Test
    public void keywordSearch_findsFileAsset_caseInsensitive_anySubstring()
            throws DotDataException, DotSecurityException {

        final List<String> terms = List.of(
                "IMG", "1004", "img", "Img",           // exact screencast inputs + case variants
                "IMG_1004", "img_1004", "jpeg",        // substrings
                "1004.jpeg",                           // boundary-spanning (failed before the fix)
                "IMG 1004", "1004 jpeg");              // multi-word (tokenized, AND)

        final List<String> failures = new ArrayList<>();
        for (final String term : terms) {
            final PaginatedContents results = search(term);
            final boolean found = contains(results, fileInode);
            Logger.info(this.getClass(), String.format(
                    "term='%s' → found=%b, %d result(s): %s",
                    term, found, results.list.size(), names(results)));
            if (!found) {
                failures.add(term);
            }
        }

        if (!failures.isEmpty()) {
            fail(String.format("Keyword search did not return '%s' for term(s): %s",
                    FILE_NAME, failures));
        }
    }

    @Test
    public void keywordSearch_uppercaseIMG_findsFile() throws DotDataException, DotSecurityException {
        assertTrue("Searching 'IMG' must return " + FILE_NAME, contains(search("IMG"), fileInode));
    }

    @Test
    public void keywordSearch_digits1004_findsFile() throws DotDataException, DotSecurityException {
        assertTrue("Searching '1004' must return " + FILE_NAME, contains(search("1004"), fileInode));
    }

    /**
     * Read-your-writes: an item that is NOT in the Elasticsearch index must still be found by keyword
     * search, proving the search resolves in the database (no ES dependency for the text term). We
     * seed an item, remove it from the index, and search by its name.
     */
    @Test
    public void keywordSearch_findsItemMissingFromElasticsearchIndex_readYourWrites()
            throws Exception {

        final String uniqueName = "readyourwrites" + System.currentTimeMillis();
        final File tmpDir = Files.createTempDirectory("kw-ryw").toFile();
        final File file = new File(tmpDir, uniqueName + ".txt");
        Files.writeString(file.toPath(), "read your writes content");
        final Contentlet asset = new FileAssetDataGen(testFolder, file)
                .languageId(1)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // Drop it from the ES index — the keyword search must not depend on it.
        APILocator.getContentletIndexAPI().removeContentFromIndex(asset);

        final PaginatedContents results = search(uniqueName);
        assertTrue("Item absent from the ES index must still be found by keyword search (read-your-writes)",
                contains(results, asset.getInode()));
    }

    /**
     * Composition: a text keyword (resolved in the DB) AND a {@code userSearchable} field filter
     * (resolved in ES) combine — the item is returned only when both match.
     */
    @Test
    public void keywordText_composesWith_userSearchableFieldFilter()
            throws DotDataException, DotSecurityException {

        // Text matches (DB) AND the field filter matches (ES) → returned.
        final PaginatedContents match = contentDriveHelper.driveSearch(
                baseRequest()
                        .contentTypes(List.of(composeType.variable()))
                        .filters(QueryFilters.builder().text(COMPOSE_TERM).build())
                        .userSearchable(Map.of(TOPIC_VAR, COMPOSE_TERM))
                        .build(),
                systemUser);
        assertTrue("Text (DB) + matching field filter (ES) must return the item",
                contains(match, composeInode));

        // Text matches (DB) but the field filter does NOT → excluded (AND semantics).
        final PaginatedContents noMatch = contentDriveHelper.driveSearch(
                baseRequest()
                        .contentTypes(List.of(composeType.variable()))
                        .filters(QueryFilters.builder().text(COMPOSE_TERM).build())
                        .userSearchable(Map.of(TOPIC_VAR, "reactnomatch"))
                        .build(),
                systemUser);
        assertFalse("A non-matching field filter must exclude the item even when the text matches",
                contains(noMatch, composeInode));
    }
}
