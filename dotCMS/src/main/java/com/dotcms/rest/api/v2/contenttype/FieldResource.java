package com.dotcms.rest.api.v2.contenttype;


import static com.dotcms.util.CollectionsUtils.imap;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
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
import io.swagger.v3.oas.annotations.tags.Tag;


@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Path("/v2/contenttype/{typeIdOrVarName}/fields")
@Tag(name = "Content Type Field")
public class FieldResource implements Serializable {
    private final WebResource webResource;
    private final FieldAPI fieldAPI;

    public FieldResource() {
        this(new WebResource(), APILocator.getContentTypeFieldAPI());
    }

    @VisibleForTesting
    protected FieldResource(final WebResource webresource, final FieldAPI fieldAPI) {
        this.fieldAPI = fieldAPI;
        this.webResource = webresource;
    }

    private static final long serialVersionUID = 1L;


    @Operation(
        summary = "Update content type fields (deprecated v2)",
        description = "Updates multiple fields for a content type. This v2 endpoint is deprecated - use v3 API instead."
    )
    @ApiResponse(responseCode = "200", description = "Fields updated successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldListView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid field data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Content type not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    /**
     * @deprecated {@link com.dotcms.rest.api.v3.contenttype.FieldResource#updateFields(String, String, HttpServletRequest)}
     * @since 5.2
     */
    @Deprecated()
    public Response updateFields(@Parameter(description = "Content type ID or variable name", required = true) @PathParam("typeIdOrVarName") final String typeIdOrVarName, @RequestBody(description = "Fields JSON data", required = true) final String fieldsJson,
                                 @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            final List<Field> fields = new JsonFieldTransformer(fieldsJson).asList();

            for (final Field field : fields) {
                fieldAPI.save(field, user);
            }
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
            final List<Field> contentTypeFields = fieldAPI.byContentTypeId(contentType.id());
            response = Response.ok(new ResponseEntityFieldListView(new JsonFieldTransformer(contentTypeFields).mapList())).build();
        } catch (Exception e) {
            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @Operation(
        summary = "Create content type field",
        description = "Creates a new field for a content type. The field definition is provided as JSON in the request body."
    )
    @ApiResponse(responseCode = "200", description = "Field created successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid field data or field ID should not be set")
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Content type not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response createContentTypeField(@Parameter(description = "Content type ID or variable name", required = true) @PathParam("typeIdOrVarName") final String typeIdOrVarName, @RequestBody(description = "Field JSON definition", required = true) final String fieldJson,
            @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should not be set");

            } else {

                field = fieldAPI.save(field, user);

                response = Response.ok(new ResponseEntityFieldView(new JsonFieldTransformer(field).mapObject())).build();
            }
        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @Operation(
        summary = "Get content type fields (deprecated v2)",
        description = "Retrieves all fields for a specific content type. This v2 endpoint is deprecated - use v3 API instead."
    )
    @ApiResponse(responseCode = "200", description = "Fields retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldListView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Content type not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @GET
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON })
    /**
     * @deprecated {@link com.dotcms.rest.api.v3.contenttype.FieldResource#getContentTypeFields(String, String, HttpServletRequest)}
     * @since 5.2
     */
    @Deprecated()
    public final Response getContentTypeFields(@Parameter(description = "Content type ID or variable name", required = true) @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        final User user = initData.getUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);

        Response response = null;

        try {
            //if we're dealing with a UUID we can use it right away. Otherwise Will attempt to resolve the ContentType out of a varName
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final List<Field> fields = fieldAPI.byContentTypeId(contentTypeAPI.find(contentTypeId).id());

            response = Response.ok(new ResponseEntityFieldListView(new JsonFieldTransformer(fields).mapList())).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }


    @Operation(
        summary = "Get content type field by ID",
        description = "Retrieves a specific field from a content type by its unique field ID."
    )
    @ApiResponse(responseCode = "200", description = "Field retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Field not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @GET
    @Path("/id/{fieldId}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getContentTypeFieldById(
            @Parameter(description = "Field ID", required = true) @PathParam("fieldId") final String fieldId, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);

        Response response = null;
        try {

            final Field field = fieldAPI.find(fieldId);

            response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(field).mapObject())).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @Operation(
        summary = "Get content type field by variable name",
        description = "Retrieves a specific field from a content type by its variable name."
    )
    @ApiResponse(responseCode = "200", description = "Field retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Content type or field not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @GET
    @Path("/var/{fieldVar}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getContentTypeFieldByVar(@Parameter(description = "Content type ID or variable name", required = true) @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @Parameter(description = "Field variable name", required = true) @PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();
        Response response = null;
        try {
            //if we're dealing with a UUID we can use it right away. Otherwise Will attempt to resolve the ContentType out of a varName
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final Field field = fieldAPI.byContentTypeIdAndVar(contentTypeId, fieldVar);

            response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(field).mapObject())).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }


    @Operation(
        summary = "Update content type field by ID",
        description = "Updates a specific field in a content type by its field ID. The field must have an ID set in the JSON data."
    )
    @ApiResponse(responseCode = "200", description = "Field updated successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid field data or field ID not set")
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Field not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PUT
    @Path("/id/{fieldId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateContentTypeFieldById(@Parameter(description = "Field ID to update", required = true) @PathParam("fieldId") final String fieldId,
            @RequestBody(description = "Field JSON data with updates", required = true) final String fieldJson, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (!UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

            } else {

                final Field currentField = fieldAPI.find(fieldId);

                if (!currentField.id().equals(field.id())) {

                    throw new DotDataValidationException("Field id '"+ field.id() +"' does not match a field with id '"+ currentField.id() +"'");

                } else {

                    field = fieldAPI.save(field, user);

                    response = Response.ok(new ResponseEntityFieldView(new JsonFieldTransformer(field).mapObject())).build();
                }
            }
        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @Operation(
        summary = "Update content type field by variable name",
        description = "Updates a specific field in a content type by its variable name. The field definition is provided as JSON in the request body."
    )
    @ApiResponse(responseCode = "200", description = "Field updated successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid field data or field ID not set")
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Content type or field not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PUT
    @Path("/var/{fieldVar}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateContentTypeFieldByVar(@Parameter(description = "Content type ID or variable name", required = true) @PathParam("typeIdOrVarName") final String typeIdOrVarName, @Parameter(description = "Field variable name to update", required = true) @PathParam("fieldVar") final String fieldVar,
            @RequestBody(description = "Field JSON data with updates", required = true) final String fieldJson, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (!UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

            } else {
                final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                        : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
                final Field currentField = fieldAPI.byContentTypeIdAndVar(contentTypeId, fieldVar);

                if (!currentField.id().equals(field.id())) {

                    throw new DotDataValidationException("Field id '"+ field.id() +"' does not match a field with id '"+ currentField.id() +"'");

                } else {

                    field = fieldAPI.save(field, user);

                    response = Response.ok(new ResponseEntityFieldView(new JsonFieldTransformer(field).mapObject())).build();
                }
            }
        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }


    @Operation(
        summary = "Delete multiple fields (deprecated v2)",
        description = "Deletes multiple fields from a content type by their field IDs. This v2 endpoint is deprecated - use v3 API instead."
    )
    @ApiResponse(responseCode = "200", description = "Fields deleted successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldOperationView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid field IDs")
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Content type or fields not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @DELETE
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    /**
     * @deprecated {@link com.dotcms.rest.api.v3.contenttype.FieldResource#deleteFields(String, String[], HttpServletRequest)}
     * @since 5.2
     */
    @Deprecated()
    public Response deleteFields(@Parameter(description = "Content type ID or variable name", required = true) @PathParam("typeIdOrVarName") final String typeIdOrVarName, @RequestBody(description = "Array of field IDs to delete", required = true) final String[] fieldsID,
                                 @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
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
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final List<Field> contentTypeFields = fieldAPI.byContentTypeId(contentTypeId);
            response = Response.ok(new ResponseEntityFieldOperationView(imap("deletedIds", deletedIds,
                    "fields", new JsonFieldTransformer(contentTypeFields).mapList()))).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @Operation(
        summary = "Delete content type field by ID",
        description = "Deletes a specific field from a content type by its unique field ID. This removes the field and all its data permanently."
    )
    @ApiResponse(responseCode = "200", description = "Field deleted successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldDeletionView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Field not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @DELETE
    @Path("/id/{fieldId}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteContentTypeFieldById(
            @Parameter(description = "Field ID to delete", required = true) @PathParam("fieldId") final String fieldId,
            @Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;
        try {

            final Field field = fieldAPI.find(fieldId);
            fieldAPI.delete(field, user);

            response = Response.ok(new ResponseEntityFieldDeletionView((String)null)).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @Operation(
        summary = "Delete content type field by variable name",
        description = "Deletes a specific field from a content type by its variable name. This removes the field and all its data permanently."
    )
    @ApiResponse(responseCode = "200", description = "Field deleted successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityFieldDeletionView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "404", description = "Content type or field not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @DELETE
    @Path("/var/{fieldVar}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteContentTypeFieldByVar(@Parameter(description = "Content type ID or variable name", required = true) @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @Parameter(description = "Field variable name to delete", required = true) @PathParam("fieldVar") final String fieldVar,
            @Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;
        try {
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final Field field = fieldAPI.byContentTypeIdAndVar(contentTypeId, fieldVar);

            fieldAPI.delete(field, user);

            response = Response.ok(new ResponseEntityFieldDeletionView((String)null)).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }
}
