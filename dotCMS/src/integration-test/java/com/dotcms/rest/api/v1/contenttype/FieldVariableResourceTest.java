package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
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

public class FieldVariableResourceTest {

	private static final ObjectMapper mapper = new ObjectMapper();


	@BeforeClass
	public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
	}


	@Test
	public void testByFieldId() throws Exception {
		final FieldVariableResource resource = new FieldVariableResource();

        ContentType contentType = getContentType();
        Field field = getField(contentType);

		Response response;
        Map<String, Object> fieldMap;


        // Ensure no field variables are present
		assertResponse_OK(
			response = resource.getFieldVariablesByFieldId(
				contentType.id(), field.id(), getHttpRequest()
			)
		);

		assertTrue(((List) ((ResponseEntityView) response.getEntity()).getEntity()).isEmpty());


		// Test Field Variable Creation
		assertResponse_OK(
			response = resource.createFieldVariableByFieldId(
				contentType.id(), field.id(), JSON_FIELD_VARIABLE_CREATE.replace("CONTENT_TYPE_FIELD_ID", field.id()), getHttpRequest()
			)
		);

		assertFieldVariableCreate(
			convertMapToFieldVariable(
				fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
			),
			field
		);


		try {
			// Test Field Variable List Retrieval
			assertResponse_OK(
				response = resource.getFieldVariablesByFieldId(
					contentType.id(), field.id(), getHttpRequest()
				)
			);

			// Check newly created field variable is present and returned
			List fieldVariables = (List) ((ResponseEntityView) response.getEntity()).getEntity();

			assertEquals(1, fieldVariables.size());

			assertFieldVariableCreate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) fieldVariables.get(0)
				),
				field
			);


			// Test Field Variable Retrieval
			assertResponse_OK(
				response = resource.getFieldVariableByFieldId(
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);

			assertFieldVariableCreate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				),
				field
			);


			// Test Field Variable Update
			assertResponse_OK(
				response = resource.updateFieldVariableByFieldId(
					contentType.id(), field.id(), (String) fieldMap.get("id"),
					JSON_FIELD_VARIABLE_UPDATE.replace("CONTENT_TYPE_FIELD_ID", field.id()).replace("FIELD_VARIABLE_ID", (String) fieldMap.get("id")),
					getHttpRequest()
				)
			);

			assertFieldVariableUpdate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				),
				field
			);


			// Test Field Variable Retrieval Again
			assertResponse_OK(
				response = resource.getFieldVariableByFieldId(
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);

			assertFieldVariableUpdate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				),
				field
			);
		} finally {
			// Test Field Variable Deletion
			assertResponse_OK(
				response = resource.deleteFieldVariableByFieldId(
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);

			assertResponse_NOT_FOUND(
				response = resource.getFieldVariableByFieldId(
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);
		}

		// Ensure field variables do not exist as it was before the test
		assertResponse_OK(
			response = resource.getFieldVariablesByFieldId(
				contentType.id(), field.id(), getHttpRequest()
			)
		);

		assertTrue(((List) ((ResponseEntityView) response.getEntity()).getEntity()).isEmpty());
	}


	@Test
	public void testByFieldVar() throws Exception {
		final FieldVariableResource resource = new FieldVariableResource();

        ContentType contentType = getContentType();
        Field field = getField(contentType);

		Response response;
        Map<String, Object> fieldMap;


        // Ensure no field variables are present
		assertResponse_OK(
			response = resource.getFieldVariablesByFieldVar(
				contentType.id(), field.variable(), getHttpRequest()
			)
		);

		assertTrue(((List) ((ResponseEntityView) response.getEntity()).getEntity()).isEmpty());


		// Test Field Variable Creation
		assertResponse_OK(
			response = resource.createFieldVariableByFieldVar(
				contentType.id(), field.variable(), JSON_FIELD_VARIABLE_CREATE.replace("CONTENT_TYPE_FIELD_ID", field.id()), getHttpRequest()
			)
		);

		assertFieldVariableCreate(
			convertMapToFieldVariable(
				fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
			),
			field
		);


		try {
			// Test Field Variable List Retrieval
			assertResponse_OK(
				response = resource.getFieldVariablesByFieldVar(
					contentType.id(), field.variable(), getHttpRequest()
				)
			);

			// Check newly created field variable is present and returned
			List fieldVariables = (List) ((ResponseEntityView) response.getEntity()).getEntity();

			assertEquals(1, fieldVariables.size());

			assertFieldVariableCreate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) fieldVariables.get(0)
				),
				field
			);


			// Test Field Variable Retrieval
			assertResponse_OK(
				response = resource.getFieldVariableByFieldVar(
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);

			assertFieldVariableCreate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				),
				field
			);


			// Test Field Variable Update
			assertResponse_OK(
				response = resource.updateFieldVariableByFieldVar(
					contentType.id(), field.variable(), (String) fieldMap.get("id"),
					JSON_FIELD_VARIABLE_UPDATE.replace("CONTENT_TYPE_FIELD_ID", field.id()).replace("FIELD_VARIABLE_ID", (String) fieldMap.get("id")),
					getHttpRequest()
				)
			);

			assertFieldVariableUpdate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				),
				field
			);


			// Test Field Variable Retrieval Again
			assertResponse_OK(
				response = resource.getFieldVariableByFieldVar(
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);

			assertFieldVariableUpdate(
				convertMapToFieldVariable(
					fieldMap = (Map<String, Object>) ((ResponseEntityView) response.getEntity()).getEntity()
				),
				field
			);
		} finally {
			// Test Field Variable Deletion
			assertResponse_OK(
				response = resource.deleteFieldVariableByFieldVar(
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);

			assertResponse_NOT_FOUND(
				response = resource.getFieldVariableByFieldVar(
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest()
				)
			);
		}

		// Ensure field variables do not exist as it was before the test
		assertResponse_OK(
			response = resource.getFieldVariablesByFieldVar(
				contentType.id(), field.variable(), getHttpRequest()
			)
		);

		assertTrue(((List) ((ResponseEntityView) response.getEntity()).getEntity()).isEmpty());
	}


	private static String JSON_FIELD_VARIABLE_CREATE =
		"{"+
				"\"clazz\": \"com.dotcms.contenttype.model.field.ImmutableFieldVariable\","+
				"\"fieldId\": \"CONTENT_TYPE_FIELD_ID\","+
				"\"key\": \"myFieldVariableKey\","+
				"\"value\": \"My Field Variable Value\""+
		"}";

	private static void assertFieldVariableCreate(FieldVariable fieldVariable, Field field) {
		assertNotNull(fieldVariable.id());

		assertNotNull(fieldVariable.fieldId());
		assertEquals(field.id(), fieldVariable.fieldId());

		assertEquals("myFieldVariableKey", fieldVariable.key());
		assertEquals("My Field Variable Value", fieldVariable.value());
	}

	private static String JSON_FIELD_VARIABLE_UPDATE =
		"{"+
				"\"clazz\": \"com.dotcms.contenttype.model.field.ImmutableFieldVariable\","+
				"\"fieldId\": \"CONTENT_TYPE_FIELD_ID\","+
				"\"id\": \"FIELD_VARIABLE_ID\","+
				"\"key\": \"myFieldVariableKeyUpdated\","+
				"\"value\": \"My Field Variable Value Updated\""+
		"}";

	private static void assertFieldVariableUpdate(FieldVariable fieldVariable, Field field) {
		assertNotNull(fieldVariable.id());

		assertNotNull(fieldVariable.fieldId());
		assertEquals(field.id(), fieldVariable.fieldId());

		assertEquals("myFieldVariableKeyUpdated", fieldVariable.key());
		assertEquals("My Field Variable Value Updated", fieldVariable.value());
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


	private static FieldVariable convertMapToFieldVariable(Map<String, Object> fieldMap) {
		try {
			return mapper.readValue(
				mapper.writeValueAsString(fieldMap),
				FieldVariable.class
			);
		} catch (IOException e) {
			return null;
		}
	}

	private static ContentType getContentType() throws DotDataException {
		User user = APILocator.getUserAPI().getSystemUser();

		return APILocator.getContentTypeAPI(user).search(" velocity_var_name = 'Testimonial'").get(0);
	}

	private static Field getField(ContentType contentType) throws DotDataException {

		return APILocator.getContentTypeFieldAPI().byContentTypeAndVar(contentType, "name");
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
