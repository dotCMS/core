package com.dotcms.contenttype.test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.CompanyDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.TestWorkflowUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ContentResource;
import com.dotcms.rest.RESTParams;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.Sets;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.DM_WORKFLOW;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest.createContentTypeAndAssignPermissions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private static PermissionAPI permissionAPI;
    private static RelationshipAPI relationshipAPI;
    private static RoleAPI roleAPI;
    private static User user;
    private static User adminUser;
    private static UserAPI userAPI;
    private static Role adminRole;
    private static Host host;

    static private WorkflowScheme testScheme;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        roleAPI   = APILocator.getRoleAPI();
        user      = APILocator.getUserAPI().getSystemUser();
        userAPI   = APILocator.getUserAPI();
        fieldAPI  = APILocator.getContentTypeFieldAPI();

        contentTypeAPI  = APILocator.getContentTypeAPI(user);
        contentletAPI   = APILocator.getContentletAPI();
        languageAPI     = APILocator.getLanguageAPI();
        permissionAPI   = APILocator.getPermissionAPI();
        relationshipAPI = APILocator.getRelationshipAPI();

        //Creating a workflow for testing

        testScheme = TestWorkflowUtils.getDocumentWorkflow();

        //Creating a test role
        adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        if (adminRole == null) {
            adminRole = new RoleDataGen().key(ADMINISTRATOR).nextPersisted();
        }

        // Test host
        host = new SiteDataGen().nextPersisted();
        adminUser = TestUserUtils.getAdminUser();
    }

    @AfterClass
    public static void cleanUp() {

        if (testScheme != null) {
            WorkflowDataGen.remove(testScheme);
        }
    }

    public static class TestCase {
        String depth;
        String responseType;
        boolean limitedUser;
        boolean testSelfRelated;
        int statusCode;

        public TestCase(final String depth, final String responseType, final int statusCode,
                final boolean limitedUser, final boolean testSelfRelated) {
            this.depth = depth;
            this.responseType = responseType;
            this.statusCode = statusCode;
            this.limitedUser = limitedUser;
            this.testSelfRelated = testSelfRelated;
        }
    }

    public static class PullSelfRelatedTestCase {
        String responseType;

        public PullSelfRelatedTestCase(final String responseType) {
            this.responseType = responseType;
        }
    }

    public static class PullRelatedTestCase {
        boolean pullFromParent;
        boolean multipleMatch;
        boolean addQuery;

        public PullRelatedTestCase(final boolean pullFromParent, final boolean addQuery,
                final boolean multipleMatch) {
            this.pullFromParent = pullFromParent;
            this.addQuery = addQuery;
            this.multipleMatch = multipleMatch;
        }
    }

    @DataProvider
    public static Object[] testCases(){
        return new TestCase[]{
                new TestCase(null, JSON_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("0", JSON_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("1", JSON_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("2", JSON_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("3", JSON_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase(null, XML_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("0", XML_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("1", XML_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("2", XML_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase("3", XML_RESPONSE, Status.OK.getStatusCode(), false, false),
                new TestCase(null, null, Status.OK.getStatusCode(), false, false),
                //Bad depth cases
                new TestCase("5", JSON_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, false),
                new TestCase("5", XML_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, false),
                new TestCase("no_depth", JSON_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, false),
                new TestCase("no_depth", XML_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, false),

                new TestCase("0", JSON_RESPONSE, Status.OK.getStatusCode(), true, false),
                new TestCase("0", XML_RESPONSE, Status.OK.getStatusCode(), true, false),

                new TestCase(null, JSON_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("0", JSON_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("1", JSON_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("2", JSON_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("3", JSON_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase(null, XML_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("0", XML_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("1", XML_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("2", XML_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase("3", XML_RESPONSE, Status.OK.getStatusCode(), false, true),
                new TestCase(null, null, Status.OK.getStatusCode(), false, true),
                //Bad depth cases
                new TestCase("5", JSON_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, true),
                new TestCase("5", XML_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, true),
                new TestCase("no_depth", JSON_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, true),
                new TestCase("no_depth", XML_RESPONSE, Status.BAD_REQUEST.getStatusCode(), false, true),

                new TestCase("0", JSON_RESPONSE, Status.OK.getStatusCode(), true, true),
                new TestCase("0", XML_RESPONSE, Status.OK.getStatusCode(), true, true)
        };
    }

    @DataProvider
    public static Object[] relatedTestCases(){
        return new PullRelatedTestCase[]{
                new PullRelatedTestCase(false, true, true),
                new PullRelatedTestCase(false, false, true),
                new PullRelatedTestCase(true, false, true),
                new PullRelatedTestCase(true, false, false),
                new PullRelatedTestCase(true, true, true)
        };
    }

    @DataProvider
    public static Object[] selfRelatedTestCases(){
        return new PullSelfRelatedTestCase[]{
                new PullSelfRelatedTestCase(JSON_RESPONSE),
                new PullSelfRelatedTestCase(XML_RESPONSE)
        };
    }

    @DataProvider
    public static Object[] categoryTestCases() {
        return new String[] {
                JSON_RESPONSE, XML_RESPONSE
        };
    }

    /**
     * Creates a custom contentType with a required field
     * @return
     * @throws Exception
     */
    private ContentType createSampleContentType(final boolean withFields) throws Exception{

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
     * @param childContentTypeVar
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Field createRelationshipField(final String relationName,
            final ContentType parentContentType, final String childContentTypeVar,
            final int cardinality) throws DotSecurityException, DotDataException {

        final Field newField = FieldBuilder.builder(RelationshipField.class).name(relationName)
                .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                .relationType(childContentTypeVar).build();

        return fieldAPI.save(newField, user);
    }

    @Test
    @UseDataProvider("selfRelatedTestCases")
    public void test_getContentWithMultipleSelfRelated_shouldReturnRelationships(final PullSelfRelatedTestCase testCase)
            throws Exception {

        final long language = languageAPI.getDefaultLanguage().getId();
        final ContentType contentType = createSampleContentType(false);

        //creates relationship fields
        final Field field1 = createRelationshipField("relationship1", contentType,
                contentType.variable(), RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        final Relationship relationship1 = relationshipAPI.getRelationshipFromField(field1, user);

        final Field field2 = createRelationshipField("relationship2", contentType,
                contentType.variable(), RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        final Relationship relationship2 = relationshipAPI.getRelationshipFromField(field2, user);


        //creates contentlets
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        final Contentlet child1 = contentletDataGen.languageId(language).nextPersisted();
        final Contentlet child2 = contentletDataGen.languageId(language).nextPersisted();
        final Contentlet child3 = contentletDataGen.languageId(language).nextPersisted();

        Contentlet parent = contentletDataGen.languageId(language).next();
        parent = contentletAPI.checkin(parent,
                Map.of(relationship1, CollectionsUtils.list(child1), relationship2,
                        CollectionsUtils.list(child2, child3)), user, false);

        final ContentResource contentResource = new ContentResource();
        final HttpServletRequest request = createHttpRequest(null, null);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Response endpointResponse = contentResource.getContent(request, response,
                "/id/" + parent.getIdentifier() + "/live/false/type/" + testCase.responseType
                        + "/depth/0");

        assertEquals(200, endpointResponse.getStatus());

        //validates results
        if (testCase.responseType.equals(JSON_RESPONSE)) {
            final JSONObject json = new JSONObject(endpointResponse.getEntity().toString());
            final JSONArray contentlets = json.getJSONArray("contentlets");
            final JSONObject contentlet = (JSONObject) contentlets.get(0);

            //Validate parent identifier
            assertEquals(parent.getIdentifier(), contentlet.get(IDENTIFIER));

            //Validate child of the first relationship
            JSONArray jsonArray = (JSONArray) contentlet.get(field1.variable());
            assertEquals(1, jsonArray.length());
            assertEquals(child1.getIdentifier(), jsonArray.get(0));

            //Validate children of the second relationship
            jsonArray = (JSONArray) contentlet.get(field2.variable());
            assertEquals(2, jsonArray.length());
            assertEquals(child2.getIdentifier(), jsonArray.get(0));
            assertEquals(child3.getIdentifier(), jsonArray.get(1));

        }else{

            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            final InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(
                    new StringReader(endpointResponse.getEntity().toString().replaceAll("\\n", "")));
            final Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();

            final Element contentlet = (Element) doc.getFirstChild().getFirstChild();

            //Validate parent identifier
            assertEquals(parent.getInode(), contentlet.getElementsByTagName("inode").item(0).getTextContent());

            //Validate child of the first relationship
            NodeList items = (contentlet.getElementsByTagName(field1.variable()).item(0)).getChildNodes();
            int j= 0;
            for (int i=0;i<items.getLength();i++){
                String textContent = items.item(i).getTextContent();
                if (textContent !=null && !textContent.trim().isEmpty()) {
                    assertEquals(child1.getIdentifier(), textContent);
                    j++;
                }
            }

            assertEquals(1, j);

            //Validate children of the second relationship
            items = ((contentlet).getElementsByTagName(field2.variable()).item(0)).getChildNodes();

            j= 0;
            for (int i=0;i<items.getLength();i++){
                String textContent = items.item(i).getTextContent();
                if (textContent !=null && !textContent.trim().isEmpty()) {
                    if (j == 0) {
                        assertEquals(child2.getIdentifier(), textContent);
                    } else{
                        assertEquals(child3.getIdentifier(), textContent);
                    }
                    j++;
                }
            }

            assertEquals(2, j);
        }
    }

    @Test
    @UseDataProvider("testCases")
    public void test_getContent_shouldReturnRelationships(final TestCase testCase)
            throws Exception {

        final long language = languageAPI.getDefaultLanguage().getId();
        final Map<String, Contentlet> contentlets = new HashMap<>();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        ContentType grandChildContentType = null;

        Role newRole = null;
        User createdLimitedUser = null;

        try {

            //creates content types
            parentContentType = createSampleContentType(false);
            childContentType = createSampleContentType(false);
            grandChildContentType = testCase.testSelfRelated? childContentType: createSampleContentType(false);

            //creates relationship fields
            final Field parentField = createRelationshipField("children", parentContentType,
                    childContentType.variable(), RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

            final Field childField = createRelationshipField("grandChildren", childContentType,
                    grandChildContentType.variable(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            //creates the other side of the parent relationship
            createRelationshipField("parent",
                    childContentType,
                    parentContentType.variable() + StringPool.PERIOD + parentField.variable(),
                    RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

            //creates the other side of the child relationship
            final Field otherSideChildField = createRelationshipField(testCase.testSelfRelated? "siblings": "parents",
                    grandChildContentType,
                    childContentType.variable() + StringPool.PERIOD + childField.variable(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

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

            child.setRelated(childField.variable(), CollectionsUtils.list(grandChild1, grandChild2));

            if (testCase.testSelfRelated){
                final Contentlet sibling = childDataGen.languageId(language).nextPersisted();
                child.setRelated(otherSideChildField, CollectionsUtils.list(sibling));
                contentlets.put("sibling", sibling);
            }

            child = contentletAPI.checkin(child, user, false);

            if (testCase.limitedUser){
                newRole = createRole();

                final Company company = new CompanyDataGen()
                        .name("TestCompany")
                        .shortName("TC")
                        .authType("email")
                        .autoLogin(true)
                        .emailAddress("lol@dotCMS.com")
                        .homeURL("localhost")
                        .city("NYC")
                        .mx("MX")
                        .type("test")
                        .phone("5552368")
                        .portalURL("/portalURL")
                        .nextPersisted();

                createdLimitedUser =
                    new UserDataGen().firstName("name").lastName("lastName").companyId(company.getCompanyId())
                        .emailAddress("email" + System.currentTimeMillis() + "@dotcms.com").skinId(UUIDGenerator.generateUuid())
                        .password("password" + System.currentTimeMillis()).roles(newRole, TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();

                //set individual permissions to the child
                permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        child.getPermissionId(), newRole.getId(), PermissionAPI.PERMISSION_READ,
                        true), child, user, false);
            }


            final ContentletDataGen parentDataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parent = parentDataGen.languageId(language).next();
            parent = contentletAPI.checkin(parent,
                    Map.of(parentRelationship, CollectionsUtils.list(child)), user,
                    false);

            if (testCase.limitedUser) {
                //set individual permissions to the child
                permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        parent.getPermissionId(), newRole.getId(), PermissionAPI.PERMISSION_READ,
                        true), parent, user, false);
            }


            contentlets.put("parent", parent);
            contentlets.put("child", child);
            contentlets.put("grandChild1", grandChild1);
            contentlets.put("grandChild2", grandChild2);

            //calls endpoint
            final ContentResource contentResource = new ContentResource();
            final HttpServletRequest request = createHttpRequest(null,
                    testCase.limitedUser ? createdLimitedUser : adminUser);
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

            if (testCase.testSelfRelated){
                deleteContentTypes(CollectionsUtils
                        .list(parentContentType, childContentType));
            } else{
                deleteContentTypes(CollectionsUtils
                        .list(parentContentType, childContentType, grandChildContentType));
            }

            if (newRole != null){
                roleAPI.delete(newRole);
            }
        }

    }

    @Test
    @UseDataProvider("relatedTestCases")
    public void testGetContentWithRelatedParameter(final PullRelatedTestCase testCase) throws Exception {
        final long language = languageAPI.getDefaultLanguage().getId();

        ContentType parentContentType = null;
        ContentType childContentType1  = null;
        ContentType childContentType2  = null;

        try {
            //creates content types
            parentContentType = createSampleContentType(false);
            childContentType1 = createSampleContentType(false);
            childContentType2 = createSampleContentType(false);

            //creates relationship fields
            final Field parentField1 = createRelationshipField("children", parentContentType,
                    childContentType1.variable(), RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            final Field parentField2 = createRelationshipField("anotherChild", parentContentType,
                    childContentType2.variable(), RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

            //creates the other side of the parent relationship
            final Field childField = createRelationshipField("parents",
                    childContentType1,
                    parentContentType.variable() + StringPool.PERIOD + parentField1.variable(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

            //gets relationships
            final Relationship relationship1 = relationshipAPI
                    .getRelationshipFromField(parentField1, user);

            final Relationship relationship2 = relationshipAPI
                    .getRelationshipFromField(parentField2, user);

            //creates contentlets
            final ContentletDataGen childDataGen = new ContentletDataGen(childContentType1.id());
            final Contentlet child1 = childDataGen.languageId(language).nextPersisted();
            final Contentlet child2 = childDataGen.languageId(language).nextPersisted();

            final ContentletDataGen anotherChildDataGen = new ContentletDataGen(childContentType2.id());
            final Contentlet anotherChild = anotherChildDataGen.languageId(language).nextPersisted();

            final ContentletDataGen parentDataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parent1 = parentDataGen.languageId(language).next();
            parent1 = contentletAPI.checkin(parent1,
                    Map.of(relationship1, CollectionsUtils.list(child1, child2),
                            relationship2, CollectionsUtils.list(anotherChild)), user,
                    false);

            Contentlet parent2 = parentDataGen.languageId(language).next();
            parent2 = contentletAPI.checkin(parent2,
                    Map.of(relationship1, CollectionsUtils.list(child1, child2)), user,
                    false);

            final StringBuilder pullRelatedQuery = new StringBuilder();
            //Get related from parent
            if (testCase.pullFromParent) {
                pullRelatedQuery.append("/related/").append(parentContentType.variable())
                        .append(StringPool.PERIOD).append(parentField1.variable());

                if (testCase.multipleMatch) {
                    pullRelatedQuery.append(StringPool.COLON).append(child1.getIdentifier());
                } else{
                    pullRelatedQuery.append(StringPool.COLON).append("invalid_id");
                }

                pullRelatedQuery.append(StringPool.COMMA).append(parentContentType.variable())
                        .append(StringPool.PERIOD).append(parentField2.variable())
                        .append(StringPool.COLON).append(anotherChild.getIdentifier());

            } else{
                pullRelatedQuery.append("/related/").append(childContentType1.variable())
                        .append(StringPool.PERIOD).append(childField.variable())
                        .append(StringPool.COLON).append(parent2.getIdentifier());
            }

            final ContentResource contentResource = new ContentResource();
            final HttpServletRequest request = createHttpRequest(null, null);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final Response endpointResponse = contentResource.getContent(request, response, (testCase.addQuery?
                    "/query/+identifier:" + (testCase.pullFromParent ? parent1.getIdentifier()
                            : child1.getIdentifier()):"") + "/live/false/type/json"
                            + pullRelatedQuery);

            assertEquals(Status.OK.getStatusCode(), endpointResponse.getStatus());

            //validates results
            final JSONObject json = new JSONObject(endpointResponse.getEntity().toString());
            final JSONArray contentlets = json.getJSONArray("contentlets");

            if (!testCase.multipleMatch){
                assertEquals(0,
                        contentlets.length());
            } else {
                assertEquals(!testCase.pullFromParent && !testCase.addQuery ? 2 : 1,
                        contentlets.length());

                final JSONObject contentlet = (JSONObject) contentlets.get(0);
                assertEquals(
                        testCase.pullFromParent ? parent1.getIdentifier()
                                : child1.getIdentifier(),
                        contentlet.get(IDENTIFIER));

                if (!testCase.pullFromParent && !testCase.addQuery) {
                    assertEquals(child2.getIdentifier(),
                            ((JSONObject) contentlets.get(1)).get(IDENTIFIER));
                }
            }

        }finally{
            deleteContentTypes(CollectionsUtils
                    .list(parentContentType, childContentType1, childContentType2));
        }

    }

    /**
     * <b>Method to test:</b> {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * <b><br></br>Given scenario:</b> The method is invoked for any piece of content</br>
     * <b><br></br>Expected result:</b> The response should include the attributes `__icon__` and `contentTypeIcon`
     */
    @Test
    public void testGetContentShouldReturnIconDetails() throws Exception {

        final Contentlet contentlet = TestDataUtils.getDotAssetLikeContentlet();
        
        // Build request and response
        final HttpServletRequest request = createHttpRequest(null, null);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Send request
        final ContentResource contentResource = new ContentResource();
        Response endpointResponse = contentResource.getContent(request, response,
                "/query/+identifier:" + contentlet.getIdentifier() + "/type/json");

        // Verify result
        assertEquals(Status.OK.getStatusCode(), endpointResponse.getStatus());
        
        // Verify JSON result
        final JSONObject json = new JSONObject(endpointResponse.getEntity().toString());
        final JSONArray contentlets = json.getJSONArray("contentlets");
        assertEquals(1, contentlets.length());
        assertNotNull(((JSONObject) contentlets.get(0)).get("__icon__"));
        assertNotNull(((JSONObject) contentlets.get(0)).get("contentTypeIcon"));
        
        endpointResponse = contentResource.getContent(request, response,
                "/query/+identifier:" + contentlet.getIdentifier() + "/type/xml");

        // Verify XML result
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        final InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(
                new StringReader(endpointResponse.getEntity().toString().replaceAll("\\n", "")));
        final Document doc = dBuilder.parse(inputSource);
        doc.getDocumentElement().normalize();

        final Element contentletFromXML = (Element) doc.getFirstChild().getFirstChild();

        assertNotNull(contentletFromXML.getElementsByTagName("__icon__"));
        assertNotNull(contentletFromXML.getElementsByTagName("contentTypeIcon"));
    }

    @Test
    @UseDataProvider("categoryTestCases")
    public void testGetContentWithAllCategoriesInfo(final String testCase) throws Exception {

        Contentlet contentlet = null;
        ContentType contentType = null;
        Category parentCategory = null;

        try {

            // Parent and child categories
            final Category firstChildCategory = createCategory("My Topic", 1).next();
            final Category secondChildCategory = createCategory("Other Topic", 1).next();
            parentCategory = createCategory("Custom Topic", 0)
                    .children(firstChildCategory, secondChildCategory).nextPersisted();

            // Content Type with category field
            final String CATEGORY_NAME = "customTopic";
            final List<Field> fields = new ArrayList<>();
            fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
            fields.add(new FieldDataGen().type(CategoryField.class)
                    .name(CATEGORY_NAME).velocityVarName(CATEGORY_NAME)
                    .values(parentCategory.getInode()).next());
            contentType = new ContentTypeDataGen().host(host).fields(fields).nextPersisted();

            // Save content with categories
            final long language = languageAPI.getDefaultLanguage().getId();
            contentlet = new ContentletDataGen(contentType.id())
                    .languageId(language)
                    .setProperty("title", "Test Contentlet")
                    .addCategory(firstChildCategory).addCategory(secondChildCategory)
                    .nextPersisted();
            assertTrue(APILocator.getContentletAPI().isInodeIndexed(
                    contentlet.getInode(), false, 1000));

            // Build request and response
            final HttpServletRequest request = createHttpRequest(null, null);
            final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

            // Send request
            final ContentResource contentResource = new ContentResource();
            final Response endpointResponse = contentResource.getContent(request, response,
                    "/id/" + contentlet.getIdentifier()
                    + "/languageId/" + language + "/live/false/type/"
                    + (testCase.equals(JSON_RESPONSE) ? "json" : "xml")
                    + "/" + RESTParams.ALL_CATEGORIES_INFO + "/true");

            // Verify result
            assertEquals(Status.OK.getStatusCode(), endpointResponse.getStatus());

            if (testCase.equals(JSON_RESPONSE)) {
                // Verify JSON result
                final JSONObject json = new JSONObject(endpointResponse.getEntity().toString());
                final JSONArray contentlets = json.getJSONArray("contentlets");
                assertEquals(1, contentlets.length());

                final JSONObject resultContentlet = (JSONObject) contentlets.get(0);
                final JSONArray resultCategories = resultContentlet.getJSONArray(CATEGORY_NAME);
                validateCategoryJSON(resultCategories, firstChildCategory, secondChildCategory);
            } else {
                // Verify xml result
                validateCategoryXML(endpointResponse.getEntity().toString(), CATEGORY_NAME,
                        firstChildCategory, secondChildCategory);
            }

        } finally {
            if (UtilMethods.isSet(contentlet) && UtilMethods.isSet(contentlet.getInode())) {
                ContentletDataGen.remove(contentlet);
            }
            if (UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.id())) {
                ContentTypeDataGen.remove(contentType);
            }
            if (UtilMethods.isSet(parentCategory) && UtilMethods.isSet(parentCategory.getInode())){
                APILocator.getCategoryAPI().delete( parentCategory, user, false );
            }
        }

    }

    private Role createRole() throws DotDataException {
        final long millis =  System.currentTimeMillis();

        // Create Role.
        Role newRole = new Role();
        newRole.setName("Role" +  millis);
        newRole.setEditUsers(true);
        newRole.setEditPermissions(true);
        newRole.setSystem(false);
        newRole.setEditLayouts(true);
        newRole.setParent(newRole.getId());
        newRole = roleAPI.save(newRole);

        return newRole;
    }

    private void deleteContentTypes(final List<ContentType> contentTypes) throws DotSecurityException, DotDataException {
        contentTypes.forEach(contentType -> {
            if (contentType != null && contentType.id() != null){
                try {
                    contentTypeAPI.delete(contentType);
                } catch (Exception e) {
                    e.getMessage();
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

        final String childVariable = parent.getContentType().fields().get(0).variable();

        assertEquals(parent.getIdentifier(), contentlet.get(IDENTIFIER));

        if (testCase.depth == null) {
            assertFalse(contentlet.has(childVariable));
        } else {
            final int depth = Integer.parseInt(testCase.depth);

            if (testCase.limitedUser){
                //it shouldn't return any children
                assertFalse(contentlet.has(childVariable));
            }else{
                if (depth == 0){
                    assertEquals(child.getIdentifier(), contentlet.get(childVariable));
                } else{
                    validateChildrenJSON(contentletMap, contentlet, parent, child, depth);
                }
            }

        }
    }

    private void validateChildrenJSON(final Map<String, Contentlet> contentletMap,
            final JSONObject contentlet, final Contentlet parent, final Contentlet child,
            final int depth) throws JSONException {

        final String fieldVariable = parent.getContentType().fields().get(0).variable();

        System.out.println("Field Variable: " + fieldVariable);

        final Object object = contentlet
                .get(fieldVariable);

        System.out.println("This should be a JSON Object:" + object.toString());

        //validates child
        assertEquals(child.getIdentifier(), ((JSONObject) object)
                .get(IDENTIFIER));

        if (depth > 1) {
            //validates grandchildren
            JSONArray jsonArray = (JSONArray) ((JSONObject) contentlet
                    .get(parent.getContentType().fields().get(0).variable()))
                    .get(child.getContentType().fields().get(0).variable());

            assertEquals(2, jsonArray.length());

            assertTrue(CollectionsUtils.list(jsonArray.get(0), jsonArray.get(1)).stream().map(
                    elem -> {
                        try {
                            return depth == 2 ? elem : JSONObject.class.cast(elem).get(IDENTIFIER);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return Optional.empty();
                        }
                    })
                    .allMatch(grandChild -> grandChild
                            .equals(contentletMap.get("grandChild1").getIdentifier())
                            || grandChild
                            .equals(contentletMap.get("grandChild2").getIdentifier())));

            //Validate self-related if needed
            if (contentletMap.containsKey("sibling")){
                jsonArray = (JSONArray) ((JSONObject) contentlet
                        .get(parent.getContentType().fields().get(0).variable()))
                        .get(child.getContentType().fields().get(2).variable());
                assertEquals(1, jsonArray.length());

                assertEquals(contentletMap.get("sibling").getIdentifier(), depth == 2 ? jsonArray.get(0)
                        : JSONObject.class.cast(jsonArray.get(0)).get(IDENTIFIER));

            }

            //parent relationship was not added back again
            assertFalse(((JSONObject) contentlet
                    .get(parent.getContentType().fields().get(0).variable()))
                    .has(child.getContentType().fields().get(1).variable()));
        }
    }

    private void validateCategoryJSON (
            final JSONArray resultCategories, final Category ...categories)
            throws JSONException {

        assertNotNull(resultCategories);
        assertEquals(categories.length, resultCategories.length());
        for (int i = 0; i < resultCategories.length(); i++) {
            final JSONObject resultCategory = resultCategories.getJSONObject(i);
            final String resultCategoryKey = resultCategory.getString("key");
            final Optional<Category> matchCategory = Arrays.stream(categories)
                    .filter(category -> category.getKey().equals(resultCategoryKey))
                    .findFirst();
            assertTrue(matchCategory.isPresent());
            final Category expectedCategory = matchCategory.get();
            assertEquals(expectedCategory.getCategoryName(), resultCategory.get("categoryName"));
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

        final Element contentlet = (Element) doc.getFirstChild().getFirstChild();

        final Contentlet parent = contentletMap.get("parent");
        final Contentlet child = contentletMap.get("child");

        assertEquals(parent.getInode(), contentlet.getElementsByTagName(
                "inode").item(0).getTextContent());

        if (testCase.depth == null) {
            assertEquals(0, contentlet
                    .getElementsByTagName(parent.getContentType().fields().get(0).variable()).getLength());
            return;
        }

        if (testCase.limitedUser){
            //it shouldn't return any children
            assertEquals(0, contentlet
                    .getElementsByTagName(parent.getContentType().fields().get(0).variable()).getLength());
        }else {
            final int depth = Integer.parseInt(testCase.depth);
            if (depth == 0) {
                assertEquals(child.getIdentifier(), contentlet
                        .getElementsByTagName(parent.getContentType().fields().get(0).variable())
                        .item(0).getTextContent());
            } else {
                validateChildrenXML(contentletMap, contentlet, parent, child, depth);
            }
        }
    }

    private void validateChildrenXML(final Map<String, Contentlet> contentletMap,
            final Element contentlet, final Contentlet parent,
            final Contentlet child, final int depth) {

        //Verifies self related case
        if (depth == 3 && contentletMap.containsKey("sibling")){
            assertEquals(contentletMap.get("sibling").getIdentifier(),
                    ((Element) contentlet
                            .getElementsByTagName(
                                    parent.getContentType().fields().get(0).variable())
                            .item(0)).getElementsByTagName(IDENTIFIER).item(0)
                            .getTextContent());

            assertEquals(child.getIdentifier(),
                    ((Element) contentlet
                            .getElementsByTagName(
                                    parent.getContentType().fields().get(0).variable())
                            .item(0)).getElementsByTagName(IDENTIFIER).item(1)
                            .getTextContent());
        } else{
            assertEquals(child.getIdentifier(),
                    ((Element) contentlet
                            .getElementsByTagName(
                                    parent.getContentType().fields().get(0).variable())
                            .item(0)).getElementsByTagName(IDENTIFIER).item(0)
                            .getTextContent());
        }

        if (depth > 1) {
            //validates grandchildren
            final NodeList items = ((Element) (contentlet)
                    .getElementsByTagName(child.getContentType().fields().get(0).variable())
                    .item(0)).getElementsByTagName("item");

            assertEquals(2, items.getLength());

            CollectionsUtils.list(items.item(0), items.item(1)).stream()
                    .map(elem -> depth == 2 ? elem: Element.class.cast(elem)
                            .getElementsByTagName(IDENTIFIER).item(0).getTextContent())
                    .allMatch(grandChild -> grandChild
                            .equals(contentletMap.get("grandChild1").getIdentifier())
                            || grandChild
                            .equals(contentletMap.get("grandChild2").getIdentifier()));

            //parent relationship was not added back again
            assertEquals(0,
                    ((Element) contentlet
                            .getElementsByTagName(
                                    parent.getContentType().fields().get(0).variable())
                            .item(0)).getElementsByTagName(child.getContentType().fields().get(1).variable()).getLength());
        }
    }

    private void validateCategoryXML(
            final String resultXml, final String categoryElement, final Category... categories)
            throws XPathExpressionException {

        final InputSource xmlSource = new InputSource(new StringReader(
                resultXml.replaceAll("\\n", "")));

        final XPath xPath = XPathFactory.newInstance().newXPath();

        final NodeList itemList = (NodeList) xPath.evaluate(
                "//" + categoryElement + "/item",
                xmlSource, XPathConstants.NODESET);

        assertEquals(categories.length, itemList.getLength());
        for (int i = 0; i < itemList.getLength(); i++) {
            final Element resultCategory = (Element) itemList.item(i);
            final String resultCategoryKey = (String) xPath.evaluate("key",
                    resultCategory, XPathConstants.STRING);
            final Optional<Category> matchCategory = Arrays.stream(categories)
                    .filter(category -> category.getKey().equals(resultCategoryKey))
                    .findFirst();
            assertTrue(matchCategory.isPresent());
            final Category expectedCategory = matchCategory.get();
            assertEquals(expectedCategory.getCategoryName(), (String) xPath.evaluate(
                    "categoryName", resultCategory, XPathConstants.STRING));
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
            assertEquals(Status.BAD_REQUEST.getStatusCode(), endpointResponse2.getStatus());

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
            assertEquals(Status.BAD_REQUEST.getStatusCode(), endpointResponse1.getStatus());
            assertEquals("Unable to set string value as a Long for the field: numeric",
                    ((Map)endpointResponse1.getEntity()).get("message"));

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
            assertEquals(Status.BAD_REQUEST.getStatusCode(), endpointResponse1.getStatus());
            /// No Detailed Message is shown here. Explaining that the field is required
        }finally {
            if(null != contentType){
                contentTypeAPI.delete(contentType);
            }
        }

    }

    @DataProvider
    public static Object[] contentResourceInvalidParamsDP(){

        // case 1 bad stInode value
        final String case1Payload = "{\n"
                + "    \"stInode\" : \"InvalidValue\",\n"
                + "    \"numeric\" : \"0\",\n"
                + "    \"image\" : \"whatever\"\n"
                + "}";

        // tuple: payload, expected response
        final Tuple2 case1 = new Tuple2<>(case1Payload, Status.BAD_REQUEST);

        // case 2 missing stInode param
        final String case2Payload = "{\n"
                + "    \"numeric\" : \"0\",\n"
                + "    \"image\" : \"whatever\"\n"
                + "}";

        // tuple: payload, expected response
        final Tuple2 case2 = new Tuple2<>(case2Payload, Status.BAD_REQUEST);

        return new Tuple2[]{
                case1,
                case2
        };
    }

    @Test
    @UseDataProvider("contentResourceInvalidParamsDP")
    public void testSinglePOST_InvalidParamsShouldReturn400(final Tuple2<String, Response.Status> testCase) throws Exception {
        final ContentResource contentResource = new ContentResource();
        ContentType contentType = null;
        try {
            contentType = createSampleContentType(true);

            //Create an instance of the CT
            final HttpServletRequest request1 = createHttpRequest(testCase._1);
            final HttpServletResponse response1 = mock(HttpServletResponse.class);
            final Response endpointResponse1 = contentResource.singlePOST(request1, response1, "/save/1");
            assertEquals(Status.BAD_REQUEST.getStatusCode(), endpointResponse1.getStatus());
        }finally {
            if(null != contentType){
                contentTypeAPI.delete(contentType);
            }
        }

    }

    private HttpServletRequest createHttpRequest(final String jsonPayload, final User user) throws Exception{

        MockHeaderRequest request = new MockHeaderRequest(

                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                ).request()
        );

        if (user == null){
            request.setHeader("Authorization", "Basic " + new String(Base64.encode(("admin@dotcms.com:admin").getBytes())));

        } else if (!user.equals(userAPI.getAnonymousUser())){
            request.setHeader("Authorization", "Basic " + new String(Base64.encode((user.getEmailAddress() + ":" + user.getPassword()).getBytes())));
        }

        if (jsonPayload != null) {
            final MockServletInputStream stream = new MockServletInputStream(new ByteArrayInputStream(jsonPayload.getBytes(StandardCharsets.UTF_8)));

            when(request.getInputStream()).thenReturn(stream);
        }

        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        request.setAttribute(WebKeys.USER,user);

        return request;

    }

    private HttpServletRequest createHttpRequest(final String jsonPayload) throws Exception {
        return createHttpRequest(jsonPayload, null);
    }


    private HttpServletRequest createHttpRequest() throws Exception {
        return createHttpRequest(null, null);
    }

    private User createUser() throws DotSecurityException, DotDataException {

        final long millis =  System.currentTimeMillis();

        // Create Role.
        Role newRole = new Role();
        newRole.setName("Role" +  millis);
        newRole.setEditUsers(true);
        newRole.setEditPermissions(true);
        newRole.setSystem(false);
        newRole.setEditLayouts(true);
        newRole.setParent(newRole.getId());
        newRole = roleAPI.save(newRole);

        final String userName = "user" + millis;
        // Now lets create a user, assign the role and test basic permissions.
        final User newUser = userAPI.createUser(userName + "@test.com", userName +  "@test.com");
        newUser.setFirstName(userName);
        newUser.setLastName(userName);
        newUser.setPassword("mypass");
        userAPI.save(newUser, user, false);

        roleAPI.addRoleToUser(newRole, newUser);

        return newUser;
    }

    private CategoryDataGen createCategory(final String categoryName, final int sortOrder) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String categoryKey = categoryName.toLowerCase().replaceAll("\\s", "")
                + "-" + simpleDateFormat.format(new Date());
        return new CategoryDataGen().setCategoryName(categoryName)
                .setKey(categoryKey).setCategoryVelocityVarName(categoryKey)
                .setSortOrder(sortOrder);
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

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: Create a Category with 3 levels of depth:
     *          Parent Category
     *                  Child Category
     *                          Grand Child Category
     *
     *                  Create a Content Type with a Category field, and a contentlet of it. To
     *                  the contentlet add the Grand Child Category.
     * Expected result: json response must include the grand child category info
     */
    @Test
    public void test_getContent_includeGrandChildCategory_success()
            throws Exception {

        final long currentTime = System.currentTimeMillis();
        //Create Parent Category.
        final Category parentCategory = new CategoryDataGen()
                .setCategoryName("CT-Category-Parent"+currentTime)
                .setKey("parent"+currentTime)
                .setCategoryVelocityVarName("parent"+currentTime)
                .nextPersisted();

        //Create First Child Category.
        final Category childCategoryA = new CategoryDataGen()
                .setCategoryName("CT-Category-A"+currentTime)
                .setKey("categoryA"+currentTime)
                .setCategoryVelocityVarName("categoryA"+currentTime)
                .next();

        //Second Level Category.
        final Category childCategoryA_1 = new CategoryDataGen()
                .setCategoryName("CT-Category-A-1"+currentTime)
                .setKey("categoryA-1"+currentTime)
                .setCategoryVelocityVarName("categoryA-1"+currentTime)
                .next();

        APILocator.getCategoryAPI().save(parentCategory, childCategoryA, user, false);
        APILocator.getCategoryAPI().save(childCategoryA, childCategoryA_1, user, false);

        // Content Type with category field
        final List<Field> fields = new ArrayList<>();
        fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
        fields.add(new FieldDataGen().type(CategoryField.class)
                .name(parentCategory.getCategoryName()).velocityVarName(parentCategory.getCategoryVelocityVarName())
                .values(parentCategory.getInode()).next());
        final ContentType contentType = new ContentTypeDataGen().host(host).fields(fields).nextPersisted();

        // Save content with grand child category
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("title", "Test Contentlet"+currentTime)
                .addCategory(childCategoryA_1)
                .nextPersisted();


        //Call resource
        final Response responseResource = new ContentResource().getContent(createHttpRequest(),new MockHttpResponse(),"/id/"+contentlet.getIdentifier() + "/live/false/type/json");

        // Verify result
        assertEquals(Status.OK.getStatusCode(), responseResource.getStatus());

        // Verify JSON result
        final JSONObject json = new JSONObject(responseResource.getEntity().toString());
        final JSONArray contentlets = json.getJSONArray("contentlets");
        assertEquals(1, contentlets.length());
        assertTrue(contentlets.toString().contains(childCategoryA_1.getCategoryName()));
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: Create a ContentType with a text field and a SelfJoined Relationship.
     *                  Create 3 contentlets and relate as below:
     *          Parent Contentlet
     *                  Child Contentlet
     *                          Grand Child Contentlet
     *
     * Expected result: json response must include the grand child contentlet info, since depth = 3
     */
    @Test
    public void test_getContent_SelfRelatedWith3LevelsOfDepth_shouldReturnRelationships() throws Exception{
        final ContentType contentType = createSampleContentType(false);

        final Field textField = FieldBuilder.builder(TextField.class).name("title")
                .contentTypeId(contentType.id()).build();
        fieldAPI.save(textField, user);

        final Field relationshipField = createRelationshipField("selfRelationship",
                contentType,contentType.variable(),RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());
        final Relationship relationship = relationshipAPI.getRelationshipFromField(relationshipField,user);

        //creates contentlets
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        final Contentlet grandChild = contentletDataGen.languageId(1).setProperty(textField.name(),"grandChild").nextPersisted();
        Contentlet child = contentletDataGen.languageId(1).setProperty(textField.name(),"child").next();
        child = contentletAPI.checkin(child,Map.of(relationship,CollectionsUtils.list(grandChild)),user,false);
        Contentlet parent = contentletDataGen.languageId(1).setProperty(textField.name(),"parent").next();
        parent = contentletAPI.checkin(parent,Map.of(relationship,CollectionsUtils.list(child)),user,false);

        final ContentResource contentResource = new ContentResource();
        final HttpServletRequest request = createHttpRequest(null, null);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Response endpointResponse = contentResource.getContent(request, response,
                "/id/" + parent.getIdentifier() + "/live/false/depth/3");

        assertEquals(200,endpointResponse.getStatus());
        final JSONObject json = new JSONObject(endpointResponse.getEntity().toString());
        final JSONArray contentlets = json.getJSONArray("contentlets");

        final JSONObject parentJSONContentlet = (JSONObject) contentlets.get(0);
        assertEquals(parent.getIdentifier(), parentJSONContentlet.get(IDENTIFIER));
        assertEquals("parent", parentJSONContentlet.get("title"));

        final JSONArray jsonArrayParentRelationships = (JSONArray) parentJSONContentlet.get(relationshipField.variable());
        final JSONObject childJSONContentlet = (JSONObject) jsonArrayParentRelationships.get(0);
        assertEquals(child.getIdentifier(), childJSONContentlet.get(IDENTIFIER));
        assertEquals("child", childJSONContentlet.get("title"));

        final JSONArray jsonArrayChildRelationships = (JSONArray) childJSONContentlet.get(relationshipField.variable());
        final JSONObject grandChildJSONContentlet = (JSONObject) jsonArrayChildRelationships.get(0);
        assertEquals(grandChild.getIdentifier(), grandChildJSONContentlet.get(IDENTIFIER));
        assertEquals("grandChild", grandChildJSONContentlet.get("title"));
    }
}
