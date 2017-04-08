package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
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

	private static final ObjectMapper mapper = new ObjectMapper();


	@BeforeClass
	public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
	}


	@Test
	public void testFieldsList() throws Exception {
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


	@Test
	public void testFieldText() throws Exception {
		testField(
			new AbstractFieldTester(){
				@Override
				protected String getJsonFieldCreate() {
					return "{"+
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
				}

				@Override
				protected void assertFieldCreate(Field field) {
					assertTrue(field instanceof TextField);
					assertEquals(DataTypes.TEXT, field.dataType());
					assertNotNull(field.id());
					assertEquals("The Field 1", field.name());
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

				@Override
				protected String getJsonFieldUpdate() {
					return "{"+
						// IDENTITY VALUES
						"	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableTextField\","+
						"	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
						"	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
						"	\"dataType\" : \"TEXT\","+
						"	\"name\" : \"The Field 2\","+

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
				}

				@Override
				protected void assertFieldUpdate(Field field) {
					assertTrue(field instanceof TextField);
					assertEquals(DataTypes.TEXT, field.dataType());
					assertNotNull(field.id());
					assertEquals("The Field 2", field.name());
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
			}
		);
	}

	@Test
	public void testFieldDate() throws Exception {
		testField(
			new AbstractFieldTester(){
				@Override
				protected String getJsonFieldCreate() {
					return "{"+
						// IDENTITY VALUES
						"	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableDateField\","+
						"	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
						"	\"dataType\" : \"DATE\","+
						"	\"name\" : \"The Field 1\","+

						// MANDATORY VALUES
						"	\"defaultValue\" : \"1995-12-05\","+
						"	\"hint\" : \"THE HINT\","+

						"	\"required\" : \"true\","+
						"	\"searchable\" : \"true\","+
						"	\"indexed\" : \"true\","+
						"	\"listed\" : \"true\","+

						// OPTIONAL VALUES
						"	\"readOnly\" : \"false\","+
						"	\"fixed\" : \"false\","+
						"	\"sortOrder\" : 11"+
					"	}";
				}

				@Override
				protected void assertFieldCreate(Field field) {
					assertTrue(field instanceof DateField);
					assertEquals(DataTypes.DATE, field.dataType());
					assertNotNull(field.id());
					assertEquals("The Field 1", field.name());
					assertEquals("theField1", field.variable());

					assertEquals("1995-12-05", field.defaultValue());
					assertEquals("THE HINT", field.hint());

					assertTrue(field.required());
					assertTrue(field.searchable());
					assertTrue(field.indexed());
					assertTrue(field.listed());

					assertFalse(field.readOnly());
					assertFalse(field.fixed());
					assertEquals(11, field.sortOrder());		
				}

				@Override
				protected String getJsonFieldUpdate() {
					return "{"+
						// IDENTITY VALUES
						"	\"clazz\" : \"com.dotcms.contenttype.model.field.ImmutableDateField\","+
						"	\"contentTypeId\" : \"CONTENT_TYPE_ID\","+
						"	\"id\" : \"CONTENT_TYPE_FIELD_ID\","+
						"	\"dataType\" : \"DATE\","+
						"	\"name\" : \"The Field 2\","+

						// MANDATORY VALUES
						"	\"variable\" : \"theField1\","+
						"	\"sortOrder\":\"12\","+

						"	\"defaultValue\" : \"1980-03-31\","+
						"	\"hint\" : \"THE HINT 2\","+

						"	\"required\" : \"false\","+
						"	\"searchable\" : \"false\","+
						"	\"indexed\" : \"false\","+
						"	\"listed\" : \"false\""+
					"	}";
				}

				@Override
				protected void assertFieldUpdate(Field field) {
					assertTrue(field instanceof DateField);
					assertEquals(DataTypes.DATE, field.dataType());
					assertNotNull(field.id());
					assertEquals("The Field 2", field.name());
					assertEquals("theField1", field.variable());

					assertEquals("1980-03-31", field.defaultValue());
					assertEquals("THE HINT 2", field.hint());

					assertFalse(field.required());
					assertFalse(field.searchable());
					assertFalse(field.indexed());
					assertFalse(field.listed());

					assertFalse(field.readOnly());
					assertFalse(field.fixed());
					assertEquals(12, field.sortOrder());		
				}
			}
		);
	}


	private static abstract class AbstractFieldTester {

		protected abstract String getJsonFieldCreate();
		protected abstract String getJsonFieldUpdate();
		
		protected abstract void assertFieldCreate(Field field);
		protected abstract void assertFieldUpdate(Field field);

		public void run() throws Exception {

			final FieldResource resource = new FieldResource();

	        final ContentType contentType = getContentType();

	        Response response;
	        Map<String, Object> fieldMap;

	        // Test Field Creation
			assertResponse_OK(
				response = resource.createContentTypeField(
					contentType.id(), getJsonFieldCreate().replace("CONTENT_TYPE_ID", contentType.id()), getHttpRequest()
				)
			);

			assertNotNull(
				fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
			);

			try {
				assertFieldCreate(
					convertMapToField((Map<String, Object>) fieldMap)
				);

				// Test Field Retrieval by Id
				assertResponse_OK(
					response = resource.getContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest())
				);

				assertNotNull(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				);

				assertFieldCreate(
					convertMapToField((Map<String, Object>) fieldMap)
				);

				// Test Field Update
				assertResponse_OK(
					response = resource.updateContentTypeFieldById(
						contentType.id(), (String) fieldMap.get("id"),
						getJsonFieldUpdate().replace("CONTENT_TYPE_ID", contentType.id()).replace("CONTENT_TYPE_FIELD_ID", (String) fieldMap.get("id")),
						getHttpRequest()
					)
				);

				assertNotNull(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				);

				assertFieldUpdate(
					convertMapToField((Map<String, Object>) fieldMap)
				);

				// Test Field Retrieval by Var
				assertResponse_OK(
					response = resource.getContentTypeFieldByVar(contentType.id(), (String) fieldMap.get("variable"), getHttpRequest())
				);

				assertNotNull(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				);

				assertFieldUpdate(
					convertMapToField((Map<String, Object>) fieldMap)
				);

			} finally {

				// Test Field Deletion
				assertResponse_OK(
					response = resource.deleteContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest())
				);

				assertResponse_NOT_FOUND(
					response = resource.getContentTypeFieldById(contentType.id(), (String) fieldMap.get("id"), getHttpRequest())
				);
			}
		}
	}

	private void testField(AbstractFieldTester fieldTester) throws Exception {
		fieldTester.run();
	}


	private static void assertResponse_OK(Response response){
		assertNotNull(response);
		assertEquals(response.getStatus(), 200);
		assertNotNull(response.getEntity());
		assertTrue(response.getEntity() instanceof ResponseEntityView);
		assertTrue(
			(ResponseEntityView.class.cast(response.getEntity()).getErrors() == null) ||
			ResponseEntityView.class.cast(response.getEntity()).getErrors().isEmpty()
		);
	}

	private static void assertResponse_NOT_FOUND(Response response){
		assertNotNull(response);
		assertEquals(response.getStatus(), 404);
		assertNotNull(response.getEntity());
	}


	private static Field convertMapToField(Map<String, Object> fieldMap) {
		try {
			return mapper.readValue(
				mapper.writeValueAsString(fieldMap),
				Field.class
			);
		} catch (IOException e) {
			return null;
		}
	}

	private static ContentType getContentType() throws DotDataException {
		User user = APILocator.getUserAPI().getSystemUser();

		return APILocator.getContentTypeAPI(user).search(" velocity_var_name = 'Employee'").get(0);
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
