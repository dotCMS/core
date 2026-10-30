package com.dotcms.contenttype.test;


import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.filter.ContentFieldFilter;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ContentResource;
import com.dotcms.rest.RESTParams;
import com.dotcms.rest.SearchForm;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import java.util.Base64;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.enterprise.context.ApplicationScoped;
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
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.DM_WORKFLOW;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest.createContentTypeAndAssignPermissions;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentResourceTest extends IntegrationTestBase {

    final static String REQUIRED_NUMERIC_FIELD_NAME = "numeric";
    final static String NON_REQUIRED_IMAGE_FIELD_NAME = "image";

    final static String REQUIRED_NUMERIC_FIELD_NAME_VALUE= "0";
    final static String NON_REQUIRED_IMAGE_VALUE= "/path/to/the/image/random.jpg";
    private static final String IDENTIFIER = "identifier";
    private static final String JSON_RESPONSE = "json";
    private static final String XML_RESPONSE = "xml";
    private static final String ADMIN_EMAIL = "admin@dotcms.com";
    private static final String ADMIN_PASSWORD = "admin";

    /**
     * Clear-text password the users created by this test sign in with. Their stored password is
     * a hash, so Basic credentials have to be built from the clear text kept here rather than
     * from {@link User#getPassword()}.
     */
    private static final String TEST_USER_PASSWORD = "testPassword";

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
                        .password(TEST_USER_PASSWORD).roles(newRole, TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();

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
            final String message = (String)((Map)endpointResponse1.getEntity()).get("message");
            assertEquals("Unable to set string value 'This isn't a numeric value' as a Long for the field: numeric", message);


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

    /**
     * Method to test: {@link ContentResource#canLockContent(HttpServletRequest, HttpServletResponse, String)}
     * <p>
     * Given scenario: a published contentlet readable by anonymous visitors, a front-end user
     * and a back-end user, locked by another back-end user.
     * <p>
     * Expected result: anonymous and front-end callers get the lock state without the lock
     * owner; the back-end caller gets the owner's id and the owner's full name as a String.
     */
    @Test
    public void test_canLockContent_lockOwnerOnlyForBackendCallers() throws Exception {
        final String password = "TestPass" + System.currentTimeMillis() + "!";
        User owner = null;
        User backendCaller = null;
        User frontendCaller = null;
        Structure structure = null;
        Contentlet content = null;
        try {
            owner = new UserDataGen().firstName("Lock").lastName("Owner")
                    .roles(TestUserUtils.getBackendRole()).nextPersisted();
            backendCaller = new UserDataGen().password(password)
                    .roles(TestUserUtils.getBackendRole()).nextPersisted();
            frontendCaller = new UserDataGen().password(password)
                    .roles(TestUserUtils.getFrontendRole()).nextPersisted();
            structure = new StructureDataGen().nextPersisted();
            content = ContentletDataGen.publish(new ContentletDataGen(structure.getInode())
                    .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                    .nextPersisted());
            final User systemUser = APILocator.systemUser();
            final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
            permissionAPI.save(new Permission(content.getPermissionId(),
                    APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                    PermissionAPI.PERMISSION_READ, true), content, systemUser, false);
            permissionAPI.save(new Permission(content.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(backendCaller).getId(),
                    PermissionAPI.PERMISSION_READ, true), content, systemUser, false);
            permissionAPI.save(new Permission(content.getPermissionId(),
                    APILocator.getRoleAPI().getUserRole(frontendCaller).getId(),
                    PermissionAPI.PERMISSION_READ, true), content, systemUser, false);
            APILocator.getVersionableAPI().setLocked(content, true, owner);

            final ContentResource contentResource = new ContentResource();
            final String params = "id/" + content.getIdentifier();

            final JSONObject anonymousView = new JSONObject(contentResource.canLockContent(
                    lockStatusRequest(null, null), mock(HttpServletResponse.class), params)
                    .getEntity().toString());
            assertTrue(anonymousView.getBoolean("locked"));
            assertFalse(anonymousView.has("lockedBy"));
            assertFalse(anonymousView.has("lockedByName"));

            final JSONObject frontendView = new JSONObject(contentResource.canLockContent(
                    lockStatusRequest(frontendCaller.getEmailAddress(), password),
                    mock(HttpServletResponse.class), params).getEntity().toString());
            assertTrue(frontendView.getBoolean("locked"));
            assertFalse(frontendView.has("lockedBy"));
            assertFalse(frontendView.has("lockedByName"));

            final JSONObject backendView = new JSONObject(contentResource.canLockContent(
                    lockStatusRequest(backendCaller.getEmailAddress(), password),
                    mock(HttpServletResponse.class), params).getEntity().toString());
            assertTrue(backendView.getBoolean("locked"));
            assertEquals(owner.getUserId(), backendView.getString("lockedBy"));
            assertEquals(owner.getFullName(), backendView.getString("lockedByName"));
        } finally {
            if (content != null) {
                APILocator.getVersionableAPI().setLocked(content, false, APILocator.systemUser());
                ContentletDataGen.destroy(content, true);
            }
            if (structure != null) {
                StructureDataGen.remove(structure);
            }
            for (final User user : new User[]{owner, backendCaller, frontendCaller}) {
                if (user != null) {
                    UserDataGen.remove(user, true);
                }
            }
        }
    }

    /**
     * Builds a request with Basic credentials for the given user, or with no credentials when
     * {@code email} is null.
     */
    private static HttpServletRequest lockStatusRequest(final String email, final String password) {
        final MockHeaderRequest request = new MockHeaderRequest(new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/")
                        .request()).request()).request());
        if (email != null) {
            request.setHeader("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString((email + ":" + password).getBytes()));
        }
        return request;
    }

    /**
     * Sets a Basic {@code Authorization} header for the given credentials.
     *
     * @param request  the request to set the header on
     * @param email    the email address to authenticate with
     * @param password the clear-text password to authenticate with
     */
    private static void setBasicCredentials(final MockHeaderRequest request, final String email,
            final String password) {
        request.setHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((email + ":" + password).getBytes()));
    }

    /**
     * Returns the clear-text password of a user this test can sign in as. {@code user.getPassword()}
     * holds a hash and cannot be replayed as a credential.
     *
     * @param user the user to authenticate as
     * @return the password that user was created with
     */
    private static String clearPasswordOf(final User user) {
        return ADMIN_EMAIL.equals(user.getEmailAddress()) ? ADMIN_PASSWORD : TEST_USER_PASSWORD;
    }

    private HttpServletRequest createHttpRequest(final String jsonPayload, final User user) throws Exception{

        MockHeaderRequest request = new MockHeaderRequest(

                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                ).request()
        );

        if (user == null){
            setBasicCredentials(request, ADMIN_EMAIL, ADMIN_PASSWORD);

        } else if (!user.equals(userAPI.getAnonymousUser())){
            setBasicCredentials(request, user.getEmailAddress(), clearPasswordOf(user));
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


    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: The method is invoked for a widget
     * Expected result: The response should override the URL with the one provided in the contentlet
     */
    @Test
    public void testGetContentShouldReturnUrlOverridden() throws Exception {

        final ContentType type = getWidgetLikeContentTypeUrlField("WidgetType", null, null);


        ContentletDataGen contentletDataGen = new ContentletDataGen(type.id())
                .languageId(1L)
                .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                .setProperty("widgetTitle", "titleContent")
                .setProperty("code", "Widget code")
                .setProperty("url", "somevalue");


        final Contentlet contentlet = contentletDataGen.nextPersisted();

        // Build request and response
        final HttpServletRequest request = createHttpRequest(null, null);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Send request
        final ContentResource contentResource = new ContentResource();


        Response endpointResponse = contentResource.getContent(request, response,
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

        assertEquals("somevalue", contentletFromXML.getElementsByTagName("url").item(0).getTextContent());

    }

    @WrapInTransaction
    public static ContentType getWidgetLikeContentTypeUrlField(final String contentTypeName,
                                                       final Set<String> workflowIds, final Supplier<String> widgetCode) {

        ContentType simpleWidgetContentType = null;
        try {
            try {
                simpleWidgetContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (simpleWidgetContentType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("Code")
                                .velocityVarName("code")
                                .required(true)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("url")
                                .velocityVarName("url")
                                .required(true)
                                .next()
                );
                if(null != widgetCode){ //If the widgetCode is not null, then add the field
                    fields.add(
                            new FieldDataGen()
                                    .name("WidgetCode")
                                    .velocityVarName("widgetCode")
                                    .type(ConstantField.class)
                                    .required(false)
                                    .values(widgetCode.get())
                                    .next()
                    );
                }

                simpleWidgetContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.WIDGET)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return simpleWidgetContentType;
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: The method is invoked for a widget with a code field and a widget code field with the same value
     * Expected result: The response should return the rendered widget code when the render parameter is set to true
     * @throws Exception
     */
    @Test
    public void testRenderWidgetCode() throws Exception {

        //We're going to use this as code and widget code to test the endpoint
        final String code = "#set($a = 10)\n"
                + "#set($b = 5) \n"
                + "#set($sum = $a + $b) \n"
                + "$sum";
        // When rendering the widget code, the result should be the sum of a and b (10 + 5 = 15)

        final ContentType type = getWidgetLikeContentTypeUrlField("CodeWidgetType", null, ()->code);

        ContentletDataGen contentletDataGen = new ContentletDataGen(type.id())
                .languageId(1L)
                .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                .setProperty("widgetTitle", "myWidgetTitle")
                .setProperty("code", code)
                .setProperty("url", "somevalue");

        final Contentlet contentlet = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(contentlet);

        // Build request and response
        final HttpServletRequest request = createHttpRequest(null, null);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Send request
        final ContentResource contentResource = new ContentResource();

        final Map<?, ?> resp = callEndpoint(contentResource, request, response, contentlet, false);
        final String codeValue = resp.get("code").toString();
        final String widgetCodeValue = resp.get("widgetCode").toString();

        final String raw = removeSpecialCharacters(code);

        assertEquals(raw, removeSpecialCharacters(codeValue));
        assertEquals(raw, removeSpecialCharacters(widgetCodeValue));

        final Map<?, ?> resp2 = callEndpoint(contentResource, request, response, contentlet, true);
        final String parsedCode = resp2.get("code").toString();
        final String parsedWidgetCode = resp2.get("widgetCode").toString();
        Assert.assertEquals(15,Integer.parseInt(parsedCode));
        Assert.assertEquals(15,Integer.parseInt(parsedWidgetCode));

    }

    public static String removeSpecialCharacters(String input) {
        return input.replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * @param contentResource The content resource
     * @param request The request
     * @param response The response
     * @param contentlet The contentlet
     * @param render If the contentlet should be rendered
     * @return The map with the contentlet
     * @throws JsonProcessingException
     */
    private static Map<?, ?> callEndpoint(final ContentResource contentResource, final HttpServletRequest request,
            final HttpServletResponse response, final Contentlet contentlet, final boolean render) throws JsonProcessingException {
        Response endpointResponse = contentResource.getContent(request, response,
                "/id/" + contentlet.getIdentifier() + "/render/"+render);

        final String out = endpointResponse.getEntity().toString();
        final Map<?,?> map = new ObjectMapper().readValue(out, Map.class);
        final List<?> contentlets = (List<?>)map.get("contentlets");
        Assert.assertFalse(contentlets.isEmpty());
        return (Map<?, ?>)contentlets.get(0);
    }

    /**
     * Content created by the system user, with one related child, both live and readable by the
     * anonymous, front-end and back-end roles, to exercise the Content API field filter.
     */
    private static final class FilteredContentFixture {
        private final ContentType contentType;
        private final Contentlet parent;
        private final Contentlet child;

        private FilteredContentFixture(final ContentType contentType, final Contentlet parent, final Contentlet child) {
            this.contentType = contentType;
            this.parent = parent;
            this.child = child;
        }
    }

    /**
     * Creates a content type with a "children" relationship to itself, a parent and a child related to it,
     * publishes both and lets the anonymous, front-end and back-end roles read the type and the content.
     *
     * @return the created content
     * @throws Exception if the content cannot be created
     */
    private FilteredContentFixture createFilteredContentFixture() throws Exception {
        final long language = languageAPI.getDefaultLanguage().getId();
        final ContentType contentType = createSampleContentType(false);
        final Field childrenField = createRelationshipField("children", contentType,
                contentType.variable(), RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        final ContentletDataGen dataGen = new ContentletDataGen(contentType.id()).languageId(language);
        final Contentlet child = dataGen.nextPersisted();
        Contentlet parent = dataGen.next();
        parent.setRelated(childrenField.variable(), CollectionsUtils.list(child));
        parent = contentletAPI.checkin(parent, user, false);
        ContentletDataGen.publish(child);
        ContentletDataGen.publish(parent);

        for (final Role role : List.of(roleAPI.loadCMSAnonymousRole(), TestUserUtils.getFrontendRole(),
                TestUserUtils.getBackendRole())) {
            permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                    contentType.getPermissionId(), role.getId(), PermissionAPI.PERMISSION_READ, true),
                    contentType, user, false);
            for (final Contentlet contentlet : List.of(parent, child)) {
                permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        contentlet.getPermissionId(), role.getId(), PermissionAPI.PERMISSION_READ, true),
                        contentlet, user, false);
            }
        }
        return new FilteredContentFixture(contentType, parent, child);
    }

    /**
     * Masks modUser and owner and hides modUserName and ownerUserName. identifier is listed too, to prove
     * it is never filtered.
     */
    private static void configureFieldFilter() {
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, "modUser, owner, identifier");
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, "modUserName,ownerUserName");
    }

    /**
     * Restores the field filter configuration to its default, which filters nothing.
     */
    private static void clearFieldFilter() {
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, "");
        Config.setProperty(ContentFieldFilter.FIELDS_TO_HIDE_PROPERTY, "");
    }

    /**
     * Creates a user with the given role only.
     *
     * @param role the {@link Role} to give the user
     * @return the persisted {@link User}
     */
    private static User createUserWithRole(final Role role) {
        return new UserDataGen().active(true)
                .emailAddress("filter" + System.nanoTime() + "@dotcms.com")
                .skinId(UUIDGenerator.generateUuid())
                .password(TEST_USER_PASSWORD).roles(role).nextPersisted();
    }

    /**
     * Calls GET /api/content/id/{id}/depth/1 in JSON for the given user and returns the parent contentlet.
     *
     * @param user the caller
     * @param fixture the content to fetch
     * @return the parent contentlet as JSON
     * @throws Exception if the call fails
     */
    private JSONObject getParentAsJson(final User user, final FilteredContentFixture fixture) throws Exception {
        final Response endpointResponse = new ContentResource().getContent(createHttpRequest(null, user),
                mock(HttpServletResponse.class), "/id/" + fixture.parent.getIdentifier() + "/depth/1");
        assertEquals(Status.OK.getStatusCode(), endpointResponse.getStatus());
        final JSONArray contentlets = new JSONObject(endpointResponse.getEntity().toString())
                .getJSONArray("contentlets");
        assertEquals(1, contentlets.length());
        return contentlets.getJSONObject(0);
    }

    /**
     * Returns the first related child of a parent returned with depth 1.
     *
     * @param parent the parent contentlet as JSON
     * @return the related child as JSON
     * @throws JSONException if the relationship is missing
     */
    private static JSONObject firstChild(final JSONObject parent) throws JSONException {
        final Object children = parent.get("children");
        return children instanceof JSONArray ? ((JSONArray) children).getJSONObject(0) : (JSONObject) children;
    }

    /**
     * Asserts a contentlet is masked and hidden as configured by {@link #configureFieldFilter()}.
     *
     * @param json the contentlet as JSON
     * @param source the contentlet it represents
     * @throws JSONException if a property cannot be read
     */
    private static void assertFiltered(final JSONObject json, final Contentlet source) throws JSONException {
        assertEquals("identifier must never be filtered", source.getIdentifier(), json.getString("identifier"));
        assertEquals(source.getInode(), json.getString("inode"));
        for (final String masked : List.of("modUser", "owner")) {
            final String value = json.getString(masked);
            assertNotEquals(masked + " must be masked", source.getStringProperty(masked), value);
            assertTrue(masked + " must be masked: " + value, value.contains("*****"));
        }
        assertFalse(json.has("modUserName"));
        assertFalse(json.has("ownerUserName"));
    }

    /**
     * Asserts a contentlet is returned without any filtering.
     *
     * @param json the contentlet as JSON
     * @param source the contentlet it represents
     * @throws JSONException if a property cannot be read
     */
    private static void assertUnfiltered(final JSONObject json, final Contentlet source) throws JSONException {
        assertEquals(source.getModUser(), json.getString("modUser"));
        assertEquals(source.getOwner(), json.getString("owner"));
        assertTrue(json.has("modUserName"));
        assertTrue(json.has("ownerUserName"));
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: CONTENT_API_FIELDS_TO_MASK and CONTENT_API_FIELDS_TO_HIDE are set, and an anonymous
     * caller and a front-end user request a contentlet with its related content (depth 1) in JSON.
     * Expected result: modUser and owner are masked and modUserName and ownerUserName are removed, on the
     * contentlet and on its related content; identifier is returned even though it is listed.
     */
    @Test
    public void test_getContent_fieldFilterConfigured_masksAndHidesForAnonymousAndFrontendUsers() throws Exception {
        final FilteredContentFixture fixture = createFilteredContentFixture();
        try {
            configureFieldFilter();
            for (final User caller : List.of(userAPI.getAnonymousUser(),
                    createUserWithRole(TestUserUtils.getFrontendRole()))) {
                final JSONObject parent = getParentAsJson(caller, fixture);
                assertFiltered(parent, fixture.parent);
                assertFiltered(firstChild(parent), fixture.child);
            }
        } finally {
            clearFieldFilter();
            deleteContentTypes(List.of(fixture.contentType));
        }
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: the field filter is configured and an anonymous caller requests a contentlet with its
     * related content (depth 1) in XML.
     * Expected result: the original modUser value and the hidden properties are not in the response.
     */
    @Test
    public void test_getContent_fieldFilterConfigured_masksAndHidesInXml() throws Exception {
        final FilteredContentFixture fixture = createFilteredContentFixture();
        try {
            configureFieldFilter();
            final Response endpointResponse = new ContentResource().getContent(
                    createHttpRequest(null, userAPI.getAnonymousUser()), mock(HttpServletResponse.class),
                    "/id/" + fixture.parent.getIdentifier() + "/type/xml/depth/1");
            assertEquals(Status.OK.getStatusCode(), endpointResponse.getStatus());

            final String xml = endpointResponse.getEntity().toString();
            assertTrue(xml.contains(fixture.parent.getIdentifier()));
            assertTrue(xml.contains(fixture.child.getIdentifier()));
            assertTrue(xml.contains("*****"));
            assertFalse(xml.contains("<modUser>" + fixture.parent.getModUser() + "</modUser>"));
            assertFalse(xml.contains("<owner>" + fixture.parent.getOwner() + "</owner>"));
            assertFalse(xml.contains("<modUserName>"));
            assertFalse(xml.contains("<ownerUserName>"));
        } finally {
            clearFieldFilter();
            deleteContentTypes(List.of(fixture.contentType));
        }
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: the field filter is configured and an administrator and a back-end user request a
     * contentlet with its related content.
     * Expected result: both get every property unfiltered.
     */
    @Test
    public void test_getContent_fieldFilterConfigured_adminsAndBackendUsersGetUnfilteredContent() throws Exception {
        final FilteredContentFixture fixture = createFilteredContentFixture();
        try {
            configureFieldFilter();
            for (final User caller : List.of(TestUserUtils.getAdminUser(),
                    createUserWithRole(TestUserUtils.getBackendRole()))) {
                final JSONObject parent = getParentAsJson(caller, fixture);
                assertUnfiltered(parent, fixture.parent);
                assertUnfiltered(firstChild(parent), fixture.child);
            }
        } finally {
            clearFieldFilter();
            deleteContentTypes(List.of(fixture.contentType));
        }
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: neither CONTENT_API_FIELDS_TO_MASK nor CONTENT_API_FIELDS_TO_HIDE is set, and an
     * anonymous caller requests a contentlet.
     * Expected result: the response is not filtered.
     */
    @Test
    public void test_getContent_fieldFilterNotConfigured_returnsUnfilteredContent() throws Exception {
        final FilteredContentFixture fixture = createFilteredContentFixture();
        try {
            clearFieldFilter();
            final JSONObject parent = getParentAsJson(userAPI.getAnonymousUser(), fixture);
            assertUnfiltered(parent, fixture.parent);
            assertUnfiltered(firstChild(parent), fixture.child);
        } finally {
            deleteContentTypes(List.of(fixture.contentType));
        }
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)} and
     * {@link ContentResource#search(HttpServletRequest, HttpServletResponse, boolean, SearchForm)}
     * Given scenario: the field filter is configured and an anonymous caller queries or sorts by a masked
     * property, which would let them recover its value.
     * Expected result: the anonymous caller gets a 400 with a readable message, never the raw message key, while
     * an administrator can still run the query; an id lookup with the same orderby, which searches nothing, is not
     * refused.
     */
    @Test
    public void test_search_fieldFilterConfigured_rejectsQueriesOnRestrictedFieldsForAnonymousUsers() throws Exception {
        final FilteredContentFixture fixture = createFilteredContentFixture();
        final String query = "+contentType:" + fixture.contentType.variable() + " +modUser:"
                + fixture.parent.getModUser();
        try {
            configureFieldFilter();
            final ContentResource contentResource = new ContentResource();
            final User anonymous = userAPI.getAnonymousUser();

            final Response rejected = contentResource.getContent(createHttpRequest(null, anonymous),
                    mock(HttpServletResponse.class), "/query/" + query);
            assertEquals(Status.BAD_REQUEST.getStatusCode(), rejected.getStatus());
            final String body = String.valueOf(rejected.getEntity());
            assertFalse("the raw message key must not reach the caller: " + body,
                    body.contains(ContentFieldFilter.SEARCH_REJECTED_MESSAGE_KEY));
            assertTrue(body, body.contains("not available to this user"));
            assertEquals(Status.BAD_REQUEST.getStatusCode(), contentResource.getContent(
                    createHttpRequest(null, anonymous), mock(HttpServletResponse.class),
                    "/query/+contentType:" + fixture.contentType.variable() + "/orderby/owner").getStatus());
            assertThrows(BadRequestException.class, () -> contentResource.search(
                    createHttpRequest(null, anonymous), mock(HttpServletResponse.class), false,
                    new SearchForm.Builder().query(query).build()));
            assertEquals("an id lookup searches nothing, so its unused orderby must not be refused",
                    Status.OK.getStatusCode(), contentResource.getContent(createHttpRequest(null, anonymous),
                            mock(HttpServletResponse.class),
                            "/id/" + fixture.parent.getIdentifier() + "/orderby/owner").getStatus());

            assertEquals(Status.OK.getStatusCode(), contentResource.getContent(
                    createHttpRequest(null, TestUserUtils.getAdminUser()), mock(HttpServletResponse.class),
                    "/query/" + query).getStatus());
            assertEquals(Status.OK.getStatusCode(), contentResource.search(
                    createHttpRequest(null, TestUserUtils.getAdminUser()), mock(HttpServletResponse.class), false,
                    new SearchForm.Builder().query(query).build()).getStatus());
        } finally {
            clearFieldFilter();
            deleteContentTypes(List.of(fixture.contentType));
        }
    }

    /**
     * Method to test: {@link ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)}, over the
     * catchall field built when content is indexed
     * Given scenario: the user properties are masked before content is indexed, and a caller searches catchall for
     * the ID of the user who last edited it, exactly and with a trailing wildcard, as an attacker would to rebuild it.
     * Expected result: neither the anonymous caller nor an administrator finds the content that way, while the
     * administrator still finds it by naming the field; content indexed with nothing configured is found through
     * catchall as before.
     */
    @Test
    public void test_search_fieldFilterConfigured_keepsRestrictedValuesOutOfCatchall() throws Exception {
        // A user ID no other indexed value starts with ("system" is not one), and without hyphens, which would keep
        // the wildcard query from matching it at all
        final User author = new UserDataGen().id("catchalluser" + System.nanoTime()).active(true)
                .emailAddress("catchall" + System.nanoTime() + "@dotcms.com")
                .skinId(UUIDGenerator.generateUuid())
                .password(TEST_USER_PASSWORD).roles(roleAPI.loadCMSAdminRole()).nextPersisted();
        final String userId = author.getUserId().toLowerCase();
        final String prefix = userId.substring(0, userId.length() - 1) + "*";

        clearFieldFilter();
        final FilteredContentFixture indexedUnfiltered = createFilteredContentFixture();
        republishAs(indexedUnfiltered.parent, author);
        Config.setProperty(ContentFieldFilter.FIELDS_TO_MASK_PROPERTY, "modUser,owner,publishUser,wfCreatedBy");
        final FilteredContentFixture indexedFiltered = createFilteredContentFixture();
        republishAs(indexedFiltered.parent, author);
        try {

            for (final String term : List.of(userId, prefix)) {
                assertEquals("catchall must not hold " + term, 0, countFound(userAPI.getAnonymousUser(),
                        indexedFiltered, "+catchall:" + term));
                assertEquals("catchall must not hold " + term, 0, countFound(TestUserUtils.getAdminUser(),
                        indexedFiltered, "+catchall:" + term));
                assertEquals("content indexed with nothing configured keeps " + term + " in catchall", 1,
                        countFound(TestUserUtils.getAdminUser(), indexedUnfiltered, "+catchall:" + term));
            }
            assertEquals(1, countFound(TestUserUtils.getAdminUser(), indexedFiltered, "+modUser:" + userId));
        } finally {
            clearFieldFilter();
            deleteContentTypes(List.of(indexedUnfiltered.contentType, indexedFiltered.contentType));
        }
    }

    /**
     * Saves and publishes a new version of a contentlet as the given user, who becomes its modUser and publishUser,
     * and waits for it to be indexed.
     *
     * @param contentlet the contentlet to save again
     * @param author the user saving it
     * @throws Exception if the contentlet cannot be saved or published
     */
    private void republishAs(final Contentlet contentlet, final User author) throws Exception {
        final Contentlet draft = contentletAPI.checkout(contentlet.getInode(), author, false);
        draft.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        draft.setIndexPolicy(IndexPolicy.WAIT_FOR);
        final Contentlet saved = contentletAPI.checkin(draft, author, false);
        saved.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        saved.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentletAPI.publish(saved, author, false);
    }

    /**
     * Counts the contentlets the given user finds with GET /api/content/query, limited to the parent of a fixture.
     *
     * @param caller the user running the query
     * @param fixture the content to look for
     * @param clause the Lucene clause to add to the query
     * @return the number of contentlets found
     * @throws Exception if the call fails
     */
    private int countFound(final User caller, final FilteredContentFixture fixture, final String clause)
            throws Exception {
        final Response endpointResponse = new ContentResource().getContent(createHttpRequest(null, caller),
                mock(HttpServletResponse.class), "/query/+contentType:" + fixture.contentType.variable()
                        + " +identifier:" + fixture.parent.getIdentifier() + " " + clause);
        assertEquals(String.valueOf(endpointResponse.getEntity()), Status.OK.getStatusCode(),
                endpointResponse.getStatus());
        return new JSONObject(endpointResponse.getEntity().toString()).getJSONArray("contentlets").length();
    }
}
