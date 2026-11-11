package com.dotcms.rest;

import com.dotcms.test.util.ApiTokenTestUtils;
import org.junit.ClassRule;

import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockAsyncResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.PushedAssetDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BundleResourceTest {

    @ClassRule
    public static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

    private static BundleResource bundleResource;
    private static User adminUser;
    static HttpServletResponse response;
    private static final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        bundleResource = new BundleResource();
        adminUser = APILocator.systemUser();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        if (!roleAPI.doesUserHaveRole(adminUser, roleAPI.loadBackEndUserRole())) {
            roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), adminUser);
        }
        response = new MockHttpResponse();
    }

    @AfterClass
    public static void cleanup() {
        for (final User user : createdUsers) {
            UserDataGen.remove(user, Boolean.TRUE);
        }
        createdUsers.clear();
    }

    /**
     * Method to Test: {@link BundleResource#uploadBundleSync(HttpServletRequest, HttpServletResponse, FormDataMultiPart)}
     *                  and {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File, Boolean)}
     * When: Send a PushPublish request with two content related, and later sned another Push Publish request removing the
     *       relationships between this two content
     * Should: Clean the relationship cache
     */
    @Test
    public void shouldCleanRelationshipCache() throws FileNotFoundException, DotPublisherException, DotDataException, DotCacheException {

        final File createRelationshipContentBundle = new File(getClass().getResource(
                "/bundle/relationship_push_publish/create_relationship_content.tar.gz")
                .getFile());

        final File removeRelationshipContentBundle = new File(getClass().getResource(
                "/bundle/relationship_push_publish/remove_relationship_in_content.tar.gz")
                .getFile());

        final FileInputStream createCOntentFileInputStream = new FileInputStream(createRelationshipContentBundle);
        final FileInputStream removeRelationshipFileInputStream = new FileInputStream(removeRelationshipContentBundle);

        publish(createCOntentFileInputStream);

        final Contentlet contentlet =
                APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage("8c66b23e-daa0-40e4-9991-d1a60e72d2f4");

        APILocator.getContentletAPI().getAllRelationships(contentlet);
        assertNotNull(CacheLocator.getRelationshipCache().getRelatedContentMap("8c66b23e-daa0-40e4-9991-d1a60e72d2f4"));

        publish(removeRelationshipFileInputStream);

        assertNull(CacheLocator.getRelationshipCache().getRelatedContentMap("8c66b23e-daa0-40e4-9991-d1a60e72d2f4"));
    }

    private void publish(FileInputStream createCOntentFileInputStream) throws DotPublisherException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());

        final BodyPart bodyPart = mock(BodyPart.class);
        when(bodyPart.getEntity()).thenReturn(createCOntentFileInputStream);

        final ContentDisposition meta = mock(ContentDisposition.class);
        when(bodyPart.getContentDisposition()).thenReturn(meta);
        when(meta.getFileName()).thenReturn("test_file.tar.gz");

        final FormDataMultiPart multipart = mock(FormDataMultiPart.class);
        when(multipart.getBodyParts()).thenReturn(list(bodyPart));

        bundleResource.uploadBundleSync(request, response, multipart);
    }

    private static void createFilter(){
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true);
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor("filterTestAPI.yml","Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");

        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }

    private String insertPublishingBundle(final String userId, final Date publishDate)
            throws DotDataException {
        final String uuid = UUIDGenerator.generateUuid();
        final Bundle bundle = new Bundle();
        bundle.setId(uuid);
        bundle.setName("testBundle"+System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(userId);
        bundle.setPublishDate(publishDate);
        APILocator.getBundleAPI().saveBundle(bundle);

        return bundle.getId();
    }

    /**
     * BasicAuth
     */
    private HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                TOKEN_UTILS.adminBearerHeader());

        return request;
    }

    /**
     * Method to Test: {@link BundleResource#generateBundle(HttpServletRequest, HttpServletResponse, AsyncResponse, GenerateBundleForm)}
     * When: Create a bundle and generate the tar.gz file of the given bundle.
     * Should: Generate the bundle without issues, 200.
     */
    @Test
    public void test_generateBundle_success() throws DotDataException {
        //Create new bundle
        final String bundleId = insertPublishingBundle(adminUser.getUserId(),new Date());

        //Create a Filter since it's needed to generate the bundle
        createFilter();

        //Create GenerateBundleForm
        final GenerateBundleForm bundleForm = new GenerateBundleForm.Builder().bundleId(bundleId).build();

        //Call generate endpoint
        final AsyncResponse asyncResponse = new MockAsyncResponse((arg) -> {

            final Response generateBundleResponse = (Response)arg;
            assertEquals(Status.OK.getStatusCode(), generateBundleResponse.getStatus());
            return true;
        }, arg -> {
            fail("Error generating bundle");
            return true;
        });

        bundleResource.generateBundle(getHttpRequest(),response,asyncResponse,bundleForm);
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();
    }

    /**
     * Method to Test: {@link BundleResource#downloadBundle(HttpServletRequest, HttpServletResponse, String)}
     * When: Create a bundle and try to download it, but since the tar.gz has not been generated should fail.
     * Should: return 404 since the file has not been generated
     */
    @Test
    public void test_downloadBundle_fileNotGenerated_return404() throws DotDataException {
        //Create new bundle
        final String bundleId = insertPublishingBundle(adminUser.getUserId(),new Date());

        //Call download endpoint
        final Response responseResource = bundleResource.downloadBundle(getHttpRequest(),response,bundleId);

        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(),responseResource.getStatus());
    }

    /**
     * Method to Test: {@link BundleResource#downloadBundle(HttpServletRequest, HttpServletResponse, String)}
     * When: Create a bundle and try to download it, since the tar.gz has been generated should succeed.
     * Should: return 200 since the file has been generated
     */
    @Test
    public void test_downloadBundle_success() throws DotDataException {
        //Create new bundle
        final String bundleId = insertPublishingBundle(adminUser.getUserId(),new Date());

        //Create a Filter since it's needed to generate the bundle
        createFilter();

        //Generate bundle file
        final Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
        bundle.setOperation(PushPublisherConfig.Operation.PUBLISH.ordinal());
        APILocator.getBundleAPI().generateTarGzipBundleFile(bundle);

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).clearFilterDescriptorList();

        //Call download endpoint
        final Response responseResource = bundleResource.downloadBundle(getHttpRequest(),response,bundleId);

        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
    }

    /**
     * Method to Test: {@link BundleResource#deleteAll(HttpServletRequest, HttpServletResponse)}
     * When: Bundles in every status
     * Should: Delete all bundles
     */
    @Test
    public void test_deleteAll() throws DotDataException, DotPublisherException {
        final List<String> bundleIds = new ArrayList<>();

        for(PublishAuditStatus.Status status :PublishAuditStatus.Status.values()) {
            final String bundleId = insertPublishingBundle(adminUser.getUserId(),new Date());
            insertPublishAuditStatus(status,bundleId);
            assertNotNull(APILocator.getBundleAPI().getBundleById(bundleId));
            bundleIds.add(bundleId);
        }

        final long currentTime = System.currentTimeMillis();
        final Response responseResource = bundleResource.deleteAll(getHttpRequest(),response);
        assertEquals(200, responseResource.getStatus());

        for(int i=0; i<18; i++) { // max 3 min
            if(SystemEventsFactory.getInstance().getSystemEventsAPI()
                    .getEventsSince(currentTime).stream().anyMatch(systemEvent ->
                            SystemEventType.DELETE_BUNDLE==systemEvent.getEventType()
                    && systemEvent.getPayload().getData().toString().contains("Bundles successfully deleted"))) {
                break;
            }
            Try.run(()->Thread.sleep(10000));
        }

        for (final String bundleId : bundleIds) {
            assertNull(APILocator.getBundleAPI().getBundleById(bundleId));
        }
    }

    private void insertPublishAuditStatus(final PublishAuditStatus.Status status, final String bundleID) throws DotPublisherException {
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleID);
        publishAuditStatus.setStatusPojo(new PublishAuditHistory());
        publishAuditStatus.setStatus(status);
        APILocator.getPublishAuditAPI().insertPublishAuditStatus(publishAuditStatus);
    }

    private static HttpServletRequest mockRequestFor(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        return request;
    }

    private static User newBackendUser() throws Exception {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        createdUsers.add(user);
        return user;
    }

    /**
     * Reproduces the horizontal information-disclosure gap in issue #36415.
     *
     * Method to Test: {@link BundleResource#getUnsendBundles(HttpServletRequest, HttpServletResponse, String)}
     * Given: a non-admin backend user
     * When:  they request the unsent bundles of ANOTHER user via the path userId
     * Should: be rejected with 403 Forbidden (a user may only list their own drafts).
     *         Before the fix this returned 200 with the other user's bundles.
     */
    @Test(expected = ForbiddenException.class)
    public void test_getUnsendBundles_otherUsersId_isForbidden() throws Exception {
        final User userA = newBackendUser();
        final User userB = newBackendUser();

        bundleResource.getUnsendBundles(
                mockRequestFor(userA), response, "userid/" + userB.getUserId());
    }

    /**
     * Method to Test: {@link BundleResource#getUnsendBundles(HttpServletRequest, HttpServletResponse, String)}
     * Given: a non-admin backend user
     * When:  they request their OWN unsent bundles
     * Should: succeed (200) - the shipped Add-to-Bundle flow always queries the caller's own id.
     */
    @Test
    public void test_getUnsendBundles_ownId_isAllowed() throws Exception {
        final User userA = newBackendUser();

        final Response resp = bundleResource.getUnsendBundles(
                mockRequestFor(userA), response, "userid/" + userA.getUserId());

        assertEquals(200, resp.getStatus());
    }

    /**
     * Method to Test: {@link BundleResource#getUnsendBundles(HttpServletRequest, HttpServletResponse, String)}
     * Given: a CMS Administrator
     * When:  they request another user's unsent bundles
     * Should: succeed (200) - admins may query any user's drafts.
     */
    @Test
    public void test_getUnsendBundles_adminCanQueryAnyUser() throws Exception {
        final User userB = newBackendUser();

        final Response resp = bundleResource.getUnsendBundles(
                mockRequestFor(APILocator.systemUser()), response, "userid/" + userB.getUserId());

        assertEquals(200, resp.getStatus());
    }

    @Test
    public void test_deletePushHistory_userWithoutPublishPermission_isForbidden() throws Exception {
        final User user = newBackendUser();
        final Contentlet contentlet = newContentlet();
        pushHistoryFor(contentlet.getIdentifier(), new EnvironmentDataGen().nextPersisted());

        assertThrows(ForbiddenException.class, () -> bundleResource.deletePushHistory(
                mockRequestFor(user), response, "assetid/" + contentlet.getIdentifier()));

        assertFalse(APILocator.getPushedAssetsAPI().getPushedAssets(contentlet.getIdentifier()).isEmpty());
    }

    @Test
    public void test_deletePushHistory_userWithOnlyEditPermission_isForbidden() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), role)
                .nextPersisted();
        createdUsers.add(user);
        final Contentlet contentlet = newContentlet();
        APILocator.getPermissionAPI().save(
                new Permission(contentlet.getPermissionId(), role.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true),
                contentlet, APILocator.systemUser(), false);
        pushHistoryFor(contentlet.getIdentifier(), new EnvironmentDataGen().nextPersisted());

        assertThrows(ForbiddenException.class, () -> bundleResource.deletePushHistory(
                mockRequestFor(user), response, "assetid/" + contentlet.getIdentifier()));

        assertFalse(APILocator.getPushedAssetsAPI().getPushedAssets(contentlet.getIdentifier()).isEmpty());
    }

    @Test
    public void test_deletePushHistory_userWithPublishPermission_deletesHistory() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), role)
                .nextPersisted();
        createdUsers.add(user);
        final Contentlet contentlet = newContentlet();
        APILocator.getPermissionAPI().save(
                new Permission(contentlet.getPermissionId(), role.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH, true),
                contentlet, APILocator.systemUser(), false);
        pushHistoryFor(contentlet.getIdentifier(), new EnvironmentDataGen().nextPersisted());

        final Response resp = bundleResource.deletePushHistory(
                mockRequestFor(user), response, "assetid/" + contentlet.getIdentifier());

        assertEquals(200, resp.getStatus());
        assertTrue(APILocator.getPushedAssetsAPI().getPushedAssets(contentlet.getIdentifier()).isEmpty());
    }

    @Test
    public void test_deleteEnvironmentPushHistory_userWithoutConfigurationAccess_isForbidden() throws Exception {
        final User user = newBackendUser();
        final Contentlet contentlet = newContentlet();
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        pushHistoryFor(contentlet.getIdentifier(), environment);

        assertThrows(ForbiddenException.class, () -> bundleResource.deleteEnvironmentPushHistory(
                mockRequestFor(user), response, "environmentid/" + environment.getId()));

        assertFalse(APILocator.getPushedAssetsAPI().getPushedAssets(contentlet.getIdentifier()).isEmpty());
    }

    @Test
    public void test_deleteEnvironmentPushHistory_admin_deletesHistory() throws Exception {
        final Contentlet contentlet = newContentlet();
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        pushHistoryFor(contentlet.getIdentifier(), environment);

        final Response resp = bundleResource.deleteEnvironmentPushHistory(
                mockRequestFor(APILocator.systemUser()), response, "environmentid/" + environment.getId());

        assertEquals(200, resp.getStatus());
        assertTrue(APILocator.getPushedAssetsAPI().getPushedAssets(contentlet.getIdentifier()).isEmpty());
    }

    @Test
    public void test_deletePushHistory_unknownAsset_isNotFound() throws Exception {
        final User user = newBackendUser();
        final String assetId = UUIDGenerator.generateUuid();
        pushHistoryFor(assetId, new EnvironmentDataGen().nextPersisted());

        assertThrows(NotFoundException.class, () -> bundleResource.deletePushHistory(
                mockRequestFor(user), response, "assetid/" + assetId));

        assertFalse(APILocator.getPushedAssetsAPI().getPushedAssets(assetId).isEmpty());
    }

    @Test
    public void test_deletePushHistory_linkPublisher_deletesHistory() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), role)
                .nextPersisted();
        createdUsers.add(user);
        final Host site = new SiteDataGen().nextPersisted();
        final Link link = new LinkDataGen().hostId(site.getIdentifier())
                .parent(new FolderDataGen().site(site).nextPersisted())
                .target("https://www.dotcms.com").linkType("EXTERNAL").nextPersisted();
        APILocator.getPermissionAPI().save(
                new Permission(link.getPermissionId(), role.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH, true),
                link, APILocator.systemUser(), false);
        pushHistoryFor(link.getIdentifier(), new EnvironmentDataGen().nextPersisted());

        final Response resp = bundleResource.deletePushHistory(
                mockRequestFor(user), response, "assetid/" + link.getIdentifier());

        assertEquals(200, resp.getStatus());
        assertTrue(APILocator.getPushedAssetsAPI().getPushedAssets(link.getIdentifier()).isEmpty());
    }

    @Test
    public void test_deletePushHistory_categoryPublisher_deletesHistory() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), role)
                .nextPersisted();
        createdUsers.add(user);
        final Category category = new CategoryDataGen()
                .setCategoryName("category-" + System.currentTimeMillis())
                .setKey("category-" + System.currentTimeMillis())
                .nextPersisted();
        APILocator.getPermissionAPI().save(
                new Permission(category.getPermissionId(), role.getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH, true),
                category, APILocator.systemUser(), false);
        pushHistoryFor(category.getInode(), new EnvironmentDataGen().nextPersisted());

        final Response resp = bundleResource.deletePushHistory(
                mockRequestFor(user), response, "assetid/" + category.getInode());

        assertEquals(200, resp.getStatus());
        assertTrue(APILocator.getPushedAssetsAPI().getPushedAssets(category.getInode()).isEmpty());
    }

    @Test
    public void test_deleteEnvironmentPushHistory_userWithConfigurationAccess_deletesHistory() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final Layout layout = new LayoutDataGen().portletIds(PortletID.CONFIGURATION.toString()).nextPersisted();
        APILocator.getRoleAPI().addLayoutToRole(layout, role);
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), role)
                .nextPersisted();
        createdUsers.add(user);
        final Contentlet contentlet = newContentlet();
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        pushHistoryFor(contentlet.getIdentifier(), environment);

        final Response resp = bundleResource.deleteEnvironmentPushHistory(
                mockRequestFor(user), response, "environmentid/" + environment.getId());

        assertEquals(200, resp.getStatus());
        assertTrue(APILocator.getPushedAssetsAPI().getPushedAssets(contentlet.getIdentifier()).isEmpty());
    }

    private static Contentlet newContentlet() {
        final Host site = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().host(site).nextPersisted();
        return new ContentletDataGen(contentType.id()).host(site).nextPersisted();
    }

    private static void pushHistoryFor(final String assetId, final Environment environment) {
        new PushedAssetDataGen()
                .assetId(assetId)
                .assetType(PusheableAsset.CONTENTLET.toString())
                .bundle(new BundleDataGen().nextPersisted())
                .publishingEndPoint(new PushPublishingEndPointDataGen().environment(environment).nextPersisted())
                .environment(environment)
                .pushDate(new Date())
                .publisher(PushPublisher.class)
                .nextPersisted();
    }
}
