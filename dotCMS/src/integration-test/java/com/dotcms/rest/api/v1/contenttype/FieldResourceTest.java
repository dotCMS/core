package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;

import org.junit.BeforeClass;
import org.junit.Test;

public class FieldResourceTest {

	private final ObjectMapper mapper = new ObjectMapper();


	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testExistingFields() throws Exception {
		final FieldResource resource = new FieldResource();

        ContentType contentType = getContentType();

		final Response response = resource.getContentTypeFields(contentType.id(), getHttpRequest());

		assertResponse_OK(response);

		List fields = (List) ((ResponseEntityView) response.getEntity()).getEntity();

		assertFalse(fields.isEmpty());

		for(Object fieldMap : fields){
			Field field = convertMapToField((Map<String, Object>) fieldMap);

			assertNotNull(field);

			assertTrue(field.getClass().getSimpleName().startsWith("Immutable"));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNewFieldTextField() throws Exception {

		final FieldResource resource = new FieldResource();

        final ContentType contentType = getContentType();

		Response response = resource.createContentTypeField(
			contentType.id(), JSON_NEW_FIELD_TEXT.replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest()
		);

		assertResponse_OK(response);


		Map<String, Object> fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();

		assertNotNull(fieldMap);

		try {
			Field field = convertMapToField((Map<String, Object>) fieldMap);

			assertNewFieldText( field );

			response = resource.getContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest());

			assertNewFieldText( field );

		} finally {

			response = resource.deleteContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest());

			assertResponse_OK(response);

			response = resource.getContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest());

			assertResponse_NOT_FOUND(response);
		}
	}

	final String JSON_NEW_FIELD_TEXT = "{"+
		// IDENTITY VALUES
		"	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTextField\","+
		"	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
		"	\"dataType\" : \"TEXT\","+
		"	\"name\" : \"The Field 1\","+

		// MANDATORY VALUES
		"	\"defaultValue\" : \"THE DEFAULT VALUE\","+
		"	\"regexCheck\" : \"THE VALIDATION REGEX\","+
		"	\"hint\" : \"THE HINT\","+

		"	\"required\" : \"true\","+
		"	\"searchable\" : \"true\","+
		"	\"indexed\" : \"true\","+
		"	\"listed\" : \"true\","+
		"	\"unique\" : \"false\","+

		// OPTIONAL VALUES
		"	\"readOnly\" : \"false\","+
		"	\"fixed\" : \"false\","+
		"	\"sortOrder\" : 11"+
	"	}";

	private void assertNewFieldText(Field field) {
		assertTrue(field instanceof TextField);
		assertNotNull(field.id());
		assertEquals("theField1", field.variable());

		assertEquals("THE DEFAULT VALUE", field.defaultValue());
		assertEquals("THE VALIDATION REGEX", field.regexCheck());
		assertEquals("THE HINT", field.hint());

		assertTrue(field.required());
		assertTrue(field.searchable());
		assertTrue(field.indexed());
		assertTrue(field.listed());
		assertFalse(field.unique());

		assertFalse(field.readOnly());
		assertFalse(field.fixed());
		assertEquals(11, field.sortOrder());		
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdatedFieldTextField() throws Exception {

		final FieldResource resource = new FieldResource();

        final ContentType contentType = getContentType();

		Response response = resource.createContentTypeField(
			contentType.id(), JSON_NEW_FIELD_TEXT.replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest()
		);

		assertResponse_OK(response);


		Map<String, Object> fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();

		assertNotNull(fieldMap);

		try {
			response = resource.updateContentTypeFieldById(
				contentType.id(), (String) fieldMap.get("id"),
				JSON_UPDATED_FIELD_TEXT.replace("CONTENT_TYPE_ID", contentType.id()).replace("CONTENT_TYPE_FIELD_ID", (String) fieldMap.get("id")),
				getHttpRequest()
			);

			fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity();

			assertNotNull(fieldMap);

			Field field = convertMapToField((Map<String, Object>) fieldMap);

			assertUpdatedFieldText( field );

		} finally {

			response = resource.deleteContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest());

			assertResponse_OK(response);

			response = resource.getContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest());

			assertResponse_NOT_FOUND(response);
		}
	}

	final String JSON_UPDATED_FIELD_TEXT = "{"+
			// IDENTITY VALUES
		"	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTextField\","+
		"	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
		"	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
		"	\"dataType\" : \"TEXT\","+
		"	\"name\" : \"The Field 1\","+

		// MANDATORY VALUES
		"	\"variable\" : \"theField1\","+
		"	\"sortOrder\":\"12\","+

		"	\"defaultValue\" : \"THE DEFAULT VALUE 2\","+
		"	\"regexCheck\" : \"THE VALIDATION REGEX 2\","+
		"	\"hint\" : \"THE HINT 2\","+

		"	\"required\" : \"false\","+
		"	\"searchable\" : \"false\","+
		"	\"indexed\" : \"false\","+
		"	\"listed\" : \"false\","+
		"	\"unique\" : \"false\""+
	"	}";

	private void assertUpdatedFieldText(Field field) {
		assertTrue(field instanceof TextField);
		assertNotNull(field.id());
		assertEquals("theField1", field.variable());

		assertEquals("THE DEFAULT VALUE 2", field.defaultValue());
		assertEquals("THE VALIDATION REGEX 2", field.regexCheck());
		assertEquals("THE HINT 2", field.hint());

		assertFalse(field.required());
		assertFalse(field.searchable());
		assertFalse(field.indexed());
		assertFalse(field.listed());
		assertFalse(field.unique());

		assertFalse(field.readOnly());
		assertFalse(field.fixed());
		assertEquals(12, field.sortOrder());		
	}


	private void assertResponse_OK(Response response){
		assertNotNull(response);
		assertEquals(response.getStatus(), 200);
		assertNotNull(response.getEntity());
		assertTrue(response.getEntity() instanceof ResponseEntityView);
		assertTrue(
			(ResponseEntityView.class.cast(response.getEntity()).getErrors() == null) ||
			ResponseEntityView.class.cast(response.getEntity()).getErrors().isEmpty()
		);
	}

	private void assertResponse_NOT_FOUND(Response response){
		assertNotNull(response);
		assertEquals(response.getStatus(), 404);
		assertNotNull(response.getEntity());
	}


	private Field convertMapToField(Map<String, Object> fieldMap) {
		try {
			return mapper.readValue(
				mapper.writeValueAsString(fieldMap),
				Field.class
			);
		} catch (IOException e) {
			return null;
		}
	}

	private ContentType getContentType() throws DotDataException {
		User user = APILocator.getUserAPI().getSystemUser();

		return APILocator.getContentTypeAPI(user).search(" velocity_var_name = 'Employee'").get(0);
	}

	private HttpServletRequest getHttpRequest() {
		MockHeaderRequest request = new MockHeaderRequest(
			(
				new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
			).request()
		);

		request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

		return request;
	}
}
