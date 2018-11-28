package com.dotcms.contenttype.test;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.DM_WORKFLOW;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest.createContentTypeAndAssignPermissions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ContentResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.google.common.collect.Sets;
import com.liferay.portal.model.User;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;


public class ContentResourceTest extends IntegrationTestBase {

    final static String REQUIRED_NUMERIC_FIELD_NAME = "numeric";
    final static String NON_REQUIRED_IMAGE_FIELD_NAME = "image";

    final static String REQUIRED_NUMERIC_FIELD_NAME_VALUE= "0";
    final static String NON_REQUIRED_IMAGE_VALUE= "/path/to/the/image/random.jpg";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Creates a custom contentType with a required field
     * @return
     * @throws Exception
     */


    private ContentType createSampleContentType() throws Exception{
        final Role adminRole = APILocator.getRoleAPI().loadRoleByKey(ADMINISTRATOR);
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        ContentType contentType;
        final String ctPrefix = "TestContentType";
        final String newContentTypeName = ctPrefix + System.currentTimeMillis();

        // Create ContentType
        contentType = createContentTypeAndAssignPermissions(newContentTypeName,
                BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final WorkflowScheme documentWorkflow = workflowAPI
                .findSchemeByName(DM_WORKFLOW);

        // Add fields to the contentType
        final Field textFieldNumeric =
                FieldBuilder.builder(TextField.class).name(REQUIRED_NUMERIC_FIELD_NAME).variable(REQUIRED_NUMERIC_FIELD_NAME)
                        .required(true)
                        .contentTypeId(contentType.id()).dataType(DataTypes.INTEGER).build();

        final Field imageField =
                FieldBuilder.builder(ImageField.class).name(NON_REQUIRED_IMAGE_FIELD_NAME).variable(NON_REQUIRED_IMAGE_FIELD_NAME)
                        .required(false)
                        .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build();


        final List<Field> fields = Arrays.asList(textFieldNumeric, imageField);
        contentType = contentTypeAPI.save(contentType, fields);

        // Assign contentType to Workflows
        workflowAPI.saveSchemeIdsForContentType(contentType,
                Stream.of(
                        systemWorkflow.getId(),
                        documentWorkflow.getId()
                ).collect(Collectors.toSet())
        );

        return contentType;
    }


    @Test
    public void Test_Save_Action_Remove_Image_Then_Verify_Fields_Were_Cleared_Issue_15340() throws Exception {
        final ContentResource contentResource = new ContentResource();
        final User sysUser = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(sysUser,false);
        ContentType contentType = null;
        try {
            contentType = createSampleContentType();

            final String payLoadTemplate = "{\n"
               + "    \"stInode\" : \"%s\",\n"
               + "    \"numeric\" : \"%s\",\n"
               + "    \"image\" : \"%s\"\n"
             + "}";

            //Create an instance of the CT
            final String jsonPayload1 = String.format(payLoadTemplate,contentType.inode(),"0","lol");
            final HttpServletRequest request1 = createHttpRequest(jsonPayload1);
            final HttpServletResponse response1 = mock(HttpServletResponse.class);
            final Response endpointResponse1 = contentResource.singlePOST(request1, response1, "/save/1");
            assertEquals(Status.OK.getStatusCode(), endpointResponse1.getStatus());
            assertNotNull(endpointResponse1.getHeaders().get("inode"));
            assertEquals(endpointResponse1.getHeaders().get("inode").size(), 1);

            final Object inode1 = endpointResponse1.getHeaders().get("inode").get(0);
            assertNotNull(endpointResponse1.getHeaders().get("identifier"));
            assertEquals(endpointResponse1.getHeaders().get("identifier").size(), 1);

            //Lets null the image. All fields are required to be send.
            final String payLoadTemplate2 = "{\n"
                    + "    \"stInode\" : \"%s\",\n"
                    + "    \"inode\"   : \"%s\",\n"
                    + "    \"numeric\" : \"%s\",\n"
                    + "    \"image\"   : null\n"
                    + "}";

            //Now update the instance of the CT setting a non required field to null
            final String jsonPayload2 = String.format(payLoadTemplate2,contentType.inode(),inode1,"1");
            final HttpServletRequest request2 = createHttpRequest(jsonPayload2);
            final HttpServletResponse response2 = mock(HttpServletResponse.class);
            final Response endpointResponse2 = contentResource.singlePOST(request2, response2, "/save/1");
            assertEquals(Status.OK.getStatusCode(), endpointResponse2.getStatus());
            assertNotNull(endpointResponse2.getHeaders().get("inode"));
            assertEquals(endpointResponse2.getHeaders().get("inode").size(), 1);

            assertNotNull(endpointResponse2.getHeaders().get("identifier"));
            assertEquals(endpointResponse2.getHeaders().get("identifier").size(), 1);

            final Object inode2 = endpointResponse2.getHeaders().get("inode").get(0);

            assertNotEquals(inode1, inode2);

            final HttpServletRequest request3 = createHttpRequest();
            final HttpServletResponse response3 = mock(HttpServletResponse.class);
            final Response endpointResponse3 = contentResource.getContent(request3, response3,"/inode/" + inode2.toString());
            assertEquals(Status.OK.getStatusCode(), endpointResponse3.getStatus());

            final JSONObject json = new JSONObject(endpointResponse3.getEntity().toString());
            final JSONArray contentlets = json.getJSONArray("contentlets");
            final JSONObject contentlet = (JSONObject)contentlets.get(0);
            assertEquals(1,contentlet.get("numeric"));
            final Set keys = Sets.newHashSet(contentlet.keys());
            //The image is gone from the properties since it was nullified.
            assertFalse(keys.contains("image"));

        }finally {
            if(null != contentType){
                contentTypeAPI.delete(contentType);
            }
        }

    }


    @Test
    public void Test_Save_Action_Send_Fields_SubSet_Issue_15340() throws Exception {
        final ContentResource contentResource = new ContentResource();
        final User sysUser = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(sysUser,false);
        ContentType contentType = null;
        try {
            contentType = createSampleContentType();

            final String payLoadTemplate = "{\n"
                    + "    \"stInode\" : \"%s\",\n"
                    + "    \"numeric\" : \"%s\",\n"
                    + "    \"image\" : \"%s\"\n"
                    + "}";

            //Create an instance of the CT
            final String jsonPayload1 = String.format(payLoadTemplate,contentType.inode(),"0","lol");
            final HttpServletRequest request1 = createHttpRequest(jsonPayload1);
            final HttpServletResponse response1 = mock(HttpServletResponse.class);
            final Response endpointResponse1 = contentResource.singlePOST(request1, response1, "/save/1");
            assertEquals(Status.OK.getStatusCode(), endpointResponse1.getStatus());
            assertNotNull(endpointResponse1.getHeaders().get("inode"));
            assertEquals(endpointResponse1.getHeaders().get("inode").size(), 1);

            final Object inode1 = endpointResponse1.getHeaders().get("inode").get(0);
            assertNotNull(endpointResponse1.getHeaders().get("identifier"));
            assertEquals(endpointResponse1.getHeaders().get("identifier").size(), 1);

            //Lets null the image. But Skip sending numeric field
            final String payLoadTemplate2 = "{\n"
                    + "    \"stInode\" : \"%s\",\n"
                    + "    \"inode\"   : \"%s\",\n"
                    + "    \"image\"   : null\n"
                    + "}";

            //Now update the instance of the CT setting a non required field to null
            final String jsonPayload2 = String.format(payLoadTemplate2, contentType.inode(), inode1);
            final HttpServletRequest request2 = createHttpRequest(jsonPayload2);
            final HttpServletResponse response2 = mock(HttpServletResponse.class);
            final Response endpointResponse2 = contentResource.singlePOST(request2, response2, "/save/1");
            assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), endpointResponse2.getStatus());

            //The Endpoint can only handle the entire set of fields.. You can not use this endpoint to only update 1 field.

        }finally {
            if(null != contentType){
                contentTypeAPI.delete(contentType);
            }
        }

    }

    @Test
    public void Test_Save_Action_Set_Words_To_Required_NumericField_Issue_15340() throws Exception {

        final ContentResource contentResource = new ContentResource();
        final User sysUser = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(sysUser,false);
        ContentType contentType = null;
        try {
            contentType = createSampleContentType();

            final String payLoadTemplate = "{\n"
                    + "    \"stInode\" : \"%s\",\n"
                    + "    \"numeric\" : \"%s\",\n"
                    + "    \"image\" : \"%s\"\n"
                    + "}";

            //Create an instance of the CT
            final String jsonPayload1 = String.format(payLoadTemplate,contentType.inode(),"This isn't a numeric value","imageName");
            final HttpServletRequest request1 = createHttpRequest(jsonPayload1);
            final HttpServletResponse response1 = mock(HttpServletResponse.class);
            final Response endpointResponse1 = contentResource.singlePOST(request1, response1, "/save/1");
            assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), endpointResponse1.getStatus());
            assertEquals("Unable to set string value as a Long", endpointResponse1.getEntity());

        }finally {
            if(null != contentType){
                contentTypeAPI.delete(contentType);
            }
        }
    }

    @Test
    public void Test_Save_Action_Set_Null_To_Required_NumericField_Issue_15340() throws Exception {

        final ContentResource contentResource = new ContentResource();
        final User sysUser = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(sysUser,false);
        ContentType contentType = null;
        try {
            contentType = createSampleContentType();

            final String payLoadTemplate = "{\n"
                    + "    \"stInode\" : \"%s\",\n"
                    + "    \"numeric\" : null,\n"
                    + "    \"image\" : \"%s\"\n"
                    + "}";

            //Create an instance of the CT
            final String jsonPayload1 = String.format(payLoadTemplate,contentType.inode(),"imageName");
            final HttpServletRequest request1 = createHttpRequest(jsonPayload1);
            final HttpServletResponse response1 = mock(HttpServletResponse.class);
            final Response endpointResponse1 = contentResource.singlePOST(request1, response1, "/save/1");
            assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), endpointResponse1.getStatus());
            /// No Detailed Message is shown here. Explaining that the field is required
        }finally {
            if(null != contentType){
                contentTypeAPI.delete(contentType);
            }
        }

    }

    private HttpServletRequest createHttpRequest(final String jsonPayload) throws Exception{
        MockHeaderRequest request = new MockHeaderRequest(

                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON);

        final MockServletInputStream stream = new MockServletInputStream(new ByteArrayInputStream(jsonPayload.getBytes(StandardCharsets.UTF_8)));

        when(request.getInputStream()).thenReturn(stream);

        return request;
    }

    private  HttpServletRequest createHttpRequest() throws Exception{
        MockHeaderRequest request = new MockHeaderRequest(

                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON);

        return request;
    }

    static class MockServletInputStream extends ServletInputStream {

        private final InputStream sourceStream;

        MockServletInputStream(final InputStream sourceStream) {
            this.sourceStream = sourceStream;
        }

        public final InputStream getSourceStream() {
            return this.sourceStream;
        }

        public int read() throws IOException {
            return this.sourceStream.read();
        }

        public void close() throws IOException {
            super.close();
            this.sourceStream.close();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
          //Not implemented
        }
    }

}
