package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldVariableResourceTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	private static final String typeName="fieldVariableResourceTest" + System.currentTimeMillis();
	private static final String fieldName="name";

	@BeforeClass
	public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);
        Field field = FieldBuilder.builder(TextField.class).name(fieldName).contentTypeId(type.id()).build();
        APILocator.getContentTypeFieldAPI().save(field,APILocator.systemUser());
	}

	@AfterClass
	public static void cleanUpData() throws DotDataException, DotSecurityException {
		ContentType contentType = getContentType();
		APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
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
				contentType.id(), field.id(), getHttpRequest(),  new EmptyHttpResponse()
			)
		);

		assertTrue(((List) ((ResponseEntityView) response.getEntity()).getEntity()).isEmpty());


		// Test Field Variable Creation
		assertResponse_OK(
			response = resource.createFieldVariableByFieldId(
				contentType.id(), field.id(), JSON_FIELD_VARIABLE_CREATE.replace("CONTENT_TYPE_FIELD_ID", field.id()), getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.id(), getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
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
					getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
				)
			);

			assertResponse_NOT_FOUND(
				response = resource.getFieldVariableByFieldId(
					contentType.id(), field.id(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
				)
			);
		}

		// Ensure field variables do not exist as it was before the test
		assertResponse_OK(
			response = resource.getFieldVariablesByFieldId(
				contentType.id(), field.id(), getHttpRequest(),  new EmptyHttpResponse()
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
				contentType.id(), field.variable(), getHttpRequest(),  new EmptyHttpResponse()
			)
		);

		assertTrue(((List) ((ResponseEntityView) response.getEntity()).getEntity()).isEmpty());


		// Test Field Variable Creation
		assertResponse_OK(
			response = resource.createFieldVariableByFieldVar(
				contentType.id(), field.variable(), JSON_FIELD_VARIABLE_CREATE.replace("CONTENT_TYPE_FIELD_ID", field.id()), getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.variable(), getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
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
					getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
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
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
				)
			);

			assertResponse_NOT_FOUND(
				response = resource.getFieldVariableByFieldVar(
					contentType.id(), field.variable(), (String) fieldMap.get("id"), getHttpRequest(),  new EmptyHttpResponse()
				)
			);
		}

		// Ensure field variables do not exist as it was before the test
		assertResponse_OK(
			response = resource.getFieldVariablesByFieldVar(
				contentType.id(), field.variable(), getHttpRequest(),  new EmptyHttpResponse()
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
		assertEquals(200, response.getStatus());
		assertNotNull(response.getEntity());
		assertTrue(response.getEntity() instanceof ResponseEntityView);
		assertTrue(
			(ResponseEntityView.class.cast(response.getEntity()).getErrors() == null) ||
			ResponseEntityView.class.cast(response.getEntity()).getErrors().isEmpty()
		);
	}

	private static void assertResponse_NOT_FOUND(Response response){
		assertNotNull(response);
		assertEquals(404, response.getStatus());
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

	private static ContentType getContentType() throws DotDataException, DotSecurityException {
		User user = APILocator.getUserAPI().getSystemUser();

		return APILocator.getContentTypeAPI(user).find(typeName);
	}

	private static Field getField(ContentType contentType) throws DotDataException {

		return APILocator.getContentTypeFieldAPI().byContentTypeAndVar(contentType, fieldName);
	}

	private static HttpServletRequest getHttpRequest() {
		MockHeaderRequest request = new MockHeaderRequest(
			(
				new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
			).request()
		);

		request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

		return request;
	}
}
