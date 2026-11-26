package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.test.util.ApiTokenTestUtils;
import org.junit.ClassRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContentTypesPaginator;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import io.vavr.control.Try;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.util.Config;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(DataProviderRunner.class)
public class ContentTypeResourceTest {

    @ClassRule
    public static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

	private static final String HOST_TO_REPLACE = "{REPLACE_ME_WITH_HOST_ID}";
	private static final ObjectMapper mapper = new ObjectMapper();

	private static Host site;

	private static final String RENDERED_FIELD_VAR = "renderedField";
	private static ContentType renderedFieldType;
	private static User frontEndUser;
	private static User backEndUser;

	@BeforeClass
	public static void prepare() throws Exception{
		IntegrationTestInitService.getInstance().init();

		//Prepare test data
		site = new SiteDataGen().nextPersisted();
		prepareRenderedFieldType();
	}

	/**
	 * Creates a Content Type with one Custom Field rendered as a component, readable by the
	 * anonymous, front-end and back-end roles, plus one user holding only the front-end role and
	 * one user holding only the back-end role.
	 */
	private static void prepareRenderedFieldType() throws Exception {
		final String variable = "renderedFieldType" + System.currentTimeMillis();
		renderedFieldType = new ContentTypeDataGen()
				.host(site)
				.name(variable)
				.velocityVarName(variable)
				.field(new FieldDataGen()
						.name(RENDERED_FIELD_VAR)
						.velocityVarName(RENDERED_FIELD_VAR)
						.type(CustomField.class)
						.values("#set($a = 40)#set($b = 2)$math.add($a, $b)")
						.next())
				.nextPersisted();
		final Field field = renderedFieldType.fieldMap().get(RENDERED_FIELD_VAR);
		new FieldVariableDataGen().field(field).key("newRenderMode").value("component").nextPersisted();

		final RoleAPI roleAPI = APILocator.getRoleAPI();
		for (final Role role : List.of(roleAPI.loadCMSAnonymousRole(), roleAPI.loadFrontEndUserRole(),
				roleAPI.loadBackEndUserRole())) {
			APILocator.getPermissionAPI().save(new Permission(renderedFieldType.getPermissionId(),
					role.getId(), PermissionAPI.PERMISSION_READ, true), renderedFieldType,
					APILocator.systemUser(), false);
		}
		frontEndUser = new UserDataGen().roles(roleAPI.loadFrontEndUserRole()).nextPersisted();
		backEndUser = new UserDataGen().roles(roleAPI.loadBackEndUserRole()).nextPersisted();
	}

	/**
	 * Removes the Content Type and the users created for the rendered Custom Field tests.
	 */
	@AfterClass
	public static void cleanUp() {
		if (null != renderedFieldType) {
			ContentTypeDataGen.remove(renderedFieldType, true);
		}
		if (null != frontEndUser) {
			UserDataGen.remove(frontEndUser, true);
		}
		if (null != backEndUser) {
			UserDataGen.remove(backEndUser, true);
		}
	}


	@Test
	public void testMain() throws Exception {
		final ContentTypeResource resource = new ContentTypeResource();

		Response response = null;
		Map<String, Object> fieldMap = null;

		final String jsonContentTypeCreate = JSON_CONTENT_TYPE_CREATE
				.replace(HOST_TO_REPLACE, site.getIdentifier());
		final String jsonContentTypeUpdate = JSON_CONTENT_TYPE_UPDATE
				.replace(HOST_TO_REPLACE, site.getIdentifier());

		ContentTypeForm.ContentTypeFormDeserialize contentTypeFormDeserialize = new ContentTypeForm.ContentTypeFormDeserialize();

		// Test INVALID Content Type Creation
		assertResponse_BAD_REQUEST(
				response = resource.createType(getHttpRequest(),  new EmptyHttpResponse(), contentTypeFormDeserialize.buildForm(jsonContentTypeUpdate.replace("CONTENT_TYPE_ID", "INVALID_CONTENT_TYPE_ID")))
		);

		// Test Content Type Creation
		RestUtilTest.verifySuccessResponse(
				response = resource.createType(getHttpRequest(), new EmptyHttpResponse(),
						contentTypeFormDeserialize.buildForm(jsonContentTypeCreate))
		);

		try {
			assertContentTypeCreate(
					convertMapToContentType(
							fieldMap = ((List<Map<String, Object>>)((ResponseEntityView) response.getEntity()).getEntity()).get(0)
					)
			);

			// Test Content Type Retrieval by ID
			RestUtilTest.verifySuccessResponse(
					response = resource.getType((String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse(), null, null)
			);

			// Test Content Type Retrieval by Var
			RestUtilTest.verifySuccessResponse(
				response = resource.getType((String) fieldMap.get("variable"), getHttpRequest(),  new EmptyHttpResponse(), null, null)
			);

			assertContentTypeCreate(
					convertMapToContentType(
							fieldMap = (Map<String, Object>)((ResponseEntityView) response.getEntity()).getEntity()
					)
			);

			// Test INVALID Content Type Update
			assertResponse_NOT_FOUND(
					response = resource.updateType(
							"INVALID_CONTENT_TYPE_ID",
							contentTypeFormDeserialize.buildForm(jsonContentTypeUpdate
									.replace(
											"CONTENT_TYPE_ID",
											"INVALID_CONTENT_TYPE_ID")
									.replace(
											"TheContentType1",
											"INVALID_CONTENT_TYPE_VARIABLE"
									)
							),
							getHttpRequest(), new EmptyHttpResponse()
					)
			);

			// Test Content Type Update
			RestUtilTest.verifySuccessResponse(
					response = resource.updateType(
							(String) fieldMap.get("id"),
							contentTypeFormDeserialize.buildForm(jsonContentTypeUpdate
									.replace("CONTENT_TYPE_ID", (String) fieldMap.get("id"))),
							getHttpRequest(),  new EmptyHttpResponse()
					)
			);

			assertContentTypeUpdate(
					convertMapToContentType(
							fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
					)
			);

			// Test Content Type Retrieval
			RestUtilTest.verifySuccessResponse(
					response = resource.getType((String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse(), null, null)
			);

			assertContentTypeUpdate(
					convertMapToContentType(
							fieldMap = (Map<String, Object>)((ResponseEntityView) response.getEntity()).getEntity()
					)
			);

		} finally {
			// Test Content Type Deletion
			RestUtilTest.verifySuccessResponse(
					response = resource.deleteType(
							(String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
					)
			);

			assertResponse_NOT_FOUND(
					response = resource.getType(
							(String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse(),null, null
					)
			);
		}
	}

	@Test
	public void Simple_Test_Using_VarName() throws Exception {
		final ContentTypeResource resource = new ContentTypeResource();
		ContentTypeForm.ContentTypeFormDeserialize contentTypeFormDeserialize = new ContentTypeForm.ContentTypeFormDeserialize();
		Response response = null;
		Map<String, Object> fieldMap = null;

		final String jsonContentTypeCreate = JSON_CONTENT_TYPE_CREATE
				.replace(HOST_TO_REPLACE, site.getIdentifier());
		final String jsonContentTypeUpdate = JSON_CONTENT_TYPE_UPDATE
				.replace(HOST_TO_REPLACE, site.getIdentifier());

		// Test Content Type Creation
		RestUtilTest.verifySuccessResponse(
				response = resource.createType(getHttpRequest(),  new EmptyHttpResponse(),  contentTypeFormDeserialize.buildForm(jsonContentTypeCreate))
		);
		try{

		    fieldMap = ((List<Map<String, Object>>)((ResponseEntityView) response.getEntity()).getEntity()).get(0);

            // Test Content Type Retrieval by Var
			RestUtilTest.verifySuccessResponse(
					resource.getType((String) fieldMap.get("variable"), getHttpRequest(),  new EmptyHttpResponse(), null, null)
			);

            // Test Content Type Update
			RestUtilTest.verifySuccessResponse(
					resource.updateType(
							(String) fieldMap.get("variable"),
							contentTypeFormDeserialize.buildForm(jsonContentTypeUpdate
									.replace("CONTENT_TYPE_ID", (String) fieldMap.get("id"))),
							getHttpRequest(),  new EmptyHttpResponse()
					)
			);


		}finally{
            // Test Content Type Deletion
			RestUtilTest.verifySuccessResponse(
					resource.deleteType(
							(String) fieldMap.get("variable"), getHttpRequest(),  new EmptyHttpResponse()
					)
			);

			assertResponse_NOT_FOUND(
					resource.getType(
							(String) fieldMap.get("variable"), getHttpRequest(),  new EmptyHttpResponse(), null, null
					)
			);
		}

	}

	private static String JSON_CONTENT_TYPE_CREATE =
			"[{"+
					"\"clazz\": \"com.dotcms.contenttype.model.type.ImmutablePersonaContentType\", "+
					"\"defaultType\": false,"+

					"\"name\": \"The Content Type 1\","+
					"\"description\": \"THE DESCRIPTION\","+

					"\"host\": \"" + HOST_TO_REPLACE + "\"," +
					"\"owner\": \"dotcms.org.1\","+

					"\"variable\": \"TheContentType1\","+

					"\"fixed\": false,"+
					"\"multilingualable\": false,"+
					"\"system\": false,"+
					"\"folder\": \"SYSTEM_FOLDER\","+

					"\"fields\": [{"+
					"\"clazz\": \"com.dotcms.contenttype.model.field.ImmutableTextField\","+
					"\"dataType\": \"TEXT\","+
					"\"name\": \"The Field 1\","+

					"\"defaultValue\": \"THE DEFAULT VALUE\","+
					"\"regexCheck\": \"THE VALIDATION REGEX\","+
					"\"hint\": \"THE HINT\","+

					"\"required\": \"true\","+
					"\"searchable\": \"true\","+
					"\"indexed\": \"true\","+
					"\"listed\": \"true\","+
					"\"unique\": \"false\","+

					"\"readOnly\": \"false\","+
					"\"fixed\": \"false\","+
					"\"sortOrder\": \"7\""+
					"}]"+
					"}]";

	private static void assertContentTypeCreate(ContentType contentType) {
		assertTrue(contentType instanceof PersonaContentType);
		assertNotNull(contentType.id());

		assertEquals("The Content Type 1", contentType.name());
		assertEquals("THE DESCRIPTION", contentType.description());
		assertEquals(site.getIdentifier(), contentType.host());
		assertEquals("dotcms.org.1", contentType.owner());
		assertEquals("TheContentType1", contentType.variable());

		assertFalse(contentType.fixed());
		assertFalse(contentType.multilingualable());
		assertFalse(contentType.system());
		assertEquals("SYSTEM_FOLDER", contentType.folder());

		assertEquals(9, contentType.fields().size());
		assertTrue(contentType.fieldMap().get("theField1") instanceof TextField);
	}

	private static String JSON_CONTENT_TYPE_UPDATE =
			"{"+
					"\"clazz\": \"com.dotcms.contenttype.model.type.ImmutablePersonaContentType\", "+
					"\"defaultType\": false,"+
					"\"id\": \"CONTENT_TYPE_ID\","+

					"\"name\": \"The Content Type 2\","+
					"\"description\": \"THE DESCRIPTION 2\","+

					"\"host\": \"" + HOST_TO_REPLACE + "\"," +
					"\"owner\": \"dotcms.org.1\","+

					"\"variable\": \"TheContentType1\","+

					"\"fixed\": false,"+
					"\"multilingualable\": false,"+
					"\"system\": false,"+
					"\"folder\": \"SYSTEM_FOLDER\""+
					"}";

	private static void assertContentTypeUpdate(ContentType contentType) {
		assertTrue(contentType instanceof PersonaContentType);
		assertNotNull(contentType.id());

		assertEquals("The Content Type 2", contentType.name());
		assertEquals("THE DESCRIPTION 2", contentType.description());
		assertEquals(site.getIdentifier(), contentType.host());
		assertEquals("dotcms.org.1", contentType.owner());
		assertEquals("TheContentType1", contentType.variable());

		assertFalse(contentType.fixed());
		assertFalse(contentType.multilingualable());
		assertFalse(contentType.system());
		assertEquals("SYSTEM_FOLDER", contentType.folder());

		assertEquals(9, contentType.fields().size());
		assertTrue(contentType.fieldMap().get("theField1") instanceof TextField);
	}

	private static void assertResponse_NOT_FOUND(Response response){
		assertNotNull(response);
		assertEquals(404, response.getStatus());
	}

	private static void assertResponse_BAD_REQUEST(Response response){
		assertNotNull(response);
		assertEquals(400, response.getStatus());
	}


	private static ContentType convertMapToContentType(Map<String, Object> fieldMap) {
		try {
			return new JsonContentTypeTransformer(
					mapper.writeValueAsString(fieldMap)
			).from();
		} catch (IOException e) {
			return null;
		}
	}


	/**
	 * Builds an admin request authenticated with a Bearer API token.
	 *
	 * @return the request
	 */
	static HttpServletRequest getHttpRequest() {
		MockHeaderRequest request = new MockHeaderRequest(
				(
						new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
				).request()
		);

		request.setHeader("Authorization", TOKEN_UTILS.adminBearerHeader());

		return request;
	}

	@Test
	public void getBaseTypes_asAdmin() throws Exception {

		final ContentTypeFactory contentTypeFactory = FactoryLocator.getContentTypeFactory();
		final Set <String> allContentTypeNames = contentTypeFactory.findAll().stream().map(ContentType::name).collect(Collectors.toSet());

		final HttpServletRequest request = mock(HttpServletRequest.class);
		final HttpSession session = mock(HttpSession.class);

		when(session.getAttribute(WebKeys.CTX_PATH)).thenReturn("/"); //prevents a NPE
		when(request.getSession()).thenReturn(session);

		final User adminUser = APILocator.getUserAPI().loadUserById("dotcms.org.1");

		final WebResource webResource = mock(WebResource.class);
		final InitDataObject dataObject = mock(InitDataObject.class);
		when(dataObject.getUser()).thenReturn(adminUser);

		when(webResource
				.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
						nullable(String.class))).thenReturn(dataObject);

		final PaginationUtil paginationUtil = mock(PaginationUtil.class);
		final PermissionAPI permissionAPI = mock(PermissionAPI.class);

		final ContentTypeResource resource = new ContentTypeResource
				(new ContentTypeHelper(), webResource, paginationUtil,
						WorkflowHelper.getInstance(), permissionAPI);
		final Response response = resource.getRecentBaseTypes(request);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		final ResponseEntityView responseEntityView = ResponseEntityView.class
				.cast(response.getEntity());
		final List<BaseContentTypesView> types = List.class.cast(responseEntityView.getEntity());

        assertEquals(BaseContentType.values().length-1,types.size());
	}


	@Test
	public void getBaseTypes_asLimitedUser() throws Exception {

		final HttpServletRequest request = mock(HttpServletRequest.class);
		final HttpSession session = mock(HttpSession.class);

		when(session.getAttribute(WebKeys.CTX_PATH)).thenReturn("/"); //prevents a NPE
		when(request.getSession()).thenReturn(session);

		final User chrisPublisher = TestUserUtils.getChrisPublisherUser();
		final WebResource webResource = mock(WebResource.class);
		final InitDataObject dataObject = mock(InitDataObject.class);
		when(dataObject.getUser()).thenReturn(chrisPublisher);

		when(webResource
				.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
						nullable(String.class))).thenReturn(dataObject);

		final PaginationUtil paginationUtil = mock(PaginationUtil.class);
		final PermissionAPI permissionAPI = mock(PermissionAPI.class);

		final ContentTypeResource resource = new ContentTypeResource
				(new ContentTypeHelper(), webResource, paginationUtil,
						WorkflowHelper.getInstance(), permissionAPI);
		final Response response = resource.getRecentBaseTypes(request);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		final ResponseEntityView responseEntityView = ResponseEntityView.class
				.cast(response.getEntity());
		final List<BaseContentTypesView> types = List.class
				.cast(responseEntityView.getEntity());
		assertEquals(BaseContentType.values().length-1,types.size());

	}

    /**
     * Integration test for the resource "/basetypes" {@link ContentTypeResource#getRecentBaseTypes(HttpServletRequest)}
     * <p>
     * When: A Base Content Type is not found.
     * <p>
     * Should: return throw a 500 error.
     */
    @Test
    public void getBaseTypes_shouldReturn500_whenHelperThrowsException() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        final ContentTypeHelper contentTypeHelper = mock(ContentTypeHelper.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final PermissionAPI permissionAPI = mock(PermissionAPI.class);

        when(session.getAttribute(WebKeys.CTX_PATH)).thenReturn("/"); //prevents a NPE
        when(request.getSession()).thenReturn(session);

        when(dataObject.getUser()).thenReturn(TestUserUtils.getChrisPublisherUser());

        when(webResource.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class), anyBoolean(), nullable(String.class)))
                .thenReturn(dataObject);
        when(contentTypeHelper.getTypes(request)).thenThrow(new IllegalArgumentException("Simulated invalid argument exception"));

        final ContentTypeResource resource = new ContentTypeResource(
                contentTypeHelper,
                webResource,
                paginationUtil,
                WorkflowHelper.getInstance(),
                permissionAPI
        );
        Response response = resource.getRecentBaseTypes(request);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }


	@DataProvider
	public static Object[] dataProviderExcludeTypesCommunity() {
		return new Object[] {
			new TestCase(false, true),
			new TestCase(true, false)
		};
	}

	private static class TestCase {
		boolean isCommunity;
		boolean typesIncluded;

		public TestCase(final boolean isCommunity, final boolean typesIncluded) {
			this.isCommunity = isCommunity;
			this.typesIncluded = typesIncluded;
		}
	}

	@Test
	@UseDataProvider("dataProviderExcludeTypesCommunity")
	public void testGetRecentBaseTypes_whenCommunity_excludeTypes(final TestCase testCase)
		throws DotSecurityException, DotDataException {

		final WebResource webResource = mock(WebResource.class);
		final InitDataObject dataObject = mock(InitDataObject.class);
		final User adminUser = APILocator.getUserAPI().loadUserById("dotcms.org.1");

		when(dataObject.getUser()).thenReturn(adminUser);

		when(webResource
			.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
					nullable(String.class))).thenReturn(dataObject);

		final ContentTypeHelper contentTypeHelper = Mockito.spy(new ContentTypeHelper());

		Mockito.doReturn(!testCase.isCommunity).when(contentTypeHelper).isStandardOrEnterprise();

		final ContentTypeResource resource = new ContentTypeResource
			(contentTypeHelper, webResource, new PaginationUtil(new ContentTypesPaginator()),
				WorkflowHelper.getInstance(), APILocator.getPermissionAPI());

		final HttpServletRequest request = mock(HttpServletRequest.class);
		final HttpSession session = mock(HttpSession.class);

		when(session.getAttribute(WebKeys.CTX_PATH)).thenReturn("/"); //prevents a NPE
		when(request.getSession()).thenReturn(session);

		final Response response = resource.getRecentBaseTypes(request);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		final ResponseEntityView responseEntityView = ResponseEntityView.class
			.cast(response.getEntity());
		final List<BaseContentTypesView> types = List.class.cast(responseEntityView.getEntity());

		assertEquals(testCase.typesIncluded, types.stream().anyMatch(this::isEnterpriseBaseType));
	}

    private boolean isEnterpriseBaseType(final BaseContentTypesView baseContentTypesView) {
        return baseContentTypesView.getName().equals(BaseContentType.FORM.name())
                || baseContentTypesView.getName().equals(BaseContentType.PERSONA.name());
    }

    /**
     * Method to test: {@link ContentTypeResource#getPagesContentTypes}
     * Given Scenario: A page whose container holds both a regular and a system content type
     * ExpectedResult: System content types are excluded from the response; regular ones are included
     */
    @Test
    public void test_getPagesContentTypes_excludesSystemContentTypes() throws Exception {

        final User adminUser = APILocator.getUserAPI().loadUserById("dotcms.org.1");

        final ContentType regularContentType = new ContentTypeDataGen().nextPersisted();
        final ContentType systemContentType = new ContentTypeDataGen().system(true).nextPersisted();

        try {
            final Container container = new ContainerDataGen()
                    .withContentType(regularContentType, "")
                    .withContentType(systemContentType, "")
                    .nextPersisted();
            ContainerDataGen.publish(container);

            final Template template = new TemplateDataGen()
                    .withContainer(container.getIdentifier())
                    .nextPersisted();
            APILocator.getVersionableAPI().setLive(template);

            final Host systemHost = APILocator.getHostAPI()
                    .findSystemHost(adminUser, false);
            final Folder folder = new FolderDataGen().site(systemHost).nextPersisted();
            final HTMLPageAsset page = new HTMLPageDataGen(folder, template).nextPersisted();
            APILocator.getContentletAPI().publish(page, adminUser, false);

            final ContentTypeResource resource = new ContentTypeResource();
            final ResponseEntityPaginatedDataView response = resource.getPagesContentTypes(
                    getHttpRequest(), new EmptyHttpResponse(),
                    page.getIdentifier(), "-1", null, 1, 10, "name", "ASC", null, null);

            @SuppressWarnings("unchecked") final List<Map<String, Object>> contentTypes = (List<Map<String, Object>>) response.getEntity();
            final List<String> variables = contentTypes.stream()
                    .map(ct -> (String) ct.get(ContentTypesPaginator.VARIABLE))
                    .collect(Collectors.toList());

            assertTrue("Regular content type should be included in the palette",
                    variables.contains(regularContentType.variable()));
            assertFalse("System content type should be excluded from the palette",
                    variables.contains(systemContentType.variable()));
        } finally {
            ContentTypeDataGen.remove(regularContentType);
            ContentTypeDataGen.remove(systemContentType);
        }
    }

	private static class TestHashMap<K, V> extends HashMap<K, V> {
		@Override
		public boolean equals(Object o) {
			Map other = (Map) o;

			if (this.size() != ((Map) o).size()) {
				return false;
			}

			for (final Object key : other.keySet()) {
				final Object otherValue = other.get(key);
				final Object value = this.get(key);

				if (otherValue.getClass().isArray() && value.getClass().isArray()) {
					if (!Arrays.deepEquals((Object[]) otherValue, (Object[]) value)) {
						return false;
					}

				} else if (!otherValue.equals(value)) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * Method to test: {@link ContentTypeResource#createType}
	 * Given Scenario: a DOTASSET type is created with its fields in one request, one of them a
	 * text field named {@code width} explicitly, the name of a whole-number property every asset
	 * field offers directly. The QA report on #34540 used a whole number, which is accepted.
	 * ExpectedResult: 400 and nothing is created: the type is saved in one transaction with its
	 * fields, so refusing the field must not leave the type behind without it.
	 */
	@Test
	public void test_createType_withFieldNamedLikeAnAssetProperty_Return400() throws Exception {
		final ContentTypeResource resource = new ContentTypeResource();
		final ContentTypeForm.ContentTypeFormDeserialize deserializer =
				new ContentTypeForm.ContentTypeFormDeserialize();
		final String variable = "PropertyCollisionTest" + System.currentTimeMillis();
		final String json = "{"
				+ "\"clazz\": \"com.dotcms.contenttype.model.type.ImmutableDotAssetContentType\","
				+ "\"name\": \"" + variable + "\", \"variable\": \"" + variable + "\","
				+ "\"host\": \"SYSTEM_HOST\","
				+ "\"fields\": [{"
				+ "\"clazz\": \"com.dotcms.contenttype.model.field.ImmutableTextField\","
				+ "\"name\": \"width\", \"variable\": \"width\", \"dataType\": \"TEXT\"}]}";

		final Response response = resource.createType(getHttpRequest(), new EmptyHttpResponse(),
				deserializer.buildForm(json));

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		final boolean created = Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
				.find(variable)).isSuccess();
		if (created) {
			APILocator.getContentTypeAPI(APILocator.systemUser()).delete(
					APILocator.getContentTypeAPI(APILocator.systemUser()).find(variable));
		}
		assertFalse("a refused field must not leave its content type behind", created);
	}

	/**
	 * Given a user holding only the front-end role, who can read the Content Type,
	 * When the Content Type is requested as is and with its Custom Fields rendered,
	 * Then the plain request succeeds and the rendered one is rejected with a 401.
	 */
	@Test
	public void getTypeWithRenderedCustomFields_frontEndUser_requiresBackEndUser() throws Exception {
		final Response plain = new ContentTypeResource().getType(renderedFieldType.variable(),
				requestAs(frontEndUser), new EmptyHttpResponse(), null, null);
		assertEquals(Response.Status.OK.getStatusCode(), plain.getStatus());

		assertUnauthorized(() -> renderAs(requestAs(frontEndUser)));
	}

	/**
	 * Given anonymous access to the content APIs set to READ,
	 * When the Content Type is requested with its Custom Fields rendered and no user,
	 * Then the request is rejected with a 401.
	 */
	@Test
	public void getTypeWithRenderedCustomFields_anonymousReadEnabled_requiresBackEndUser() throws Exception {
		final String previous = Config.getStringProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS,
				AnonymousAccess.NONE.name());
		Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, AnonymousAccess.READ.name());
		try {
			assertUnauthorized(() -> renderAs(requestAs(null)));
		} finally {
			Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, previous);
		}
	}

	/**
	 * Given a user holding only the back-end role, who can read the Content Type,
	 * When the Content Type is requested with its Custom Fields rendered,
	 * Then the Custom Field comes back with its Velocity code evaluated.
	 */
	@Test
	public void getTypeWithRenderedCustomFields_backEndUser_rendersCustomField() throws Exception {
		final Response response = renderAs(requestAs(backEndUser));

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		final String rendered = renderedValue(response);
		assertNotNull("The Custom Field should come back with a rendered value", rendered);
		assertEquals("42", rendered.trim());
	}

	/**
	 * Builds a request carrying the given user, or no user at all when it is {@code null}.
	 */
	private static HttpServletRequest requestAs(final User user) {
		final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
				new MockHttpRequestIntegrationTest("localhost", "/api/v1/contenttype").request()).request())
				.request();
		if (null != user) {
			request.setAttribute(WebKeys.USER, user);
		}
		return request;
	}

	/**
	 * Requests the test Content Type with its Custom Fields rendered. The request and response are
	 * placed on the thread, as a real request would, and the previous ones are put back afterwards.
	 */
	private static Response renderAs(final HttpServletRequest request) throws DotDataException {
		final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final HttpServletResponse previousResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();
		final HttpServletResponse response = new EmptyHttpResponse();
		HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
		HttpServletResponseThreadLocal.INSTANCE.setResponse(response);
		try {
			return new ContentTypeResource().getTypeWithRenderedCustomFields(renderedFieldType.variable(),
					request, response, null, null, null);
		} finally {
			HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
			HttpServletResponseThreadLocal.INSTANCE.setResponse(previousResponse);
		}
	}

	/**
	 * Returns the {@code rendered} value of the test Custom Field in a Content Type response.
	 */
	@SuppressWarnings("unchecked")
	private static String renderedValue(final Response response) {
		final Map<String, Object> entity =
				(Map<String, Object>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
		return ((List<Map<String, Object>>) entity.get("fields")).stream()
				.filter(fieldMap -> RENDERED_FIELD_VAR.equals(fieldMap.get("variable")))
				.map(fieldMap -> (String) fieldMap.get("rendered"))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Asserts that the call is rejected with a 401.
	 */
	private static void assertUnauthorized(final org.junit.function.ThrowingRunnable call) {
		final SecurityException exception = assertThrows(SecurityException.class, call);
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
	}

	/**
	 * Given a Content Type assigned to a custom workflow scheme,
	 * When the GET response is PUT back unchanged (it carries {@code workflows}, never {@code workflow}),
	 * Then the Content Type keeps its schemes.
	 */
	@Test
	public void updateType_getResponsePutBack_keepsSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Set<String> before = schemeIds(type);
			assertTrue(before.contains(scheme.getId()));

			RestUtilTest.verifySuccessResponse(putType(type.id(), getTypeAsJson(type.id())));

			assertEquals(before, schemeIds(type));
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
		}
	}

	/**
	 * Given a Content Type assigned to a custom workflow scheme,
	 * When it is PUT with neither {@code workflow} nor {@code workflows} in the body,
	 * Then the Content Type keeps its schemes.
	 */
	@Test
	public void updateType_withoutAnyWorkflowKey_keepsSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Set<String> before = schemeIds(type);

			final Map<String, Object> body = getTypeAsJson(type.id());
			body.remove("workflows");
			RestUtilTest.verifySuccessResponse(putType(type.id(), body));

			assertEquals(before, schemeIds(type));
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
		}
	}

	/**
	 * Given a Content Type assigned to the System Workflow and a custom scheme,
	 * When it is PUT without {@code workflow} but with {@code workflows} (the read-only key GET
	 * returns) listing another scheme,
	 * Then the original schemes are kept, since {@code workflows} is ignored on write.
	 */
	@Test
	public void updateType_withPluralWorkflowsObjects_keepsSchemes() throws Exception {
		final WorkflowScheme oldScheme = new WorkflowDataGen().nextPersisted();
		final WorkflowScheme newScheme = new WorkflowDataGen().name("newScheme" + System.nanoTime()).nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(oldScheme.getId()).nextPersisted();
		try {
			final Set<String> originalSchemeIds = schemeIds(type);
			assertTrue(originalSchemeIds.contains(oldScheme.getId()));

			final Map<String, Object> body = getTypeAsJson(type.id());
			body.put("workflows", List.of(Map.of("id", newScheme.getId(), "name", newScheme.getName())));
			RestUtilTest.verifySuccessResponse(putType(type.id(), body));

			assertEquals(originalSchemeIds, schemeIds(type));
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(oldScheme);
			WorkflowDataGen.remove(newScheme);
		}
	}

	/**
	 * Given a Content Type assigned to workflow schemes,
	 * When it is PUT with an explicit empty {@code workflow} array,
	 * Then every scheme is removed.
	 */
	@Test
	public void updateType_withEmptyWorkflow_removesSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Map<String, Object> body = getTypeAsJson(type.id());
			body.put("workflow", List.of());
			RestUtilTest.verifySuccessResponse(putType(type.id(), body));

			assertTrue(schemeIds(type).isEmpty());
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
		}
	}

	/**
	 * Given a Content Type assigned to the System Workflow and a custom scheme,
	 * When it is PUT with {@code workflow} listing only the custom scheme's id,
	 * Then the Content Type is assigned exactly that scheme.
	 */
	@Test
	public void updateType_withWorkflowIds_replacesSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Map<String, Object> body = getTypeAsJson(type.id());
			body.put("workflow", List.of(scheme.getId()));
			RestUtilTest.verifySuccessResponse(putType(type.id(), body));

			assertEquals(Set.of(scheme.getId()), schemeIds(type));
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
		}
	}

	/**
	 * Given a Content Type assigned to the System Workflow and a custom scheme,
	 * When it is PUT with {@code workflow} set to a single scheme id string,
	 * Then the Content Type is assigned exactly that scheme.
	 */
	@Test
	public void updateType_withSingleWorkflowString_replacesSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final WorkflowScheme otherScheme = new WorkflowDataGen().name("otherScheme" + System.nanoTime()).nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Map<String, Object> body = getTypeAsJson(type.id());
			body.put("workflow", otherScheme.getId());
			RestUtilTest.verifySuccessResponse(putType(type.id(), body));

			assertEquals(Set.of(otherScheme.getId()), schemeIds(type));
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
			WorkflowDataGen.remove(otherScheme);
		}
	}

	/**
	 * Given a Content Type assigned to the System Workflow and a custom scheme,
	 * When it is PUT with {@code workflow} set to a comma-separated string of two other scheme
	 * ids, with spaces and an empty part,
	 * Then the Content Type is assigned exactly those two schemes.
	 */
	@Test
	public void updateType_withCommaSeparatedWorkflowString_replacesSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final WorkflowScheme first = new WorkflowDataGen().name("firstScheme" + System.nanoTime()).nextPersisted();
		final WorkflowScheme second = new WorkflowDataGen().name("secondScheme" + System.nanoTime()).nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Map<String, Object> body = getTypeAsJson(type.id());
			body.put("workflow", " " + first.getId() + " , ," + second.getId() + " ");
			RestUtilTest.verifySuccessResponse(putType(type.id(), body));

			assertEquals(Set.of(first.getId(), second.getId()), schemeIds(type));
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
			WorkflowDataGen.remove(first);
			WorkflowDataGen.remove(second);
		}
	}

	/**
	 * Given a Content Type assigned to workflow schemes,
	 * When it is PUT with {@code "workflow": null},
	 * Then every scheme is removed, the same as {@code "workflow": []}.
	 */
	@Test
	public void updateType_withNullWorkflow_clearsSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final ObjectMapper jsonMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
			final ObjectNode body = jsonMapper.valueToTree(getTypeAsJson(type.id()));
			body.putNull("workflow");
			RestUtilTest.verifySuccessResponse(putJson(type.id(), body.toString()));

			assertTrue(schemeIds(type).isEmpty());
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
		}
	}

	/**
	 * Given a Content Type assigned to workflow schemes,
	 * When it is PUT with {@code workflow} set to an object, and then to a number,
	 * Then each request is rejected with a 400 and the schemes are unchanged.
	 */
	@Test
	public void updateType_withObjectOrNumberWorkflow_returnsBadRequest() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Set<String> originalSchemeIds = schemeIds(type);
			for (final Object invalidValue : List.of(Map.of("id", scheme.getId()), 42)) {
				final Map<String, Object> body = getTypeAsJson(type.id());
				body.put("workflow", invalidValue);

				final BadRequestException e = assertThrows(BadRequestException.class,
						() -> putType(type.id(), body));
				assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
				assertEquals(originalSchemeIds, schemeIds(type));
			}
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
		}
	}

	/**
	 * Given a Content Type assigned to the System Workflow and a custom scheme,
	 * When it is PUT with a JSON array body whose single entry has {@code workflow} listing
	 * another scheme,
	 * Then the Content Type is assigned exactly that scheme.
	 */
	@Test
	public void updateType_withArrayBodyAndWorkflow_replacesSchemes() throws Exception {
		final WorkflowScheme scheme = new WorkflowDataGen().nextPersisted();
		final WorkflowScheme otherScheme = new WorkflowDataGen().name("otherScheme" + System.nanoTime()).nextPersisted();
		final ContentType type = new ContentTypeDataGen().workflowId(scheme.getId()).nextPersisted();
		try {
			final Map<String, Object> body = getTypeAsJson(type.id());
			body.put("workflow", List.of(otherScheme.getId()));
			final String json = DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
					.writeValueAsString(List.of(body));
			RestUtilTest.verifySuccessResponse(putJson(type.id(), json));

			assertEquals(Set.of(otherScheme.getId()), schemeIds(type));
		} finally {
			ContentTypeDataGen.remove(type);
			WorkflowDataGen.remove(scheme);
			WorkflowDataGen.remove(otherScheme);
		}
	}

	/**
	 * Reads the Content Type through the resource and returns it as the JSON a client receives,
	 * so a test can PUT it back the way the MCP server and the admin UI do.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getTypeAsJson(final String id) throws Exception {
		final Response response = new ContentTypeResource()
				.getType(id, getHttpRequest(), new EmptyHttpResponse(), null, null);
		final ObjectMapper jsonMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
		return jsonMapper.readValue(jsonMapper.writeValueAsString(
				((ResponseEntityView<?>) response.getEntity()).getEntity()), Map.class);
	}

	/**
	 * Sends the given body to {@link ContentTypeResource#updateType} as the request JSON.
	 */
	private static Response putType(final String id, final Map<String, Object> body) throws Exception {
		return putJson(id, DotObjectMapperProvider.getInstance().getDefaultObjectMapper()
				.writeValueAsString(body));
	}

	/**
	 * Sends the given raw JSON to {@link ContentTypeResource#updateType} as the request body.
	 */
	private static Response putJson(final String id, final String json) throws Exception {
		return new ContentTypeResource().updateType(id,
				new ContentTypeForm.ContentTypeFormDeserialize().buildForm(json),
				getHttpRequest(), new EmptyHttpResponse());
	}

	/**
	 * Returns the ids of the workflow schemes currently assigned to the Content Type.
	 */
	private static Set<String> schemeIds(final ContentType type) throws DotDataException {
		return APILocator.getWorkflowAPI().findSchemesForContentType(type).stream()
				.map(WorkflowScheme::getId)
				.collect(Collectors.toSet());
	}
}
