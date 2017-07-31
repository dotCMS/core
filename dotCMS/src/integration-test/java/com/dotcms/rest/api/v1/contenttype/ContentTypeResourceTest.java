package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentTypeResourceTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@BeforeClass
	public static void prepare() throws Exception{
		IntegrationTestInitService.getInstance().init();
	}


	@Test
	public void testMain() throws Exception {
		final ContentTypeResource resource = new ContentTypeResource();

		Response response = null;
		Map<String, Object> fieldMap = null;

		// Test INVALID Content Type Creation
		assertResponse_BAD_REQUEST(
				response = resource.createType(getHttpRequest(), JSON_CONTENT_TYPE_UPDATE.replace("CONTENT_TYPE_ID", "INVALID_CONTENT_TYPE_ID"))
		);

		// Test Content Type Creation
		RestUtilTest.verifySuccessResponse(
				response = resource.createType(getHttpRequest(), JSON_CONTENT_TYPE_CREATE)
		);

		try {
			assertContentTypeCreate(
					convertMapToContentType(
							fieldMap = ((List<Map<String, Object>>)((ResponseEntityView) response.getEntity()).getEntity()).get(0)
					)
			);

			// Test Content Type Retrieval
			RestUtilTest.verifySuccessResponse(
					response = resource.getType((String) fieldMap.get("id"), getHttpRequest())
			);

			assertContentTypeCreate(
					convertMapToContentType(
							fieldMap = (Map<String, Object>)((ResponseEntityView) response.getEntity()).getEntity()
					)
			);

			// Test INVALID Content Type Update
			assertResponse_BAD_REQUEST(
					response = resource.updateType(
							(String) fieldMap.get("id"),
							JSON_CONTENT_TYPE_UPDATE.replace("CONTENT_TYPE_ID", "INVALID_CONTENT_TYPE_ID"),
							getHttpRequest()
					)
			);

			// Test Content Type Update
			RestUtilTest.verifySuccessResponse(
					response = resource.updateType(
							(String) fieldMap.get("id"),
							JSON_CONTENT_TYPE_UPDATE.replace("CONTENT_TYPE_ID", (String) fieldMap.get("id")),
							getHttpRequest()
					)
			);

			assertContentTypeUpdate(
					convertMapToContentType(
							fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
					)
			);

			// Test Content Type Retrieval
			RestUtilTest.verifySuccessResponse(
					response = resource.getType((String) fieldMap.get("id"), getHttpRequest())
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
							(String) fieldMap.get("id"), getHttpRequest()
					)
			);

			assertResponse_NOT_FOUND(
					response = resource.getType(
							(String) fieldMap.get("id"), getHttpRequest()
					)
			);
		}
	}

	@Test
	public void getContentTypes(){
		final HttpServletRequest request  = mock(HttpServletRequest.class);
		final WebResource webResource = mock(WebResource.class);
		final InitDataObject initDataObject = mock(InitDataObject.class);
		final User user = new User();
		when(initDataObject.getUser()).thenReturn(user);
		when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);

		String filter = "filter";
		boolean showArchived = true;
		int page = 3;
		int perPage = 4;
		String orderBy = "name";
		OrderDirection direction = OrderDirection.ASC;

		List<ContentType> contentTypes = new ArrayList<>();
		Response responseExpected = Response.ok(new ResponseEntityView(contentTypes)).build();

		final PaginationUtil paginationUtil = mock(PaginationUtil.class);
		when(paginationUtil.getPage(request, user, filter, page, perPage, orderBy, direction.toString())).thenReturn(responseExpected);


		final ContentTypeResource resource = new ContentTypeResource(new ContentTypeHelper(), webResource, paginationUtil);
		Response response = null;

		RestUtilTest.verifySuccessResponse(
				response = resource.getContentTypes(request, filter, page, perPage, orderBy, direction.toString())
		);

		assertEquals(responseExpected.getEntity(), response.getEntity());
	}

	private static String JSON_CONTENT_TYPE_CREATE =
			"[{"+
					"\"clazz\": \"com.dotcms.contenttype.model.type.ImmutablePersonaContentType\", "+
					"\"defaultType\": false,"+

					"\"name\": \"The Content Type 1\","+
					"\"description\": \"THE DESCRIPTION\","+

					"\"host\": \"48190c8c-42c4-46af-8d1a-0cd5db894797\","+
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
		assertEquals("48190c8c-42c4-46af-8d1a-0cd5db894797", contentType.host());
		assertEquals("dotcms.org.1", contentType.owner());
		assertEquals("TheContentType1", contentType.variable());

		assertFalse(contentType.fixed());
		assertFalse(contentType.multilingualable());
		assertFalse(contentType.system());
		assertEquals("SYSTEM_FOLDER", contentType.folder());

		assertEquals(7, contentType.fields().size());
		assertTrue(contentType.fieldMap().get("theField1") instanceof TextField);
	}

	private static String JSON_CONTENT_TYPE_UPDATE =
			"{"+
					"\"clazz\": \"com.dotcms.contenttype.model.type.ImmutablePersonaContentType\", "+
					"\"defaultType\": false,"+
					"\"id\": \"CONTENT_TYPE_ID\","+

					"\"name\": \"The Content Type 2\","+
					"\"description\": \"THE DESCRIPTION 2\","+

					"\"host\": \"48190c8c-42c4-46af-8d1a-0cd5db894797\","+
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
		assertEquals("48190c8c-42c4-46af-8d1a-0cd5db894797", contentType.host());
		assertEquals("dotcms.org.1", contentType.owner());
		assertEquals("TheContentType1", contentType.variable());

		assertFalse(contentType.fixed());
		assertFalse(contentType.multilingualable());
		assertFalse(contentType.system());
		assertEquals("SYSTEM_FOLDER", contentType.folder());

		assertEquals(7, contentType.fields().size());
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


	private static HttpServletRequest getHttpRequest() {
		MockHeaderRequest request = new MockHeaderRequest(
				(
						new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
				).request()
		);

		request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

		return request;
	}
}