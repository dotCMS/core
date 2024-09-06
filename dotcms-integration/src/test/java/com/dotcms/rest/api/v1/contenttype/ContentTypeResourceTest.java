package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContentTypesPaginator;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.glassfish.jersey.internal.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(DataProviderRunner.class)
public class ContentTypeResourceTest {

	private static final String HOST_TO_REPLACE = "{REPLACE_ME_WITH_HOST_ID}";
	private static final ObjectMapper mapper = new ObjectMapper();

	private static Host site;

	@BeforeClass
	public static void prepare() throws Exception{
		IntegrationTestInitService.getInstance().init();

		//Prepare test data
		site = new SiteDataGen().nextPersisted();
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


	static HttpServletRequest getHttpRequest() {
		MockHeaderRequest request = new MockHeaderRequest(
				(
						new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
				).request()
		);

		request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

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
}