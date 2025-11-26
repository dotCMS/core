package com.dotcms.rest;

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
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.control.Try;
import java.util.Base64;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
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

    private static BundleResource bundleResource;
    private static User adminUser;
    static HttpServletResponse response;

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
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));

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
}
