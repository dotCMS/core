package com.dotcms.rest.api.v2.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.ResponseEntityTagOperationView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.tag.RestTag;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the v2 TagResource, covering tagStorage-aware
 * site handling and UTF-8/Unicode support across create, update, import,
 * and export operations.
 *
 * @author hassandotcms
 */
public class TagResourceIntegrationTest extends IntegrationTestBase {

    private static TagAPI tagAPI;
    private static User systemUser;

    private TagResource resource;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        tagAPI = APILocator.getTagAPI();
        systemUser = APILocator.systemUser();
    }

    @Before
    public void setUp() {
        resource = createTagResource();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v2/tags");
        response = new MockHttpResponse();
    }

    // --- Tag Storage Tests ---

    /**
     * When a site has tagStorage=SYSTEM_HOST, POST should store the tag under SYSTEM_HOST.
     */
    @Test
    public void test_createTag_withSiteWhoseTagStorageIsSystemHost_shouldStoreUnderSystemHost() throws Exception {
        final Host site = createSiteWithTagStorage(Host.SYSTEM_HOST);
        final String tagName = "create-syshost-" + UUIDGenerator.shorty();

        try {
            final List<RestTag> tags = createTagsAndGetCreated(List.of(
                    new TagForm(tagName, site.getIdentifier(), null, null)));

            assertFalse(tags.isEmpty());
            assertEquals(Host.SYSTEM_HOST, tags.get(0).siteId);
        } finally {
            cleanup(tagName, Host.SYSTEM_HOST);
        }
    }

    /**
     * When a site has tagStorage=ownId, POST should store the tag under the site itself.
     */
    @Test
    public void test_createTag_withSiteWhoseTagStorageIsSelf_shouldStoreUnderSite() throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        site.setTagStorage(site.getIdentifier());
        APILocator.getHostAPI().save(site, systemUser, false);

        final String tagName = "create-self-" + UUIDGenerator.shorty();

        try {
            final List<RestTag> tags = createTagsAndGetCreated(List.of(
                    new TagForm(tagName, site.getIdentifier(), null, null)));

            assertFalse(tags.isEmpty());
            assertEquals("Tag should be stored under site's own ID",
                    site.getIdentifier(), tags.get(0).siteId);
        } finally {
            cleanup(tagName, site.getIdentifier());
        }
    }

    /**
     * PUT to change siteId (where tagStorage=SYSTEM_HOST) should resolve through tagStorage
     * and keep the tag visible in GET results.
     */
    @Test
    public void test_updateTag_withTagStorageSystemHost_shouldResolveAndRemainVisible() throws Exception {
        final Host site = createSiteWithTagStorage(Host.SYSTEM_HOST);
        final String tagName = "update-visible-" + UUIDGenerator.shorty();

        try {
            final Tag createdTag = tagAPI.getTagAndCreate(tagName, "", Host.SYSTEM_HOST, false, false);

            final ResponseEntityRestTagView updateResult = resource.updateTag(
                    request, response, createdTag.getTagId(), null,
                    buildUpdateForm(tagName, site.getIdentifier()));

            assertEquals("Updated tag should resolve to SYSTEM_HOST via tagStorage",
                    Host.SYSTEM_HOST, updateResult.getEntity().siteId);

            final List<?> items = listTags(tagName, Host.SYSTEM_HOST);
            assertFalse("Tag should be visible in GET after PUT", items.isEmpty());
        } finally {
            cleanup(tagName, Host.SYSTEM_HOST);
        }
    }

    /**
     * PUT to move a tag from SYSTEM_HOST to a site with tagStorage=ownId
     * should store under the site and be visible when listing that site.
     */
    @Test
    public void test_updateTag_moveBetweenSites_withDifferentTagStorage() throws Exception {
        final Host siteB = new SiteDataGen().nextPersisted();
        siteB.setTagStorage(siteB.getIdentifier());
        APILocator.getHostAPI().save(siteB, systemUser, false);

        final String tagName = "move-sites-" + UUIDGenerator.shorty();

        try {
            final Tag createdTag = tagAPI.getTagAndCreate(tagName, "", Host.SYSTEM_HOST, false, false);

            final ResponseEntityRestTagView updateResult = resource.updateTag(
                    request, response, createdTag.getTagId(), null,
                    buildUpdateForm(tagName, siteB.getIdentifier()));

            assertEquals("Tag should move to site B's own storage",
                    siteB.getIdentifier(), updateResult.getEntity().siteId);

            final List<?> items = listTags(tagName, siteB.getIdentifier());
            assertFalse("Tag should be visible under site B", items.isEmpty());
        } finally {
            cleanup(tagName, siteB.getIdentifier());
            cleanup(tagName, Host.SYSTEM_HOST);
        }
    }

    /**
     * GET with siteId filter should find tags in the site's effective tag storage.
     */
    @Test
    public void test_listTags_filterBySiteId_findsTagsInEffectiveStorage() throws Exception {
        final Host site = createSiteWithTagStorage(Host.SYSTEM_HOST);
        final String tagName = "filter-site-" + UUIDGenerator.shorty();

        try {
            tagAPI.getTagAndCreate(tagName, "", Host.SYSTEM_HOST, false, false);

            final List<?> items = listTags(tagName, true, site.getIdentifier());
            assertFalse("GET with siteId should find tags in effective tag storage",
                    items.isEmpty());
        } finally {
            cleanup(tagName, Host.SYSTEM_HOST);
        }
    }

    /**
     * Regression: SYSTEM_HOST tags should continue to work normally
     * through create, rename, and list.
     */
    @Test
    public void test_createAndUpdateTag_onSystemHost_shouldWorkNormally() throws Exception {
        final String tagName = "syshost-reg-" + UUIDGenerator.shorty();
        final String renamedName = "syshost-ren-" + UUIDGenerator.shorty();

        try {
            final List<RestTag> created = createTagsAndGetCreated(List.of(
                    new TagForm(tagName, Host.SYSTEM_HOST, null, null)));
            final String tagId = created.get(0).id;

            final ResponseEntityRestTagView updateResult = resource.updateTag(
                    request, response, tagId, null,
                    buildUpdateForm(renamedName, Host.SYSTEM_HOST));

            assertEquals(Host.SYSTEM_HOST, updateResult.getEntity().siteId);
            assertEquals(renamedName, updateResult.getEntity().label);

            final List<?> items = listTags(renamedName, Host.SYSTEM_HOST);
            assertFalse("Renamed tag should be visible", items.isEmpty());
        } finally {
            cleanup(tagName, Host.SYSTEM_HOST);
            cleanup(renamedName, Host.SYSTEM_HOST);
        }
    }

    // --- UTF-8 / Unicode Tests ---

    /**
     * Bulk creation of tags across multiple Unicode scripts (Chinese, Japanese,
     * Arabic, Korean, Cyrillic, mixed) should all succeed and be searchable.
     */
    @Test
    public void test_createTags_withMultipleUnicodeScripts_shouldSucceed() throws Exception {
        final String chinese = "百度一下-" + UUIDGenerator.shorty();
        final String japanese = "東京タワー-" + UUIDGenerator.shorty();
        final String arabic = "مرحبا-" + UUIDGenerator.shorty();
        final String korean = "태그테스트-" + UUIDGenerator.shorty();
        final String cyrillic = "тегтест-" + UUIDGenerator.shorty();
        final String mixed = "dotCMS-百度-タグ-" + UUIDGenerator.shorty();
        final List<String> allNames = List.of(chinese, japanese, arabic, korean, cyrillic, mixed);

        try {
            final List<TagForm> forms = List.of(
                    new TagForm(chinese, Host.SYSTEM_HOST, null, null),
                    new TagForm(japanese, Host.SYSTEM_HOST, null, null),
                    new TagForm(arabic, Host.SYSTEM_HOST, null, null),
                    new TagForm(korean, Host.SYSTEM_HOST, null, null),
                    new TagForm(cyrillic, Host.SYSTEM_HOST, null, null),
                    new TagForm(mixed, Host.SYSTEM_HOST, null, null));

            final List<RestTag> created = createTagsAndGetCreated(forms);
            assertEquals("All 6 Unicode tags should be created", 6, created.size());

            // Verify Chinese tag is searchable
            final List<?> found = listTags("百度", Host.SYSTEM_HOST);
            assertFalse("Chinese tag should be findable via search", found.isEmpty());
        } finally {
            allNames.forEach(name -> cleanup(name.toLowerCase(), Host.SYSTEM_HOST));
        }
    }

    /**
     * A UTF-8 tag should be renamable to another UTF-8 name and remain searchable.
     */
    @Test
    public void test_updateTag_renameUtf8ToUtf8_shouldSucceed() throws Exception {
        final String originalName = "百度一下-" + UUIDGenerator.shorty();
        final String renamedName = "谷歌搜索-" + UUIDGenerator.shorty();

        try {
            final Tag created = tagAPI.getTagAndCreate(
                    originalName, "", Host.SYSTEM_HOST, false, false);

            final ResponseEntityRestTagView result = resource.updateTag(
                    request, response, created.getTagId(), null,
                    buildUpdateForm(renamedName, Host.SYSTEM_HOST));

            assertEquals(renamedName.toLowerCase(), result.getEntity().label);

            final List<?> found = listTags("谷歌", Host.SYSTEM_HOST);
            assertFalse("Renamed UTF-8 tag should be findable", found.isEmpty());
        } finally {
            cleanup(originalName.toLowerCase(), Host.SYSTEM_HOST);
            cleanup(renamedName.toLowerCase(), Host.SYSTEM_HOST);
        }
    }

    /**
     * Importing a CSV with multi-script tag names (Japanese, Korean, Cyrillic)
     * should create all tags and persist them correctly in the database.
     */
    @Test
    public void test_importTags_withMultipleUnicodeScripts_shouldCreateAll() throws Exception {
        final String japanese = "東京タワー-" + UUIDGenerator.shorty();
        final String korean = "태그테스트-" + UUIDGenerator.shorty();
        final String cyrillic = "тегтест-" + UUIDGenerator.shorty();
        final List<String> allNames = List.of(japanese, korean, cyrillic);

        try {
            final String csv = buildCsv(allNames, Host.SYSTEM_HOST);
            final Map<String, Object> stats = importCsvAndGetStats(csv);
            assertEquals("All 3 tags should import", 3, stats.get("successCount"));

            for (final String name : allNames) {
                assertNotNull(name + " should exist in DB",
                        tagAPI.getTagByNameAndHost(name.toLowerCase(), Host.SYSTEM_HOST));
            }
        } finally {
            allNames.forEach(name -> cleanup(name.toLowerCase(), Host.SYSTEM_HOST));
        }
    }

    /**
     * Exporting tags as JSON should preserve UTF-8 characters in the output.
     */
    @Test
    public void test_exportTagsJson_withUtf8Tags_shouldPreserveCharacters() throws Exception {
        final String tag = "json导出-" + UUIDGenerator.shorty();

        try {
            tagAPI.getTagAndCreate(tag, "", Host.SYSTEM_HOST, false, false);

            final Response result = resource.exportTags(
                    request, response, "json", false, Host.SYSTEM_HOST, tag);

            assertEquals(200, result.getStatus());
            final String json = streamToString(result);
            assertTrue("JSON should contain UTF-8 tag", json.contains(tag.toLowerCase()));
            assertTrue("JSON should have tags array", json.contains("\"tags\""));
        } finally {
            cleanup(tag.toLowerCase(), Host.SYSTEM_HOST);
        }
    }

    /**
     * Round-trip: export UTF-8 tags as CSV, verify the output preserves characters,
     * then re-import — should detect duplicates, not create new tags.
     */
    @Test
    public void test_exportCsvThenReimport_withUtf8Tags_shouldRoundTrip() throws Exception {
        final String tag1 = "往返测试-" + UUIDGenerator.shorty();
        final String tag2 = "ラウンドトリップ-" + UUIDGenerator.shorty();

        try {
            tagAPI.getTagAndCreate(tag1, "", Host.SYSTEM_HOST, false, false);
            tagAPI.getTagAndCreate(tag2, "", Host.SYSTEM_HOST, false, false);

            // Export as CSV
            final Response exportResult = resource.exportTags(
                    request, response, "csv", false, Host.SYSTEM_HOST, null);
            assertEquals(200, exportResult.getStatus());

            final String csv = streamToString(exportResult);
            assertTrue("CSV should contain tag1", csv.contains(tag1.toLowerCase()));
            assertTrue("CSV should contain tag2", csv.contains(tag2.toLowerCase()));

            // Re-import — all should be duplicates
            final Map<String, Object> stats = importCsvAndGetStats(csv);
            assertTrue("Should detect duplicates",
                    (int) stats.get("duplicateCount") >= 2);
            assertEquals("No new tags on re-import",
                    0, stats.get("successCount"));
        } finally {
            cleanup(tag1.toLowerCase(), Host.SYSTEM_HOST);
            cleanup(tag2.toLowerCase(), Host.SYSTEM_HOST);
        }
    }

    // --- Helpers ---

    /**
     * Creates tags via the resource and extracts the "created" list from the response.
     */
    @SuppressWarnings("unchecked")
    private List<RestTag> createTagsAndGetCreated(final List<TagForm> forms) {
        final Response result = resource.createTags(request, response, forms);
        assertEquals(200, result.getStatus());
        final ResponseEntityView<Map<String, Object>> entity =
                (ResponseEntityView<Map<String, Object>>) result.getEntity();
        return (List<RestTag>) entity.getEntity().get("created");
    }

    /**
     * Lists tags by filter and siteId, returns the entity list.
     */
    private List<?> listTags(final String filter, final String siteId) {
        return listTags(filter, false, siteId);
    }

    private List<?> listTags(final String filter, final boolean global, final String siteId) {
        final ResponseEntityPaginatedDataView result = resource.list(
                request, response, filter, global, siteId,
                1, 25, "tagname", "ASC");
        return (List<?>) result.getEntity();
    }

    /**
     * Imports a CSV string and returns the operation stats map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> importCsvAndGetStats(final String csvContent) throws Exception {
        final FormDataMultiPart multipart = createMultipartWithCsv(csvContent);
        final ResponseEntityTagOperationView result =
                resource.importTags(request, response, multipart);
        assertNotNull(result);
        return (Map<String, Object>) result.getEntity();
    }

    /**
     * Reads a StreamingOutput response into a UTF-8 string.
     */
    private String streamToString(final Response resp) throws Exception {
        final StreamingOutput streaming = (StreamingOutput) resp.getEntity();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streaming.write(baos);
        return baos.toString(StandardCharsets.UTF_8);
    }

    /**
     * Builds a CSV string with "Tag Name,Host ID" header and one row per tag name.
     */
    private String buildCsv(final List<String> tagNames, final String hostId) {
        final StringBuilder sb = new StringBuilder("Tag Name,Host ID\n");
        for (final String name : tagNames) {
            sb.append(String.format("\"%s\",\"%s\"\n", name, hostId));
        }
        return sb.toString();
    }

    private UpdateTagForm buildUpdateForm(final String tagName, final String siteId) {
        return new UpdateTagForm.Builder()
                .tagName(tagName)
                .siteId(siteId)
                .build();
    }

    private FormDataMultiPart createMultipartWithCsv(final String csvContent) throws Exception {

        final File tempDir = Files.createTempDirectory("tmp_upload_test").toFile();
        tempDir.deleteOnExit();
        final File csvFile = new File(tempDir, "tags-import.csv");
        csvFile.deleteOnExit();
        Files.write(csvFile.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));

        final InputStream fileInputStream = Files.newInputStream(csvFile.toPath());
        final FormDataBodyPart bodyPart = mock(FormDataBodyPart.class);
        when(bodyPart.getEntityAs(InputStream.class)).thenReturn(fileInputStream);

        final ContentDisposition disposition = ContentDisposition
                .type("form-data")
                .fileName("tags-import.csv")
                .build();
        when(bodyPart.getContentDisposition()).thenReturn(disposition);

        final FormDataMultiPart multipart = mock(FormDataMultiPart.class);
        when(multipart.getFields("file")).thenReturn(List.of(bodyPart));
        when(multipart.getBodyParts()).thenReturn(List.of(bodyPart));

        return multipart;
    }

    private TagResource createTagResource() {
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(systemUser);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);

        return new TagResource(
                tagAPI,
                APILocator.getHostAPI(),
                APILocator.getFolderAPI(),
                webResource
        );
    }

    private Host createSiteWithTagStorage(final String tagStorageId) throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        site.setTagStorage(tagStorageId);
        APILocator.getHostAPI().save(site, systemUser, false);
        return site;
    }

    private void cleanup(final String tagName, final String hostId) {
        try {
            final Tag tag = tagAPI.getTagByNameAndHost(tagName, hostId);
            if (tag != null) {
                tagAPI.deleteTag(tag.getTagId());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
