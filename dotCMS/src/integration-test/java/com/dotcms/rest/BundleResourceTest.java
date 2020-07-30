package com.dotcms.rest;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.toImmutableList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BundleResourceTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
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

        final HttpServletResponse response = mock(HttpServletResponse.class);

        final BodyPart bodyPart = mock(BodyPart.class);
        when(bodyPart.getEntity()).thenReturn(createCOntentFileInputStream);

        final ContentDisposition meta = mock(ContentDisposition.class);
        when(bodyPart.getContentDisposition()).thenReturn(meta);
        when(meta.getFileName()).thenReturn("test_file.tar.gz");

        final FormDataMultiPart multipart = mock(FormDataMultiPart.class);
        when(multipart.getBodyParts()).thenReturn(list(bodyPart));

        final BundleResource bundleResource = new BundleResource();
        bundleResource.uploadBundleSync(request, response, multipart);
    }
}
