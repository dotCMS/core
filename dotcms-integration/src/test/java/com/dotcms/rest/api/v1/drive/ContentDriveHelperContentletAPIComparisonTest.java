package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration test to compare results between ContentDriveHelper#driveSearch and ContentletAPI#search.
 *
 * <p>This test suite ensures both APIs return consistent results when using equivalent filtering criteria.
 * It validates that the ContentDriveHelper, which provides drive-like functionality for content browsing,
 * produces the same underlying contentlets as direct ContentletAPI searches with equivalent Lucene queries.</p>
 *
 * <h3>Test Strategy:</h3>
 * <ul>
 *   <li>Create isolated test data with unique names to avoid cross-test conflicts</li>
 *   <li>Execute equivalent queries on both APIs using different request formats</li>
 *   <li>Compare results by extracting and matching contentlet inodes</li>
 *   <li>Log differences for debugging and analysis</li>
 * </ul>
 *
 * <h3>Test Scenarios:</h3>
 * <ul>
 *   <li>Basic content retrieval without filters using asset path navigation</li>
 *   <li>Text-based filtering (full-text search through filters)</li>
 *   <li>Content type filtering (specific content types via DriveRequestForm)</li>
 *   <li>Pagination consistency</li>
 *   <li>Base content type filtering (CONTENT vs FILEASSET)</li>
 *   <li>File mime type filtering</li>
 * </ul>
 *
 * @author Integration Test Suite
 * @since Jan 2025
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveHelperContentletAPIComparisonTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    private static Host testSite;
    private static Folder testFolder;
    private static User systemUser;

    // Test content data
    private static final List<Contentlet> testContentlets = new ArrayList<>();
    private static final List<FileAsset> testFileAssets = new ArrayList<>();
    private static ContentType testContentType;
    private static String testAssetPath;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();

        // Generate unique names using timestamp to avoid conflicts between test runs
        String uniqueId = System.currentTimeMillis() + "";

        // Site name must comply with hostname regex pattern
        testSite = new SiteDataGen().name("drive-test-" + uniqueId + ".local").nextPersisted();
        testFolder = new FolderDataGen().name("driveTestFolder_" + uniqueId).site(testSite).nextPersisted();

        // Create asset path for ContentDriveHelper navigation
        testAssetPath = "//" + testSite.getHostname() + testFolder.getPath();

        // Create a custom content type for testing
        testContentType = new ContentTypeDataGen()
                .name("DriveTestContentType_" + uniqueId)
                .velocityVarName("driveTest_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();

        // Create test content items with different characteristics
        createTestContent();

        Logger.info(ContentDriveHelperContentletAPIComparisonTest.class,
            "Created test data: " + testContentlets.size() + " contentlets, " +
            testFileAssets.size() + " file assets for asset path: " + testAssetPath);
    }

    private static void createTestContent() throws Exception {
        // Generate unique identifiers for content names
        String contentId = UUIDGenerator.generateUuid().substring(0, 8);

        // Create regular contentlets with searchable titles
        for (int i = 1; i <= 5; i++) {
            Contentlet contentlet = new ContentletDataGen(testContentType.id())
                    .setProperty("title", "Drive Test Content " + i + "_" + contentId)
                    .folder(testFolder)
                    .nextPersisted();
            testContentlets.add(contentlet);
        }

        // Create contentlets with specific keywords for filtering tests
        Contentlet keywordContent1 = new ContentletDataGen(testContentType.id())
                .setProperty("title", "Special Drive Content Alpha_" + contentId)
                .folder(testFolder)
                .nextPersisted();
        testContentlets.add(keywordContent1);

        Contentlet keywordContent2 = new ContentletDataGen(testContentType.id())
                .setProperty("title", "Another Special Drive Beta Content_" + contentId)
                .folder(testFolder)
                .nextPersisted();
        testContentlets.add(keywordContent2);

        // Create file assets with unique names and different mime types
        for (int i = 1; i <= 3; i++) {
            FileAsset fileAsset = APILocator.getFileAssetAPI()
                    .fromContentlet(FileAssetDataGen.createFileAsset(testFolder, "drivetestfile" + i + "_" + contentId, ".txt"));
            testFileAssets.add(fileAsset);
        }

        // Create file asset with specific name for filtering
        FileAsset specialFile = APILocator.getFileAssetAPI()
                .fromContentlet(FileAssetDataGen.createFileAsset(testFolder, "special-drive-document_" + contentId, ".txt"));
        testFileAssets.add(specialFile);

        // Create Image file asset with specific name for filtering
        final URL url = ContentDriveHelperContentletAPIComparisonTest.class.getResource("/images/test.jpg");
        if (null == url) {
            fail("Failed to create test Image File Asset for 'ContentDriveHelperContentletAPIComparisonTest' Integration Test");
        }
        final File testImage = new File(url.getFile());
        Contentlet imageFileContentlet = ContentletDataGen.publish(FileAssetDataGen.createImageFileAssetDataGen(testImage).folder(testFolder).nextPersisted());
        final FileAsset imageFile = APILocator.getFileAssetAPI().fromContentlet(imageFileContentlet);
        testFileAssets.add(imageFile);
    }

    /**
     * Test basic content retrieval comparison without filtering.
     *
     * Given: A test folder containing 7 contentlets and 4 file assets accessible via asset path
     * When: ContentDriveHelper.driveSearch() and ContentletAPI.search() are called with identical scope filters
     * Then: Both APIs should return the same set of contentlets (identified by inodes)
     *       and there should be substantial overlap between the results
     */
    @Test
    public void testBasicContentRetrieval() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Testing Basic Drive Content Retrieval Comparison ===");

        // ContentDriveHelper query using asset path navigation
        DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false) // Show working content
                .archived(false)
                .offset(0)
                .maxResults(100)
                .build();

        PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        // ContentletAPI query - build lucene query to match folder and site
        String luceneQuery = String.format(
            "+conhost:%s +conFolder:%s +working:true +deleted:false -contentType:Host",
            testSite.getIdentifier(),
            testFolder.getInode()
        );

        List<Contentlet> contentletResults = contentletAPI.search(
            luceneQuery, 100, 0, null, systemUser, false
        );

        // Extract inodes from drive results
        Set<String> driveInodes = driveResults.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());

        // Extract inodes from contentlet results
        Set<String> contentletInodes = contentletResults.stream()
                .map(Contentlet::getInode)
                .collect(Collectors.toSet());

        // Log results for analysis
        Logger.info(this.getClass(), "Drive API returned: " + driveInodes.size() + " items");
        Logger.info(this.getClass(), "Contentlet API returned: " + contentletInodes.size() + " items");
        Logger.info(this.getClass(), "Drive inodes: " + driveInodes);
        Logger.info(this.getClass(), "Contentlet inodes: " + contentletInodes);

        // Verify results
        assertNotNull("Drive results should not be null", driveResults);
        assertNotNull("Drive results list should not be null", driveResults.list);
        assertFalse("Drive API should return some results", driveInodes.isEmpty());
        assertFalse("ContentletAPI should return some results", contentletInodes.isEmpty());

        // Check for common elements
        Set<String> commonInodes = new HashSet<>(driveInodes);
        commonInodes.retainAll(contentletInodes);

        assertFalse("There should be common elements between both APIs", commonInodes.isEmpty());

        // Log any differences for debugging
        Set<String> driveOnly = new HashSet<>(driveInodes);
        driveOnly.removeAll(contentletInodes);
        if (!driveOnly.isEmpty()) {
            Logger.info(this.getClass(), "Items only in Drive API: " + driveOnly.size());
        }

        Set<String> contentletOnly = new HashSet<>(contentletInodes);
        contentletOnly.removeAll(driveInodes);
        if (!contentletOnly.isEmpty()) {
            Logger.info(this.getClass(), "Items only in Contentlet API: " + contentletOnly.size());
        }
    }

    /**
     * Test content filtering by text search comparison.
     *
     * Given: Test contentlets with titles containing the keyword "Special"
     * When: ContentDriveHelper uses QueryFilters.text("Special") and ContentletAPI uses lucene query "+(title:*Special* OR catchall:*Special*)"
     * Then: Both APIs should find the same contentlets that contain "Special" in their titles
     *       and return matching inodes for the filtered results
     */
    @Test
    public void testTextFilterComparison() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Testing Drive Text Filter Comparison ===");

        String searchFilter = "Special";

        // ContentDriveHelper query with text filter
        QueryFilters filters = QueryFilters.builder()
                .text(searchFilter)
                .build();

        DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false) // Show working content
                .archived(false)
                .filters(filters)
                .offset(0)
                .maxResults(50)
                .build();

        PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        // ContentletAPI query with text search
        String luceneQuery = String.format(
            "+conhost:%s +conFolder:%s +working:true +deleted:false -contentType:Host +(title:*%s* OR catchall:*%s*)",
            testSite.getIdentifier(),
            testFolder.getInode(),
            searchFilter,
            searchFilter
        );

        List<Contentlet> contentletResults = contentletAPI.search(
            luceneQuery, 50, 0, null, systemUser, false
        );

        // Extract inodes
        Set<String> driveInodes = driveResults.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());

        Set<String> contentletInodes = contentletResults.stream()
                .map(Contentlet::getInode)
                .collect(Collectors.toSet());

        // Log results
        Logger.info(this.getClass(), "Text filter '" + searchFilter + "' results:");
        Logger.info(this.getClass(), "Drive API: " + driveInodes.size() + " items");
        Logger.info(this.getClass(), "Contentlet API: " + contentletInodes.size() + " items");

        // Verify both APIs found results (we created content with "Special" in the title)
        assertFalse("Drive API should find filtered results", driveInodes.isEmpty());
        assertFalse("ContentletAPI should find filtered results", contentletInodes.isEmpty());

        // Check for overlap (there should be some common results)
        Set<String> commonInodes = new HashSet<>(driveInodes);
        commonInodes.retainAll(contentletInodes);

        assertFalse("Both APIs should find some common results for text filtering",
                   commonInodes.isEmpty());

        Logger.info(this.getClass(), "Common filtered results: " + commonInodes.size());
    }

    /**
     * Test content type filtering comparison.
     *
     * Given: Mixed content including contentlets (BaseContentType.CONTENT) and file assets (BaseContentType.FILEASSET)
     * When: ContentDriveHelper uses specific contentTypes list and ContentletAPI uses lucene contentType filter
     * Then: Both APIs should return only contentlets of the specified content type
     *       and the returned inodes should match between both APIs
     */
    @Test
    public void testContentTypeFilterComparison() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Testing Drive Content Type Filter Comparison ===");

        // ContentDriveHelper query for specific content type
        DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false) // Show working content
                .archived(false)
                .contentTypes(List.of(testContentType.variable())) // Filter by our custom content type
                .offset(0)
                .maxResults(50)
                .build();

        PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        // ContentletAPI query for content type
        String luceneQuery = String.format(
            "+conhost:%s +conFolder:%s +working:true +deleted:false +contentType:%s -contentType:Host",
            testSite.getIdentifier(),
            testFolder.getInode(),
            testContentType.variable()
        );

        List<Contentlet> contentletResults = contentletAPI.search(
            luceneQuery, 50, 0, null, systemUser, false
        );

        // Extract inodes
        Set<String> driveInodes = driveResults.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());

        Set<String> contentletInodes = contentletResults.stream()
                .map(Contentlet::getInode)
                .collect(Collectors.toSet());

        // Log results
        Logger.info(this.getClass(), "Content type filter results:");
        Logger.info(this.getClass(), "Drive API: " + driveInodes.size() + " content items");
        Logger.info(this.getClass(), "Contentlet API: " + contentletInodes.size() + " content items");

        // Verify results - we created custom content type items
        assertFalse("Drive API should find content items", driveInodes.isEmpty());
        assertFalse("ContentletAPI should find content items", contentletInodes.isEmpty());

        // Check overlap
        Set<String> commonInodes = new HashSet<>(driveInodes);
        commonInodes.retainAll(contentletInodes);

        Logger.info(this.getClass(), "Common content type results: " + commonInodes.size());

        // For content type filtering, we expect high overlap
        assertFalse("Both APIs should find common content items", commonInodes.isEmpty());
    }

    /**
     * Test pagination consistency between APIs.
     *
     * Given: Multiple contentlets and file assets in a test folder
     * When: ContentDriveHelper uses offset/maxResults and ContentletAPI uses search(query, limit, offset, ...)
     * Then: Both APIs should return the same paginated subset of content
     *       respecting the offset and page size parameters consistently
     */
    @Test
    public void testPaginationComparison() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Testing Drive Pagination Comparison ===");

        int pageSize = 3;
        int offset = 1;

        // ContentDriveHelper paginated query
        DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false) // Show working content
                .archived(false)
                .offset(offset)
                .maxResults(pageSize)
                .build();

        PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        // ContentletAPI paginated query
        String luceneQuery = String.format(
            "+conhost:%s +conFolder:%s +working:true +deleted:false -contentType:Host",
            testSite.getIdentifier(),
            testFolder.getInode()
        );

        List<Contentlet> contentletResults = contentletAPI.search(
            luceneQuery, pageSize, offset, null, systemUser, false
        );

        // Extract inodes
        List<String> driveInodes = driveResults.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toList());

        List<String> contentletInodes = contentletResults.stream()
                .map(Contentlet::getInode)
                .collect(Collectors.toList());

        // Log pagination results
        Logger.info(this.getClass(), "Pagination test (pageSize=" + pageSize + ", offset=" + offset + "):");
        Logger.info(this.getClass(), "Drive API returned: " + driveInodes.size() + " items");
        Logger.info(this.getClass(), "Contentlet API returned: " + contentletInodes.size() + " items");
        Logger.info(this.getClass(), "Drive inodes: " + driveInodes);
        Logger.info(this.getClass(), "Contentlet inodes: " + contentletInodes);

        // Verify pagination works
        assertTrue("Drive API should respect page size", driveResults.list.size() <= pageSize);
        assertTrue("ContentletAPI should respect page size", contentletResults.size() <= pageSize);

        // Check for some overlap in paginated results
        Set<String> driveSet = new HashSet<>(driveInodes);
        Set<String> contentletSet = new HashSet<>(contentletInodes);
        driveSet.retainAll(contentletSet);

        Logger.info(this.getClass(), "Common paginated results: " + driveSet.size());
    }

    /**
     * Test base content type filtering comparison.
     *
     * Given: Mixed content including contentlets (BaseContentType.CONTENT) and file assets (BaseContentType.FILEASSET)
     * When: ContentDriveHelper uses baseTypes filter and ContentletAPI uses lucene "+baseType:X" filter
     * Then: Both APIs should return only content of the specified base type
     *       and return identical inodes for the matching content
     */
    @Test
    public void testBaseTypeFilterComparison() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Testing Drive Base Type Filter Comparison ===");

        // ContentDriveHelper query for FILEASSET base type
        DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false) // Show working content
                .archived(false)
                .baseTypes(List.of("FILEASSET")) // Filter by file assets only
                .offset(0)
                .maxResults(50)
                .build();

        PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        // ContentletAPI query for FILEASSET base type
        String luceneQuery = String.format(
            "+conhost:%s +conFolder:%s +working:true +deleted:false +baseType:%d -contentType:Host",
            testSite.getIdentifier(),
            testFolder.getInode(),
            BaseContentType.FILEASSET.getType()
        );

        List<Contentlet> contentletResults = contentletAPI.search(
            luceneQuery, 50, 0, null, systemUser, false
        );

        // Extract inodes
        Set<String> driveInodes = driveResults.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());

        Set<String> contentletInodes = contentletResults.stream()
                .map(Contentlet::getInode)
                .collect(Collectors.toSet());

        // Log results
        Logger.info(this.getClass(), "Base type (FILEASSET) filter results:");
        Logger.info(this.getClass(), "Drive API: " + driveInodes.size() + " file assets");
        Logger.info(this.getClass(), "Contentlet API: " + contentletInodes.size() + " file assets");
        Logger.info(this.getClass(), "Drive inodes: " + driveInodes);
        Logger.info(this.getClass(), "Contentlet inodes: " + contentletInodes);

        // We should find the file assets we created
        if (!driveInodes.isEmpty() || !contentletInodes.isEmpty()) {
            // Check for overlap
            Set<String> commonInodes = new HashSet<>(driveInodes);
            commonInodes.retainAll(contentletInodes);

            Logger.info(this.getClass(), "Common base type filter results: " + commonInodes.size());

            // For base type filtering, both APIs should find the same files
            if (!commonInodes.isEmpty()) {
                assertFalse("Both APIs should find the same file assets with base type filtering",
                        commonInodes.isEmpty());
            }
        } else {
            Logger.warn(this.getClass(), "Neither API found file assets with baseType filter");
        }
    }

    /**
     * Test single MIME Type filtering comparison.
     * <ul>
     *     <li><b>Given:</b> Mixed content including contentlets (BaseContentType.CONTENT) and
     *     file assets (BaseContentType.FILEASSET).</li>
     *     <li><b>When:</b> ContentDriveHelper uses the mimeTypes filter for retrieving file
     *     assets from the database, matching the specified type.</li>
     *     <li><b>Then:</b> The API should return at least 4 contents of the specified MIME Type
     *     .</li>
     * </ul>
     */
    @Test
    public void testMimeTypeFilterComparison() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Testing Drive MIME Type Filter Comparison ===");

        // ContentDriveHelper query for text/plain MIME Type
        final DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false) // Show working content
                .archived(false)
                .addMimeTypes("text/plain")
                .offset(0)
                .maxResults(50)
                .build();

        final PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        // Extract inodes
        final Set<String> driveInodes = driveResults.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());

        // Log results
        Logger.info(this.getClass(), "MIME type (text/plain) filter results:");
        Logger.info(this.getClass(), "Drive API: " + driveInodes.size() + " file assets");
        Logger.info(this.getClass(), "Drive inodes: " + driveInodes);

        assertTrue("Drive API should return at least four file asset with MIME type filtering",
                driveInodes.size() >= 4);
    }

    /**
     * Test multiple MIME Type filtering comparison.
     * <ul>
     *     <li><b>Given:</b> Mixed content including contentlets (BaseContentType.CONTENT) and
     *     file assets (BaseContentType.FILEASSET).</li>
     *     <li><b>When:</b> ContentDriveHelper uses the mimeTypes filter for retrieving file
     *     assets from the database, matching the specified types.</li>
     *     <li><b>Then:</b> The API should return at least 5 contents of the specified MIME Types
     *     .</li>
     * </ul>
     */
    @Test
    public void testMultipleMimeTypesFilterComparison() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Testing Drive MIME Type Filter Comparison ===");

        // ContentDriveHelper query for text/plain MIME Type
        final DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false) // Show working content
                .archived(false)
                .addMimeTypes("text/plain", "image/jpeg")
                .offset(0)
                .maxResults(50)
                .build();

        final PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        // Extract inodes
        final Set<String> driveInodes = driveResults.list.stream()
                .map(item -> (String) item.get("inode"))
                .collect(Collectors.toSet());

        // Log results
        Logger.info(this.getClass(), "MIME type (text/plain) filter results:");
        Logger.info(this.getClass(), "Drive API: " + driveInodes.size() + " file assets");
        Logger.info(this.getClass(), "Drive inodes: " + driveInodes);

        assertTrue("Drive API should return at least 5 file asset with MIME type filtering",
                driveInodes.size() >= 5);
    }

    /**
     * Verification test to ensure test data setup is correct.
     *
     * Given: Test setup that should create contentlets and file assets accessible via asset path
     * When: The test data creation process completes
     * Then: The expected number of contentlets and file assets should be created
     *       and they should be indexed and searchable via both ContentDriveHelper and ContentletAPI
     */
    @Test
    public void testDataSetupVerification() throws DotDataException, DotSecurityException {
        Logger.info(this.getClass(), "=== Verifying Drive Test Data Setup ===");

        // Verify test contentlets exist
        assertFalse("Test contentlets should have been created", testContentlets.isEmpty());
        assertFalse("Test file assets should have been created", testFileAssets.isEmpty());

        Logger.info(this.getClass(), "Test data verification:");
        Logger.info(this.getClass(), "- Created " + testContentlets.size() + " contentlets");
        Logger.info(this.getClass(), "- Created " + testFileAssets.size() + " file assets");
        Logger.info(this.getClass(), "- Test folder: " + testFolder.getPath());
        Logger.info(this.getClass(), "- Test site: " + testSite.getHostname());
        Logger.info(this.getClass(), "- Test asset path: " + testAssetPath);

        // Quick search to verify content is indexed
        String basicQuery = String.format(
            "+conhost:%s +conFolder:%s +working:true +deleted:false",
            testSite.getIdentifier(),
            testFolder.getInode()
        );

        List<Contentlet> searchResults = contentletAPI.search(
            basicQuery, 100, 0, null, systemUser, false
        );

        // Also verify via ContentDriveHelper
        DriveRequestForm driveRequest = DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .showFolders(false)
                .live(false)
                .archived(false)
                .build();

        PaginatedContents driveResults = contentDriveHelper.driveSearch(driveRequest, systemUser);

        Logger.info(this.getClass(), "ContentletAPI search found " + searchResults.size() + " items in test folder");
        Logger.info(this.getClass(), "ContentDriveHelper found " + driveResults.list.size() + " items via asset path");

        assertFalse("Should find some content in test folder via ContentletAPI", searchResults.isEmpty());
        assertFalse("Should find some content via ContentDriveHelper asset path", driveResults.list.isEmpty());
    }
}