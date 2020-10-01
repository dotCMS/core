package com.dotcms.rest;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BundleResourceTest {

    private static BundleResource bundleResource;
    private static User adminUser;
    static HttpServletResponse response;
    static ServletOutputStream servletOutputStream;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        bundleResource = new BundleResource();
        adminUser = APILocator.systemUser();
        response = new MockHttpResponse();
        servletOutputStream = mock(ServletOutputStream.class);
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

        return uuid;
    }

    /**
     * BasicAuth
     */
    private HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }

    /**
     * Method to Test: {@link BundleResource#generateBundle(HttpServletRequest, HttpServletResponse, GenerateBundleForm)}
     * When: Create a bundle and generate the tar.gz file of the given bundle.
     * Should: Generate the bundle without issues, 200.
     */
    @Test
    public void test_generateBundle_success() throws DotDataException, IOException {
        //Create new bundle
        final String bundleId = insertPublishingBundle(adminUser.getUserId(),new Date());

        //Create a Filter since it's needed to generate the bundle
        createFilter();

        //Create GenerateBundleForm
        final GenerateBundleForm bundleForm = new GenerateBundleForm.Builder().bundleId(bundleId).build();

        //Call generate endpoint
        when(response.getOutputStream()).thenReturn(servletOutputStream);
        final Response responseResource = bundleResource.generateBundle(getHttpRequest(),response,bundleForm);

        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
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
     * When: Create a bundle and try to download it, but since the tar.gz has not been generated should fail.
     * Should: return 404 since the file has not been generated
     */
    @Test
    public void test_downloadBundle_success() throws DotDataException, IOException {
        //Create new bundle
        final String bundleId = insertPublishingBundle(adminUser.getUserId(),new Date());

        //Create a Filter since it's needed to generate the bundle
        createFilter();

        //Create GenerateBundleForm
        final GenerateBundleForm bundleForm = new GenerateBundleForm.Builder().bundleId(bundleId).build();

        //Call generate endpoint
        when(response.getOutputStream()).thenReturn(servletOutputStream);
        Response responseResource = bundleResource.generateBundle(getHttpRequest(),response,bundleForm);

        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();

        //Call download endpoint
        responseResource = bundleResource.downloadBundle(getHttpRequest(),response,bundleId);

        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
    }
}
