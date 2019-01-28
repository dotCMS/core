package com.dotcms.contenttype.test;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.DM_WORKFLOW;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest.createContentTypeAndAssignPermissions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ContentResource;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.Sets;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xerces.dom.DeferredElementImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@RunWith(DataProviderRunner.class)
public class ContentResourceTest extends IntegrationTestBase {

    final static String REQUIRED_NUMERIC_FIELD_NAME = "numeric";
    final static String NON_REQUIRED_IMAGE_FIELD_NAME = "image";

    final static String REQUIRED_NUMERIC_FIELD_NAME_VALUE= "0";
    final static String NON_REQUIRED_IMAGE_VALUE= "/path/to/the/image/random.jpg";
    private static final String IDENTIFIER = "identifier";
    private static final String JSON_RESPONSE = "json";
    private static final String XML_RESPONSE = "xml";

    private static FieldAPI fieldAPI;
    private static LanguageAPI languageAPI;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static RelationshipAPI relationshipAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user      = APILocator.getUserAPI().getSystemUser();
        fieldAPI  = APILocator.getContentTypeFieldAPI();

        contentTypeAPI  = APILocator.getContentTypeAPI(user);
        contentletAPI   = APILocator.getContentletAPI();
        languageAPI     = APILocator.getLanguageAPI();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    public static class TestCase {
        String depth;
        String responseType;
        int statusCode;

        public TestCase(final String depth, final String responseType, final int statusCode) {
            this.depth        = depth;
            this.responseType = responseType;
            this.statusCode   = statusCode;
        }
    }

    @DataProvider
    public static Object[] testCases(){
        return new TestCase[]{
                new TestCase(null, JSON_RESPONSE, Status.OK.getStatusCode()),
                new TestCase("0", JSON_RESPONSE, Status.OK.getStatusCode()),
                new TestCase("1", JSON_RESPONSE, Status.OK.getStatusCode()),
                new TestCase("2", JSON_RESPONSE, Status.OK.getStatusCode()),
                new TestCase(null, XML_RESPONSE, Status.OK.getStatusCode()),
                new TestCase("0", XML_RESPONSE, Status.OK.getStatusCode()),
                new TestCase("1", XML_RESPONSE, Status.OK.getStatusCode()),
                new TestCase("2", XML_RESPONSE, Status.OK.getStatusCode()),
                new TestCase(null, null, Status.OK.getStatusCode()),
                //Bad depth cases
                new TestCase("5", JSON_RESPONSE, Status.BAD_REQUEST.getStatusCode()),
                new TestCase("5", XML_RESPONSE, Status.BAD_REQUEST.getStatusCode()),
                new TestCase("no_depth", JSON_RESPONSE, Status.BAD_REQUEST.getStatusCode()),
                new TestCase("no_depth", XML_RESPONSE, Status.BAD_REQUEST.getStatusCode()),
        };
    }

    /**
     * Creates a custom contentType with a required field
     * @return
     * @throws Exception
     */
    private ContentType createSampleContentType(final boolean withFields) throws Exception{
        final Role adminRole = APILocator.getRoleAPI().loadRoleByKey(ADMINISTRATOR);
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        ContentType contentType;
        final String ctPrefix = "TestContentType";
        final String newContentTypeName = ctPrefix + System.currentTimeMillis();

        // Create ContentType
        contentType = createContentTypeAndAssignPermissions(newContentTypeName,
                BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final WorkflowScheme documentWorkflow = workflowAPI
                .findSchemeByName(DM_WORKFLOW);

        if (withFields) {

            // Add fields to the contentType
            final Field textFieldNumeric =
                    FieldBuilder.builder(TextField.class).name(REQUIRED_NUMERIC_FIELD_NAME)
                            .variable(REQUIRED_NUMERIC_FIELD_NAME)
                            .required(true)
                            .contentTypeId(contentType.id()).dataType(DataTypes.INTEGER).build();

            final Field imageField =
                    FieldBuilder.builder(ImageField.class).name(NON_REQUIRED_IMAGE_FIELD_NAME)
                            .variable(NON_REQUIRED_IMAGE_FIELD_NAME)
                            .required(false)
                            .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build();

            final List<Field> fields = Arrays.asList(textFieldNumeric, imageField);
            contentType = contentTypeAPI.save(contentType, fields);
        } else{
            contentType = contentTypeAPI.save(contentType);
        }
        // Assign contentType to Workflows
        workflowAPI.saveSchemeIdsForContentType(contentType,
                Stream.of(
                        systemWorkflow.getId(),
                        documentWorkflow.getId()
                ).collect(Collectors.toSet())
        );

        return contentType;
    }

    /**
     * Creates relationship fields
     * @param relationName
     * @param parentContentType
     * @param childContentType
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Field createRelationshipField(final String relationName,
            final ContentType parentContentType, final ContentType childContentType,
            final int cardinality) throws DotSecurityException, DotDataException {

        final Field newField = FieldBuilder.builder(RelationshipField.class).name(relationName)
                .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                .relationType(childContentType.id()).build();

        return fieldAPI.save(newField, user);
    }

    @Test
    @UseDataProvider("testCases")
    public void test_getContent_shouldReturnRelationships(final TestCase testCase)
            throws Exception {

        final long language = languageAPI.getDefaultLanguage().getId();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        ContentType grandChildContentType = null;

        try {

            //creates content types
            parentContentType = createSampleContentType(false);
            childContentType = createSampleContentType(false);
            grandChildContentType = createSampleContentType(false);

            //creates relationship fields
            final Field parentField = createRelationshipField("myChild", parentContentType,
                    childContentType, RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());
            final Field childField = createRelationshipField("myChild", childContentType,
                    grandChildContentType, RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            //gets relationships
            final Relationship childRelationship = relationshipAPI
                    .getRelationshipFromField(childField, user);
            final Relationship parentRelationship = relationshipAPI
                    .getRelationshipFromField(parentField, user);

            //creates contentlets
            final ContentletDataGen grandChildDataGen = new ContentletDataGen(
                    grandChildContentType.id());
            final Contentlet grandChild1 = grandChildDataGen.languageId(language).nextPersisted();
            final Contentlet grandChild2 = grandChildDataGen.languageId(language).nextPersisted();

            final ContentletDataGen childDataGen = new ContentletDataGen(childContentType.id());
            Contentlet child = childDataGen.languageId(language).next();
            child = contentletAPI.checkin(child, CollectionsUtils
                            .map(childRelationship, CollectionsUtils.list(grandChild1, grandChild2)), user,
                    false);

            final ContentletDataGen parentDataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parent = parentDataGen.languageId(language).next();
            parent = contentletAPI.checkin(parent,
                    CollectionsUtils.map(parentRelationship, CollectionsUtils.list(child)), user,
                    false);

            final Map<String, Contentlet> contentlets = new HashMap();
            contentlets.put("parent", parent);
            contentlets.put("child", child);
            contentlets.put("grandChild1", grandChild1);
            contentlets.put("grandChild2", grandChild2);

            //calls endpoint
            final ContentResource contentResource = new ContentResource();
            final HttpServletRequest request = createHttpRequest();
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final Response endpointResponse = contentResource.getContent(request, response,
                    "/id/" + parent.getIdentifier() + "/live/false/type/" + testCase.responseType
                            + "/depth/"
                            + testCase.depth);

            assertEquals(testCase.statusCode, endpointResponse.getStatus());

            if (testCase.statusCode != Status.BAD_REQUEST.getStatusCode()){
                //validates results
                if (testCase.responseType == null || testCase.responseType.equals(JSON_RESPONSE)) {
                    validateJSON(contentlets, testCase, endpointResponse);
                }else{
                    validateXML(contentlets, testCase, endpointResponse);
                }
            }

        }finally{
            deleteContentTypes(CollectionsUtils
                    .list(parentContentType, childContentType, grandChildContentType));
        }

    }

    private void deleteContentTypes(final List<ContentType> contentTypes) throws DotSecurityException, DotDataException {
        contentTypes.forEach(contentType -> {
            if (contentType != null && contentType.id() != null){
                try {
                    contentTypeAPI.delete(contentType);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    /**
     * Validates relationships in a json response
     * @param contentletMap
     * @param testCase
     * @param endpointResponse
     * @throws JSONException
     */
    private void validateJSON(final Map<String, Contentlet> contentletMap, final TestCase testCase,
            final Response endpointResponse) throws JSONException {
        final JSONObject json = new JSONObject(endpointResponse.getEntity().toString());
        final JSONArray contentlets = json.getJSONArray("contentlets");
        final JSONObject contentlet = (JSONObject) contentlets.get(0);

        final Contentlet parent = contentletMap.get("parent");
        final Contentlet child = contentletMap.get("child");

        assertEquals(parent.getIdentifier(), contentlet.get(IDENTIFIER));

        if (testCase.depth == null) {
            assertEquals(child.getIdentifier(),
                    contentlet.get(parent.getContentType().fields().get(0).variable()));
        } else {
            switch (Integer.parseInt(testCase.depth)) {
                case 0:
                    assertEquals(child.getIdentifier(),
                            contentlet.get(parent.getContentType().fields().get(0).variable()));
                    break;

                case 1:
                    assertEquals(child.getIdentifier(), ((JSONObject) contentlet
                            .get(parent.getContentType().fields().get(0).variable()))
                            .get(IDENTIFIER));
                    break;

                case 2:

                    //validates child
                    assertEquals(child.getIdentifier(), ((JSONObject) contentlet
                            .get(parent.getContentType().fields().get(0).variable()))
                            .get(IDENTIFIER));


                    //validates grandchildren
                    final JSONArray jsonArray = (JSONArray)((JSONObject) contentlet
                            .get(parent.getContentType().fields().get(0).variable()))
                            .get(child.getContentType().fields().get(0).variable());

                    assertEquals(2, jsonArray.length());

                    assertTrue(jsonArray.get(0)
                            .equals(contentletMap.get("grandChild1").getIdentifier())
                            || jsonArray.get(0)
                            .equals(contentletMap.get("grandChild2").getIdentifier()));

                    assertTrue(jsonArray.get(1)
                            .equals(contentletMap.get("grandChild1").getIdentifier())
                            || jsonArray.get(1)
                            .equals(contentletMap.get("grandChild2").getIdentifier()));

                    break;
            }
        }
    }

    /**
     * Validates relationships in an xml response
     * @param contentletMap
     * @param testCase
     * @param endpointResponse
     * @throws JSONException
     */
    private void validateXML(final Map<String, Contentlet> contentletMap, final TestCase testCase,
            final Response endpointResponse)
            throws ParserConfigurationException, IOException, SAXException {

        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        final InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(
                new StringReader(endpointResponse.getEntity().toString().replaceAll("\\n", "")));
        final Document doc = dBuilder.parse(inputSource);
        doc.getDocumentElement().normalize();

        final Node contentlet = doc.getFirstChild().getFirstChild();

        final Contentlet parent = contentletMap.get("parent");
        final Contentlet child = contentletMap.get("child");

        assertEquals(parent.getIdentifier(), ((DeferredElementImpl) contentlet).getElementsByTagName(
                IDENTIFIER).item(0).getTextContent());

        if (testCase.depth == null) {
            assertEquals(child.getIdentifier(), ((DeferredElementImpl) contentlet)
                    .getElementsByTagName(parent.getContentType().fields().get(0).variable())
                    .item(0).getTextContent());
            return;
        }

        switch (Integer.parseInt(testCase.depth)){
                case 0:
                    assertEquals(child.getIdentifier(), ((DeferredElementImpl) contentlet)
                            .getElementsByTagName(parent.getContentType().fields().get(0).variable())
                            .item(0).getTextContent());
                    break;

                case 1:
                    assertEquals(child.getIdentifier(),
                            ((DeferredElementImpl) ((DeferredElementImpl) contentlet)
                                    .getElementsByTagName(
                                            parent.getContentType().fields().get(0).variable())
                                    .item(0)).getElementsByTagName(IDENTIFIER).item(0)
                                    .getTextContent());
                    break;

                case 2:
                    //validates child
                    assertEquals(child.getIdentifier(),
                            ((DeferredElementImpl) ((DeferredElementImpl) contentlet)
                                    .getElementsByTagName(
                                            parent.getContentType().fields().get(0).variable())
                                    .item(0)).getElementsByTagName(IDENTIFIER).item(0)
                                    .getTextContent());

                    //validates grandchildren
                    final String[] items = ((DeferredElementImpl) contentlet).getElementsByTagName(
                            parent.getContentType().fields().get(0).variable()).item(1)
                            .getTextContent().split(", ");

                    assertEquals(2, items.length);

                    Arrays.stream(items).allMatch(grandChildIdentifier -> grandChildIdentifier
                            .equals(contentletMap.get("grandChild1").getIdentifier())
                            || grandChildIdentifier
                            .equals(contentletMap.get("grandChild2").getIdentifier()));

                    break;
        }
    }


    @Test
    public void Test_Save_Action_Remove_Image_Then_Verify_Fields_Were_Cleared_Issue_15340() throws Exception {
        final ContentResource contentResource = new ContentResource();
        ContentType contentType = null;
        try {
            contentType = createSampleContentType(true);

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
            assertNotNull(endpointResponse1.getHeaders().get(IDENTIFIER));
            assertEquals(endpointResponse1.getHeaders().get(IDENTIFIER).size(), 1);

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

            assertNotNull(endpointResponse2.getHeaders().get(IDENTIFIER));
            assertEquals(endpointResponse2.getHeaders().get(IDENTIFIER).size(), 1);

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
        ContentType contentType = null;
        try {
            contentType = createSampleContentType(true);

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
            assertNotNull(endpointResponse1.getHeaders().get(IDENTIFIER));
            assertEquals(endpointResponse1.getHeaders().get(IDENTIFIER).size(), 1);

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
        ContentType contentType = null;
        try {
            contentType = createSampleContentType(true);

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
        ContentType contentType = null;
        try {
            contentType = createSampleContentType(true);

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
