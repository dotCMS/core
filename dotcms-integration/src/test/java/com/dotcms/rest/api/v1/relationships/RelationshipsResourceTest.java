package com.dotcms.rest.api.v1.relationships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class RelationshipsResourceTest {

    private static ContentTypeAPI contentTypeAPI;
    private static RelationshipAPI relationshipAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = APILocator.systemUser();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    @Test
    public void testGetOneSidedRelationships() throws Throwable {

        final RelationshipsResource relationshipsResource = new RelationshipsResource();

        final long time = System.currentTimeMillis();
        final ContentType contentType = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .description("description" + time).folder(FolderAPI.SYSTEM_FOLDER)
                        .host(Host.SYSTEM_HOST)
                        .name("ContentType" + time).owner("owner")
                        .variable("velocityVarName" + time).build());
        try {
            final Relationship relationship = new Relationship();
            relationship.setParentRelationName("Parent");
            relationship.setRelationTypeValue("IT-Parent-Child" + System.currentTimeMillis());
            relationship.setParentStructureInode(contentType.inode());
            relationship.setChildStructureInode(contentType.inode());
            relationshipAPI.create(relationship);

            final Response response = relationshipsResource
                    .getOneSidedRelationships(contentType.id(), -1, 100, getHttpRequest(),  new EmptyHttpResponse());

            //Validate response
            assertEquals(Status.OK.getStatusCode(), response.getStatus());

            final Collection entities = (Collection) ((ResponseEntityView) response.getEntity())
                    .getEntity();

            final List<Map> responseList = CollectionsUtils
                    .asList(entities.iterator());

            assertTrue(UtilMethods.isSet(entities));

            assertEquals(1, responseList.size());
        } finally {
            contentTypeAPI.delete(contentType);
        }
    }

    @Test
    public void testGetOneSidedRelationshipsWithoutContentTypeReturnsBadRequest() throws Throwable {
        final RelationshipsResource relationshipsResource = new RelationshipsResource();

        final Response response = relationshipsResource
                .getOneSidedRelationships(null, -1, 100, getHttpRequest(),  new EmptyHttpResponse());

        //Validate response
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetOneSidedRelationshipsWithInvalidContentTypeReturnsBadRequest()
            throws Throwable {
        final RelationshipsResource relationshipsResource = new RelationshipsResource();

        final Response response = relationshipsResource
                .getOneSidedRelationships(null, -1, 0, getHttpRequest(),  new EmptyHttpResponse());

        //Validate response
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    private static HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                (
                        new MockSessionRequest(new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));

        return request;
    }

}
