package com.dotcms.rest.api.v2.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.ResponseEntityRestTagListView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.tag.RestTag;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the v2 TagResource, focused on tagStorage-aware
 * site handling across create, update, and list operations.
 *
 * @author hassandotcms
 */
public class TagResourceIntegrationTest extends IntegrationTestBase {

    private static TagAPI tagAPI;
    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        tagAPI = APILocator.getTagAPI();
        systemUser = APILocator.systemUser();
    }

    /**
     * When a site has tagStorage=SYSTEM_HOST, POST should store the tag under SYSTEM_HOST.
     */
    @Test
    public void test_createTag_withSiteWhoseTagStorageIsSystemHost_shouldStoreUnderSystemHost() throws Exception {
        final Host site = createSiteWithTagStorage(Host.SYSTEM_HOST);
        final String tagName = "create-syshost-" + UUIDGenerator.shorty();

        try {
            final TagResource resource = createTagResource();
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2/tags");
            final HttpServletResponse response = new MockHttpResponse();

            final List<TagForm> forms = List.of(
                    new TagForm(tagName, site.getIdentifier(), null, null));
            final Response result = resource.createTags(request, response, forms);

            assertEquals(201, result.getStatus());
            final ResponseEntityRestTagListView entity =
                    (ResponseEntityRestTagListView) result.getEntity();
            final List<RestTag> tags = entity.getEntity();
            assertFalse("Should return created tag", tags.isEmpty());
            assertEquals("Tag should be stored under SYSTEM_HOST per tagStorage",
                    Host.SYSTEM_HOST, tags.get(0).siteId);

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
            final TagResource resource = createTagResource();
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2/tags");
            final HttpServletResponse response = new MockHttpResponse();

            final List<TagForm> forms = List.of(
                    new TagForm(tagName, site.getIdentifier(), null, null));
            final Response result = resource.createTags(request, response, forms);

            assertEquals(201, result.getStatus());
            final ResponseEntityRestTagListView entity =
                    (ResponseEntityRestTagListView) result.getEntity();
            final List<RestTag> tags = entity.getEntity();
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
            // Create tag under SYSTEM_HOST
            final Tag createdTag = tagAPI.getTagAndCreate(tagName, "", Host.SYSTEM_HOST, false, false);
            assertNotNull(createdTag);
            assertEquals(Host.SYSTEM_HOST, createdTag.getHostId());

            final TagResource resource = createTagResource();
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2/tags");
            final HttpServletResponse response = new MockHttpResponse();

            // PUT: update tag siteId to the site (whose tagStorage=SYSTEM_HOST)
            final UpdateTagForm updateForm = new UpdateTagForm.Builder()
                    .tagName(tagName)
                    .siteId(site.getIdentifier())
                    .build();

            final ResponseEntityRestTagView updateResult =
                    resource.updateTag(request, response, createdTag.getTagId(), null, updateForm);

            assertNotNull(updateResult);
            assertEquals("Updated tag should resolve to SYSTEM_HOST via tagStorage",
                    Host.SYSTEM_HOST, updateResult.getEntity().siteId);

            // Verify tag is still visible in list
            final ResponseEntityPaginatedDataView listResult = resource.list(
                    request, response, tagName, false, Host.SYSTEM_HOST,
                    1, 25, "tagname", "ASC");

            assertNotNull(listResult);
            final List<?> items = (List<?>) listResult.getEntity();
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
            // Create tag under SYSTEM_HOST
            final Tag createdTag = tagAPI.getTagAndCreate(tagName, "", Host.SYSTEM_HOST, false, false);
            assertEquals(Host.SYSTEM_HOST, createdTag.getHostId());

            final TagResource resource = createTagResource();
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2/tags");
            final HttpServletResponse response = new MockHttpResponse();

            // PUT: move tag to site B (tagStorage=own ID)
            final UpdateTagForm updateForm = new UpdateTagForm.Builder()
                    .tagName(tagName)
                    .siteId(siteB.getIdentifier())
                    .build();

            final ResponseEntityRestTagView updateResult =
                    resource.updateTag(request, response, createdTag.getTagId(), null, updateForm);

            assertNotNull(updateResult);
            assertEquals("Tag should move to site B's own storage",
                    siteB.getIdentifier(), updateResult.getEntity().siteId);

            // Verify visible when listing site B
            final ResponseEntityPaginatedDataView listResult = resource.list(
                    request, response, tagName, false, siteB.getIdentifier(),
                    1, 25, "tagname", "ASC");

            assertNotNull(listResult);
            final List<?> items = (List<?>) listResult.getEntity();
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
            // Create tag under SYSTEM_HOST
            tagAPI.getTagAndCreate(tagName, "", Host.SYSTEM_HOST, false, false);

            final TagResource resource = createTagResource();
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2/tags");
            final HttpServletResponse response = new MockHttpResponse();

            // GET with siteId (whose tagStorage=SYSTEM_HOST) and global=true
            final ResponseEntityPaginatedDataView listResult = resource.list(
                    request, response, tagName, true, site.getIdentifier(),
                    1, 25, "tagname", "ASC");

            assertNotNull(listResult);
            final List<?> items = (List<?>) listResult.getEntity();
            assertFalse("GET with siteId should find tags in effective tag storage",
                    items.isEmpty());

        } finally {
            cleanup(tagName, Host.SYSTEM_HOST);
        }
    }

    /**
     * Regression: SYSTEM_HOST tags should continue to work normally.
     */
    @Test
    public void test_createAndUpdateTag_onSystemHost_shouldWorkNormally() throws Exception {
        final String tagName = "syshost-reg-" + UUIDGenerator.shorty();
        final String renamedName = "syshost-ren-" + UUIDGenerator.shorty();

        try {
            final TagResource resource = createTagResource();
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2/tags");
            final HttpServletResponse response = new MockHttpResponse();

            // Create on SYSTEM_HOST
            final List<TagForm> forms = List.of(
                    new TagForm(tagName, Host.SYSTEM_HOST, null, null));
            final Response createResult = resource.createTags(request, response, forms);
            assertEquals(201, createResult.getStatus());

            final ResponseEntityRestTagListView createEntity =
                    (ResponseEntityRestTagListView) createResult.getEntity();
            final String tagId = createEntity.getEntity().get(0).id;

            // Rename, keep on SYSTEM_HOST
            final UpdateTagForm updateForm = new UpdateTagForm.Builder()
                    .tagName(renamedName)
                    .siteId(Host.SYSTEM_HOST)
                    .build();

            final ResponseEntityRestTagView updateResult =
                    resource.updateTag(request, response, tagId, null, updateForm);

            assertEquals(Host.SYSTEM_HOST, updateResult.getEntity().siteId);
            assertEquals(renamedName, updateResult.getEntity().label);

            // Verify visible in list
            final ResponseEntityPaginatedDataView listResult = resource.list(
                    request, response, renamedName, false, Host.SYSTEM_HOST,
                    1, 25, "tagname", "ASC");

            final List<?> items = (List<?>) listResult.getEntity();
            assertFalse("Renamed tag should be visible", items.isEmpty());

        } finally {
            cleanup(tagName, Host.SYSTEM_HOST);
            cleanup(renamedName, Host.SYSTEM_HOST);
        }
    }

    // --- Helpers ---

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
