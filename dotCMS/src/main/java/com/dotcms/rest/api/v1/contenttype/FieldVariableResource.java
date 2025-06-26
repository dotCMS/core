package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.transform.field.JsonFieldVariableTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

/**
 * This REST Endpoint provides the ability to manage Field Variables for a given Field in a Content
 * Type in dotCMS.
 * <p>Field variables provide extended options for configuring fields in dotCMS. A field variable
 * takes the form of a key-value pair, but should not be confused with the Key/Value Field; rather,
 * field variables are properties of all fields, capable of changing a field's behavior in important
 * ways.</p>
 *
 * @author Anibal Gomez
 * @since Apre 26th, 2017
 */
@Path("/v1/contenttype/{typeId}/fields")
@Tag(name = "Content Type Field", description = "Content type field management and configuration")
public class FieldVariableResource implements Serializable {

	private final transient WebResource webResource;
	private final transient FieldAPI fieldAPI;

	public FieldVariableResource() {
		this(new WebResource(), APILocator.getContentTypeFieldAPI());
	}

	@VisibleForTesting
	public FieldVariableResource(final WebResource webresource, final FieldAPI fieldAPI) {
		this.webResource = webresource;
		this.fieldAPI = fieldAPI;
	}

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a Field Variable for a given Field based on its ID. Here's an example of how you can
	 * call this method:
	 * <pre>
	 *     POST {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/id/{{fieldId}}/variables
	 *
	 *     JSON Body:
	 *
	 *     {
	 *         "key": "{{field-variable-key}}",
	 *         "value": "{{field-variable-value}}",
	 *         "clazz": "com.dotcms.contenttype.model.field.FieldVariable",
	 *         "fieldId": "{{field-id}}"
	 *     }
	 * </pre>
	 *
	 * @param typeId            The ID of the Content Type that the Field is associated with.
	 * @param fieldId           The ID of the Field that the new Field Variable will be associated
	 *                          with.
	 * @param fieldVariableJson the JSON body with the information of the new Field Variable.
	 * @param req               The current instance of the {@link HttpServletRequest}.
	 * @param res               The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The new Field Variable.
	 *
	 * @throws DotDataException     An error occurred when creating the Field Variable in the
	 *                              database.
	 * @throws DotSecurityException The current user does not have the necessary permissions to
	 *                              execute this action.
	 */
	@POST
	@Path("/id/{fieldId}/variables")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response createFieldVariableByFieldId(@PathParam("typeId") final String typeId,
												 @PathParam("fieldId") final String fieldId,
												 final String fieldVariableJson,
												 @Context final HttpServletRequest req,
												 @Context final HttpServletResponse res) throws DotDataException, DotSecurityException {
		final User user = new WebResource.InitBuilder(this.webResource)
				.requestAndResponse(req, res)
				.requiredBackendUser(true)
				.rejectWhenNoUser(true)
				.init().getUser();
		final Field field = this.fieldAPI.find(fieldId);
		final FieldVariable fieldVariable = this.saveFieldVariable(field, fieldVariableJson, user);
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
	}

	/**
	 * Creates a Field Variable for a given Field based on its Velocity Variable Name. Here's an
	 * example of how you can call this method:
	 * <pre>
	 *     POST {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/var/{{fieldVarName}}/variables
	 *
	 *     JSON Body:
	 *
	 *     {
	 *         "key": "{{field-variable-key}}",
	 *         "value": "{{field-variable-value}}",
	 *         "clazz": "com.dotcms.contenttype.model.field.FieldVariable",
	 *         "fieldId": "{{field-id}}"
	 *     }
	 * </pre>
	 *
	 * @param typeId            The ID of the Content Type that the Field is associated with.
	 * @param fieldVar          The Valocity Variable Name of the Field that the new Field Variable
	 *                          will be associated with.
	 * @param fieldVariableJson the JSON body with the information of the new Field Variable.
	 * @param req               The current instance of the {@link HttpServletRequest}.
	 * @param res               The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The new Field Variable.
	 *
	 * @throws DotDataException     An error occurred when creating the Field Variable in the
	 *                              database.
	 * @throws DotSecurityException The current user does not have the necessary permissions to
	 *                              execute this action.
	 */
	@POST
	@Path("/var/{fieldVar}/variables")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response createFieldVariableByFieldVar(@PathParam("typeId") final String typeId,
												  @PathParam("fieldVar") final String fieldVar,
												  final String fieldVariableJson,
												  @Context final HttpServletRequest req,
												  @Context final HttpServletResponse res) throws DotDataException, DotSecurityException {
		final User user = new WebResource.InitBuilder(this.webResource)
				.requestAndResponse(req, res)
				.requiredBackendUser(true)
				.rejectWhenNoUser(true)
				.init().getUser();
		final Field field = this.fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);
		final FieldVariable fieldVariable = this.saveFieldVariable(field, fieldVariableJson, user);
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
	}

	/**
	 * Returns all Field Variables for a given Field based on its ID. Here's an example of how you
	 * can call this method:
	 * <pre>
	 *     GET {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/id/{{fieldId}}/variables
	 * </pre>
	 *
	 * @param typeId  The ID of the Content Type that the Field is associated with.
	 * @param fieldId The ID of the Field that the new Field Variable will be associated with.
	 * @param req     The current instance of the {@link HttpServletRequest}.
	 * @param res     The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The list of all Field Variables of the specified Field.
	 *
	 * @throws DotDataException An error occurred when accessing the database.
	 */
	@GET
	@Path("/id/{fieldId}/variables")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getFieldVariablesByFieldId(@PathParam("typeId") final String typeId,
													 @PathParam("fieldId") final String fieldId,
													 @Context final HttpServletRequest req,
													 @Context final HttpServletResponse res) throws DotDataException {
		final Field field = this.fieldAPI.find(fieldId);
		final List<FieldVariable> fieldVariables = field.fieldVariables();
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(fieldVariables).mapList())).build();
	}

	/**
	 * Returns all Field Variables for a given Field based on its Velocity Variable Name. Here's an
	 * example of how you can call this method:
	 * <pre>
	 *     GET {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/var/{{fieldVarName}}/variables
	 * </pre>
	 *
	 * @param typeId   The ID of the Content Type that the Field is associated with.
	 * @param fieldVar The Velocity Variable Nme of the Field that the new Field Variable will be
	 *                 associated with.
	 * @param req      The current instance of the {@link HttpServletRequest}.
	 * @param res      The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The list of all Field Variables of the specified Field.
	 *
	 * @throws DotDataException An error occurred when accessing the database.
	 */
	@GET
	@Path("/var/{fieldVar}/variables")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getFieldVariablesByFieldVar(@PathParam("typeId") final String typeId,
													  @PathParam("fieldVar") final String fieldVar,
													  @Context final HttpServletRequest req,
													  @Context final HttpServletResponse res) throws DotDataException {
		final Field field = this.fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);
		final List<FieldVariable> fieldVariables = field.fieldVariables();
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(fieldVariables).mapList())).build();
	}

	/**
	 * Returns a Field Variable for a given Field based on its ID and Field Variable ID. Here's an
	 * example of how you can call this method:
	 * <pre>
	 *     GET {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/id/{{fieldId}}/variables/id/{{fieldVariableId}}
	 * </pre>
	 *
	 * @param typeId     The ID of the Content Type that the Field is associated with.
	 * @param fieldId    The ID of the Field that the new Field Variable will be associated with.
	 * @param fieldVarId The ID of the Field Variable that will be retrieved.
	 * @param req        The current instance of the {@link HttpServletRequest}.
	 * @param res        The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The new Field Variable.
	 *
	 * @throws DotDataException An error occurred when retrieving the Field Variable from the
	 *                          database.
	 */
	@GET
	@Path("/id/{fieldId}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response getFieldVariableByFieldId(@PathParam("typeId") final String typeId,
											  @PathParam("fieldId") final String fieldId,
											  @PathParam("fieldVarId") final String fieldVarId,
											  @Context final HttpServletRequest req,
											  @Context final HttpServletResponse res) throws DotDataException {
		final Field field = this.fieldAPI.find(fieldId);
		final FieldVariable fieldVariable = this.getFieldVariable(field, fieldVarId);
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
	}

	/**
	 * Returns a Field Variable for a given Field based on its Velocity Variable Name and Field
	 * Variable ID. Here's an example of how you can call this method:
	 * <pre>
	 *     GET {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/var/{{fieldVarName}}/variables/id/{{fieldVariableId}}
	 * </pre>
	 *
	 * @param typeId     The ID of the Content Type that the Field is associated with.
	 * @param fieldVar   The Velocity Variable Name of the Field that the new Field Variable
	 *                   will be associated with.
	 * @param fieldVarId The ID of the Field Variable that will be retrieved.
	 * @param req        The current instance of the {@link HttpServletRequest}.
	 * @param res        The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The new Field Variable.
	 *
	 * @throws DotDataException An error occurred when retrieving the Field Variable from the
	 *                          database.
	 */
	@GET
	@Path("/var/{fieldVar}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response getFieldVariableByFieldVar(@PathParam("typeId") final String typeId,
											   @PathParam("fieldVar") final String fieldVar,
											   @PathParam("fieldVarId") final String fieldVarId,
											   @Context final HttpServletRequest req,
											   @Context final HttpServletResponse res) throws DotDataException {
		final Field field = this.fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);
		final FieldVariable fieldVariable = this.getFieldVariable(field, fieldVarId);
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
	}

	/**
	 * Updates a Field Variable for a given Field based on its ID and Field Variable ID. Here's an
	 * example of how you can call this method:
	 * <pre>
	 *     PUT {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/id/{{fieldID}}/variables/id/{{fieldVarId}}
	 *
	 *     JSON Body:
	 *
	 *     {
	 *         "key": "{{field-variable-key}}",
	 *         "value": "{{field-variable-value}}",
	 *         "clazz": "com.dotcms.contenttype.model.field.FieldVariable",
	 *         "fieldId": "{{field-id}}"
	 *     }
	 * </pre>
	 *
	 * @param typeId            The ID of the Content Type that the Field is associated with.
	 * @param fieldId           The ID of the Field that the new Field Variable will be associated
	 *                          with.
	 * @param fieldVarId        The ID of the Field Variable that will be retrieved.
	 * @param fieldVariableJson the JSON body with the information of the new Field Variable.
	 * @param req               The current instance of the {@link HttpServletRequest}.
	 * @param res               The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The updated Field Variable.
	 *
	 * @throws DotDataException     An error occurred when updating the Field Variable in the
	 *                              database.
	 * @throws DotSecurityException The current user does not have the necessary permissions to
	 *                              execute this action.
	 */
	@PUT
	@Path("/id/{fieldId}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response updateFieldVariableByFieldId(@PathParam("typeId") final String typeId,
												 @PathParam("fieldId") final String fieldId,
												 @PathParam("fieldVarId") final String fieldVarId,
												 final String fieldVariableJson,
												 @Context final HttpServletRequest req,
												 @Context final HttpServletResponse res) throws DotDataException, DotSecurityException {
		final User user = new WebResource.InitBuilder(this.webResource)
				.requestAndResponse(req, res)
				.requiredBackendUser(true)
				.rejectWhenNoUser(true)
				.init().getUser();
		final Field field = this.fieldAPI.find(fieldId);
		final FieldVariable updatedFieldVariable = this.updateFieldVariable(fieldVarId, fieldVariableJson, field, user);
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(updatedFieldVariable).mapObject())).build();
	}

	/**
	 * Updates a Field Variable for a given Field based on its Velocity Variable Name and Field
	 * Variable ID. Here's an example of how you can call this method:
	 * <pre>
	 *     PUT {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/var/{{fieldVarName}}/variables/id/{{fieldVarId}}
	 *
	 *     JSON Body:
	 *
	 *     {
	 *         "key": "{{field-variable-key}}",
	 *         "value": "{{field-variable-value}}",
	 *         "clazz": "com.dotcms.contenttype.model.field.FieldVariable",
	 *         "fieldId": "{{field-id}}"
	 *     }
	 * </pre>
	 *
	 * @param typeId            The ID of the Content Type that the Field is associated with.
	 * @param fieldVar          The Velocity Variable Name of the Field that the new Field Variable
	 *                          will be associated with.
	 * @param fieldVarId        The ID of the Field Variable that will be retrieved.
	 * @param fieldVariableJson the JSON body with the information of the new Field Variable.
	 * @param req               The current instance of the {@link HttpServletRequest}.
	 * @param res               The current instance of the {@link HttpServletResponse}.
	 *
	 * @return The updated Field Variable.
	 *
	 * @throws DotDataException     An error occurred when updating the Field Variable in the
	 *                              database.
	 * @throws DotSecurityException The current user does not have the necessary permissions to
	 *                              execute this action.
	 */
	@PUT
	@Path("/var/{fieldVar}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response updateFieldVariableByFieldVar(@PathParam("typeId") final String typeId,
												  @PathParam("fieldVar") final String fieldVar,
												  @PathParam("fieldVarId") final String fieldVarId,
												  final String fieldVariableJson,
												  @Context final HttpServletRequest req,
												  @Context final HttpServletResponse res) throws DotDataException, DotSecurityException {
		final User user = new WebResource.InitBuilder(this.webResource)
				.requestAndResponse(req, res)
				.requiredBackendUser(true)
				.rejectWhenNoUser(true)
				.init().getUser();
		final Field field = this.fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);
		final FieldVariable updatedFieldVariable = this.updateFieldVariable(fieldVarId, fieldVariableJson, field, user);
		return Response.ok(new ResponseEntityView<>(new JsonFieldVariableTransformer(updatedFieldVariable).mapObject())).build();
	}

	/**
	 * Deletes a Field Variable for a given Field based on its ID and Field Variable ID. Here's an
	 * example of how you can call this method:
	 * <pre>
	 *     DELETE {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/id/{{fieldId}}/variables/id/{{fieldVarId}}
	 * </pre>
	 *
	 * @param typeId     The ID of the Content Type that the Field is associated with.
	 * @param fieldId    The ID of the Field that the new Field Variable will be associated with.
	 * @param fieldVarId The ID of the Field Variable that will be retrieved.
	 * @param req        The current instance of the {@link HttpServletRequest}.
	 * @param res        The current instance of the {@link HttpServletResponse}.
	 *
	 * @return An empty response indicating a successful deletion.
	 *
	 * @throws DotDataException An error occurred when deleting the Field Variable from the
	 *                          database.
	 */
	@DELETE
	@Path("/id/{fieldId}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response deleteFieldVariableByFieldId(@PathParam("typeId") final String typeId,
												 @PathParam("fieldId") final String fieldId,
												 @PathParam("fieldVarId") final String fieldVarId,
												 @Context final HttpServletRequest req,
												 @Context final HttpServletResponse res) throws DotDataException, UniqueFieldValueDuplicatedException {
		new WebResource.InitBuilder(this.webResource)
				.requestAndResponse(req, res)
				.requiredBackendUser(true)
				.rejectWhenNoUser(true)
				.init();
		final Field field = this.fieldAPI.find(fieldId);
		this.deleteFieldVariable(field, fieldVarId);
		return Response.ok(new ResponseEntityView<>((String) null)).build();
	}

	/**
	 * Deletes a Field Variable for a given Field based on its Velocity Variable Name and Field
	 * Variable ID. Here's an example of how you can call this method:
	 * <pre>
	 *     DELETE {{serverURL}}/api/v1/contenttype/{{contentTypeId}}/fields/var/{{fieldVarName}}/variables/id/{{fieldVarId}}
	 * </pre>
	 *
	 * @param typeId     The ID of the Content Type that the Field is associated with.
	 * @param fieldVar   The Velocity Variable Name of the Field that the new Field Variable
	 *                      will be
	 *                   associated with.
	 * @param fieldVarId The ID of the Field Variable that will be retrieved.
	 * @param req        The current instance of the {@link HttpServletRequest}.
	 * @param res        The current instance of the {@link HttpServletResponse}.
	 *
	 * @return An empty response indicating a successful deletion.
	 *
	 * @throws DotDataException An error occurred when deleting the Field Variable from the
	 *                          database.
	 */
	@DELETE
	@Path("/var/{fieldVar}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public Response deleteFieldVariableByFieldVar(@PathParam("typeId") final String typeId,
												  @PathParam("fieldVar") final String fieldVar,
												  @PathParam("fieldVarId") final String fieldVarId,
												  @Context final HttpServletRequest req,
												  @Context final HttpServletResponse res) throws DotDataException, UniqueFieldValueDuplicatedException {
		new WebResource.InitBuilder(this.webResource)
				.requestAndResponse(req, res)
				.requiredBackendUser(true)
				.rejectWhenNoUser(true)
				.init();
		final Field field = this.fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);
		this.deleteFieldVariable(field, fieldVarId);
		return Response.ok(new ResponseEntityView<>((String) null)).build();
	}

	/**
	 * Deletes the specified Field Variable from the given Field.
	 *
	 * @param field      The {@link Field} that the Field Variable is associated with.
	 * @param fieldVarId The ID of the Field Variable that will be deleted.
	 *
	 * @throws DotDataException An error occurred when deleting the Field Variable from the
	 *                          database.
	 */
	@WrapInTransaction
	private void deleteFieldVariable(final Field field, final String fieldVarId) throws DotDataException, UniqueFieldValueDuplicatedException {
		final FieldVariable fieldVariable = this.getFieldVariable(field, fieldVarId);
		this.fieldAPI.delete(fieldVariable);
	}

	/**
	 * Returns a Field Variable from a Field based on its ID.
	 *
	 * @param field      The {@link Field} containing the Field Variable.
	 * @param fieldVarId The ID of the Field Variable that will be retrieved.
	 *
	 * @return The {@link FieldVariable}.
	 *
	 * @throws DotDataException An error occurred when retrieving the Field Variable from the
	 *                          database.
	 */
	private FieldVariable getFieldVariable(final Field field, final String fieldVarId) throws DotDataException {
		final FieldVariable fieldVariable = field.fieldVariablesMap().get(fieldVarId);
		if (null == fieldVariable || UtilMethods.isNotSet(fieldVariable.id())) {
			throw new NotFoundInDbException(String.format("Field Variable with ID '%s' was not found", fieldVarId));
		}
		if (!Objects.equals(field.id(), fieldVariable.fieldId())) {
			throw new DotDataException(String.format("Field Variable with ID '%s' is not associated to Field " +
					"'%s' [ %s ]", fieldVarId, field.name(), field.id()));
		}
		return fieldVariable;
	}

	/**
	 * Saves the specified Field Variable to the given Field.
	 *
	 * @param field             The {@link Field} that the new Field Variable will be associated
	 *                          with.
	 * @param fieldVariableJson The JSON body with the information of the new Field Variable.
	 * @param user              The {@link User} performing the action.
	 *
	 * @return The new {@link FieldVariable}.
	 *
	 * @throws DotDataException     An error occurred when creating the Field Variable in the
	 *                              database.
	 * @throws DotSecurityException The current user does not have the necessary permissions to
	 *                              perform this action.
	 */
	private FieldVariable saveFieldVariable(final Field field, final String fieldVariableJson,
											final User user) throws DotDataException, DotSecurityException {
		checkNotNull(field, "'field' parameter is required");
		checkNotEmpty(fieldVariableJson, IllegalArgumentException.class,"'fieldVariableJson' parameter is required");
		final FieldVariable fieldVariable = this.jsonToFieldVariable(fieldVariableJson, field, false);
		return this.fieldAPI.save(fieldVariable, user);
	}

	/**
	 * Updates the specified Field Variable to the given Field.
	 *
	 * @param fieldVarId        The ID of the existing Field Variable.
	 * @param fieldVariableJson The JSON body with the information of the updated Field Variable.
	 * @param field             The {@link Field} that the Field Variable is associated with.
	 * @param user              The {@link User} performing the action.
	 *
	 * @return The updated {@link FieldVariable}.
	 *
	 * @throws DotDataException     An error occurred when updating the Field Variable in the
	 *                              database.
	 * @throws DotSecurityException The current user does not have the necessary permissions to
	 *                              perform this action.
	 */
	private FieldVariable updateFieldVariable(final String fieldVarId,
											  final String fieldVariableJson, final Field field,
											  final User user) throws DotDataException, DotSecurityException {
		final FieldVariable updatedFieldVariable = this.jsonToFieldVariable(fieldVariableJson, field, true);
		final FieldVariable currentFieldVariable = this.getFieldVariable(field, fieldVarId);
		if (!Objects.equals(currentFieldVariable.id(), updatedFieldVariable.id())) {
			throw new DotDataException(String.format("Existing Field Variable ID '%s' does not match the updated Field Variable ID " +
							"'%s'", fieldVarId, updatedFieldVariable.id()));
		}
		return this.fieldAPI.save(updatedFieldVariable, user);
	}

	/**
	 * Takes a JSON representation of a Field Variable and converts it to a Field Variable object.
	 *
	 * @param fieldVariableJson The JSON representation of the Field Variable.
	 * @param field             The Field that the Field Variable is associated with.
	 *
	 * @return The {@link FieldVariable} object.
	 *
	 * @throws DotDataException An error occurred when converting the JSON to a Field Variable.
	 */
	private FieldVariable jsonToFieldVariable(final String fieldVariableJson, final Field field,
											  final boolean isFieldVariableIdRequired) throws DotDataException {
		final FieldVariable fieldVariable =
				new JsonFieldVariableTransformer(fieldVariableJson).from();
		if (!UtilMethods.isSet(fieldVariable.id()) && isFieldVariableIdRequired) {
			throw new DotDataException("Field Variable ID is required");
		} else if (!UtilMethods.isSet(fieldVariable.fieldId()) || !Objects.equals(fieldVariable.fieldId(), field.id())) {
			throw new DotDataException(String.format("Field Variable with ID '%s' is not associated to Field " +
					"'%s' [ %s ]", fieldVariable.fieldId(), field.name(), field.id()));
		}
		return fieldVariable;
	}

}
