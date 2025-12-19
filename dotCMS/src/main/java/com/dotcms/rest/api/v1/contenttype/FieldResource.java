package com.dotcms.rest.api.v1.contenttype;

import static com.dotcms.util.CollectionsUtils.imap;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapStringObjectView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityListMapView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @deprecated {@link com.dotcms.rest.api.v2.contenttype.FieldResource} should be used instead. Path:/v2/contenttype/{typeId}/fields
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Deprecated
@Path("/v1/contenttype/{typeId}/fields")
@Tag(name = "Content Type Field")
public class FieldResource implements Serializable {
	private final WebResource webResource;
	private final FieldAPI fieldAPI;

	public FieldResource() {
		this(new WebResource(), APILocator.getContentTypeFieldAPI());
	}

	@VisibleForTesting
	public FieldResource(final WebResource webresource, final FieldAPI fieldAPI) {
		this.fieldAPI = fieldAPI;
		this.webResource = webresource;
	}

	private static final long serialVersionUID = 1L;


	@Operation(
		summary = "Update content type fields (deprecated)",
		description = "Updates multiple fields for a content type. Use v2 API instead."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Fields updated successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityListMapView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid field data",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Not found - content type not found",
					content = @Content(mediaType = "application/json"))
	})
	@PUT
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateFields(@Parameter(description = "Content type ID", required = true)
								 @PathParam("typeId") final String typeId, 
								 @RequestBody(description = "Fields JSON data", 
											required = true,
											content = @Content(schema = @Schema(implementation = String.class)))
								 final String fieldsJson,
								 @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		
		Response response = null;
		
		try {
			final List<Field> fields = new JsonFieldTransformer(fieldsJson).asList();

			for (final Field field : fields) {
				fieldAPI.save(field, user);
			}

			final List<Field> contentTypeFields = fieldAPI.byContentTypeId(typeId);
			response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(contentTypeFields).mapList())).build();
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
		} catch (DotSecurityException e) {

			throw new ForbiddenException(e);
		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@Operation(
		summary = "Create content type field (deprecated)",
		description = "Creates a new field for a content type. Use v2 API instead."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Field created successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid field data",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Not found - content type not found",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createContentTypeField(@Parameter(description = "Content type ID", required = true)
										   @PathParam("typeId") final String typeId, 
										   @RequestBody(description = "Field JSON data", 
													  required = true,
													  content = @Content(schema = @Schema(implementation = String.class)))
										   final String fieldJson,
										   @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			Field field = new JsonFieldTransformer(fieldJson).from();
			if (UtilMethods.isSet(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should not be set");

			} else {

				field = fapi.save(field, user);

				response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(field).mapObject())).build();
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@Operation(
		summary = "Get content type fields (deprecated)",
		description = "Retrieves all fields for a specific content type. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Fields retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityListMapView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON })
	public final Response getContentTypeFields(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId,
			@Context final HttpServletRequest req) {

		final InitDataObject initData = this.webResource.init(null, true, req, true, null);
		final User user = initData.getUser();
		final ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			List<Field> fields = fapi.byContentTypeId(tapi.find(typeId).id());

			response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(fields).mapList())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@Operation(
		summary = "Get content type field by ID (deprecated)",
		description = "Retrieves a specific field from a content type by its unique field ID. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Field retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type or field not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/id/{fieldId}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getContentTypeFieldById(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId,
			@Parameter(description = "Field ID", required = true) @PathParam("fieldId") final String fieldId, @Context final HttpServletRequest req)
			throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.find(fieldId);

			response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(field).mapObject())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@Operation(
		summary = "Get content type field by variable name (deprecated)",
		description = "Retrieves a specific field from a content type by its variable name. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Field retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type or field not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/var/{fieldVar}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getContentTypeFieldByVar(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId,
											 @Parameter(description = "Field variable name", required = true) @PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
			throws DotDataException, DotSecurityException {

		this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
		final FieldAPI typeFieldAPI = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = typeFieldAPI.byContentTypeIdAndVar(typeId, fieldVar);

			response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(field).mapObject())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@Operation(
		summary = "Update content type field by ID (deprecated)",
		description = "Updates a specific field in a content type by its field ID. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Field updated successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid field data or missing field ID",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type or field not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@PUT
	@Path("/id/{fieldId}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateContentTypeFieldById(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId, @Parameter(description = "Field ID to update", required = true) @PathParam("fieldId") final String fieldId, 
			@RequestBody(description = "Field JSON data with updates", required = true) final String fieldJson, @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
	
		try {
			Field field = new JsonFieldTransformer(fieldJson).from();
			if (!UtilMethods.isSet(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else {

				Field currentField = fapi.find(fieldId);

				if (!currentField.id().equals(field.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field id '"+ fieldId +"' does not match a field with id '"+ field.id() +"'");

				} else {

					field = fapi.save(field, user);

					response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(field).mapObject())).build();
				}
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@Operation(
		summary = "Update content type field by variable name (deprecated)",
		description = "Updates a specific field in a content type by its variable name. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Field updated successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid field data",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type or field not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@PUT
	@Path("/var/{fieldVar}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateContentTypeFieldByVar(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId, @Parameter(description = "Field variable name to update", required = true) @PathParam("fieldVar") final String fieldVar,
			@RequestBody(description = "Field JSON data with updates", required = true) final String fieldJson, @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
	
		try {
			Field field = new JsonFieldTransformer(fieldJson).from();
			if (!UtilMethods.isSet(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else {

				Field currentField = fapi.byContentTypeIdAndVar(typeId, fieldVar);

				if (!currentField.id().equals(field.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field var '"+ fieldVar +"' does not match a field with id '"+ field.id() +"'");

				} else {

					field = fapi.save(field, user);

					response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(field).mapObject())).build();
				}
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@Operation(
		summary = "Delete multiple fields (deprecated)",
		description = "Deletes multiple fields from a content type by their field IDs. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Fields deleted successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - invalid field IDs",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type or one or more fields not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteFields(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId, @RequestBody(description = "Array of field IDs to delete", required = true) final String[] fieldsID, @Context final HttpServletRequest req)
			throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();

		Response response = null;
		try {
			final List<String> deletedIds = new ArrayList<>();

			for (final String fieldId : fieldsID) {
				try {
					final Field field = fieldAPI.find(fieldId);
					fieldAPI.delete(field, user);
					deletedIds.add(fieldId);
				} catch (NotFoundInDbException e) {
					continue;
				}
			}

			final List<Field> contentTypeFields = fieldAPI.byContentTypeId(typeId);
			response = Response.ok(new ResponseEntityView<>(imap("deletedIds", deletedIds,
					"fields", new JsonFieldTransformer(contentTypeFields).mapList()))).build();

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		}catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@Operation(
		summary = "Delete content type field by ID (deprecated)",
		description = "Deletes a specific field from a content type by its field ID. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Field deleted successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type or field not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@Path("/id/{fieldId}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteContentTypeFieldById(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId,
											   @Parameter(description = "Field ID to delete", required = true) @PathParam("fieldId") final String fieldId, @Context final HttpServletRequest req)
			throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();

		Response response = null;
		try {

			Field field = fieldAPI.find(fieldId);
			fieldAPI.delete(field, user);

			String responseString = null;
			response = Response.ok(new ResponseEntityView<>(responseString)).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@Operation(
		summary = "Delete content type field by variable name (deprecated)",
		description = "Deletes a specific field from a content type by its variable name. Use v2 API instead for new implementations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Field deleted successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Content type or field not found",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@Path("/var/{fieldVar}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteContentTypeFieldByVar(@Parameter(description = "Content type ID", required = true) @PathParam("typeId") final String typeId,
			@Parameter(description = "Field variable name to delete", required = true) @PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest req)
			throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.byContentTypeIdAndVar(typeId, fieldVar);

			fapi.delete(field, user);

			response = Response.ok(new ResponseEntityView<>((String)null)).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}
}
