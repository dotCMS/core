package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.business.ContentTypeFieldLayoutAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.filtering.Specification;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resource for handle fields operations, all this end point check if the {@link ContentType}'s layout is valid
 *
 * @see FieldLayout
 */
@Path("/v3/contenttype/{typeIdOrVarName}/fields")
@Tag(name = "Content Type Field", description = "Content type field definitions and configuration")
public class FieldResource {

    private final WebResource webResource;
    private final ContentTypeFieldLayoutAPI contentTypeFieldLayoutAPI;

    public FieldResource() {
        this.webResource = new WebResource();
        this.contentTypeFieldLayoutAPI = APILocator.getContentTypeFieldLayoutAPI();
    }

    /**
     * Updates a field and returns the new Content Type's {@link FieldLayout} in the response. The
     * request body must have the follow syntax:
     * <pre>
     * {@code
     *     {
     *         field: {
     *             //All the field attributes including those that we want to keep with the same value
     *         }
     *     }
     * }
     * </pre>
     * <p>
     * The sort order attribute in the field is ignored as it is correctly re-calculated by the API.
     * If the Content Type has a wrong layout, it is also fixed by the API before the field id
     * updated.
     *
     * @param typeIdOrVarName The Content Type ID or Velocity Variable Name.
     * @param fieldId         The ID of the field being updated.
     * @param updateFieldForm The {@link UpdateFieldForm} with the updated attributes for the
     *                        field.
     * @param httpRequest             The current instance of the {@link HttpServletRequest}.
     *
     * @return The updated {@link FieldLayout} reflecting the new changes.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The User performing this action doesn't have the required
     *                              permissions to do so.
     */
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    @Path("/{id}")
    @Operation(
            operationId = "updateContentTypeField",
            summary = "Updates a field in a Content Type",
            description = "Updates a field in a Content Type. The request body must have the follow " +
                    "syntax:",
            tags = {"Content Type Field"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The updated Content Type's layout.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    examples = @ExampleObject(
                                            value = "{\n" +
                                                    "    \"entity\": [\n" +
                                                    "        {\n" +
                                                    "            \"divider\": {\n" +
                                                    "                \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableRowField\",\n" +
                                                    "                \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                    "                \"dataType\": \"SYSTEM\",\n" +
                                                    "                \"fieldContentTypeProperties\": [],\n" +
                                                    "                \"fieldType\": \"Row\",\n" +
                                                    "                \"fieldTypeLabel\": \"Row\",\n" +
                                                    "                \"fieldVariables\": [],\n" +
                                                    "                \"fixed\": false,\n" +
                                                    "                \"forceIncludeInApi\": false,\n" +
                                                    "                \"iDate\": 1764268372000,\n" +
                                                    "                \"id\": \"1b950209ec7b1a5e6901f7fe277e0a6a\",\n" +
                                                    "                \"indexed\": false,\n" +
                                                    "                \"listed\": false,\n" +
                                                    "                \"modDate\": 1764268380000,\n" +
                                                    "                \"name\": \"fields-0\",\n" +
                                                    "                \"readOnly\": false,\n" +
                                                    "                \"required\": false,\n" +
                                                    "                \"searchable\": false,\n" +
                                                    "                \"sortOrder\": 0,\n" +
                                                    "                \"unique\": false,\n" +
                                                    "                \"variable\": \"fields0\"\n" +
                                                    "            },\n" +
                                                    "            \"columns\": [\n" +
                                                    "                {\n" +
                                                    "                    \"columnDivider\": {\n" +
                                                    "                        \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableColumnField\",\n" +
                                                    "                        \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                    "                        \"dataType\": \"SYSTEM\",\n" +
                                                    "                        \"fieldContentTypeProperties\": [],\n" +
                                                    "                        \"fieldType\": \"Column\",\n" +
                                                    "                        \"fieldTypeLabel\": \"Column\",\n" +
                                                    "                        \"fieldVariables\": [],\n" +
                                                    "                        \"fixed\": false,\n" +
                                                    "                        \"forceIncludeInApi\": false,\n" +
                                                    "                        \"iDate\": 1764268372000,\n" +
                                                    "                        \"id\": \"6515a7a25deb3b99711398c14bfe3062\",\n" +
                                                    "                        \"indexed\": false,\n" +
                                                    "                        \"listed\": false,\n" +
                                                    "                        \"modDate\": 1764268380000,\n" +
                                                    "                        \"name\": \"fields-1\",\n" +
                                                    "                        \"readOnly\": false,\n" +
                                                    "                        \"required\": false,\n" +
                                                    "                        \"searchable\": false,\n" +
                                                    "                        \"sortOrder\": 1,\n" +
                                                    "                        \"unique\": false,\n" +
                                                    "                        \"variable\": \"fields1\"\n" +
                                                    "                    },\n" +
                                                    "                    \"fields\": [\n" +
                                                    "                        {\n" +
                                                    "                            \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableTextField\",\n" +
                                                    "                            \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                    "                            \"dataType\": \"TEXT\",\n" +
                                                    "                            \"fieldType\": \"Text\",\n" +
                                                    "                            \"fieldTypeLabel\": \"Text\",\n" +
                                                    "                            \"fieldVariables\": [],\n" +
                                                    "                            \"fixed\": false,\n" +
                                                    "                            \"forceIncludeInApi\": false,\n" +
                                                    "                            \"iDate\": 1764268380000,\n" +
                                                    "                            \"id\": \"ba34f109c9e86793384387e2619267ea\",\n" +
                                                    "                            \"indexed\": true,\n" +
                                                    "                            \"listed\": true,\n" +
                                                    "                            \"modDate\": 1764277924000,\n" +
                                                    "                            \"name\": \"Title\",\n" +
                                                    "                            \"readOnly\": false,\n" +
                                                    "                            \"required\": false,\n" +
                                                    "                            \"searchable\": false,\n" +
                                                    "                            \"sortOrder\": 2,\n" +
                                                    "                            \"unique\": false,\n" +
                                                    "                            \"variable\": \"title\"\n" +
                                                    "                        },\n" +
                                                    "                        {\n" +
                                                    "                            \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableCustomField\",\n" +
                                                    "                            \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                    "                            \"dataType\": \"LONG_TEXT\",\n" +
                                                    "                            \"fieldType\": \"Custom-Field\",\n" +
                                                    "                            \"fieldTypeLabel\": \"Custom Field\",\n" +
                                                    "                            \"fieldVariables\": [\n" +
                                                    "                                {\n" +
                                                    "                                    \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableFieldVariable\",\n" +
                                                    "                                    \"fieldId\": \"803d40e80721ae61196dc566a15c7ea5\",\n" +
                                                    "                                    \"id\": \"6f96be03-6c35-4ced-b825-dd52715b9b06\",\n" +
                                                    "                                    \"key\": \"newRenderMode\",\n" +
                                                    "                                    \"value\": \"component\"\n" +
                                                    "                                }\n" +
                                                    "                            ],\n" +
                                                    "                            \"fixed\": false,\n" +
                                                    "                            \"forceIncludeInApi\": false,\n" +
                                                    "                            \"iDate\": 1764433985000,\n" +
                                                    "                            \"id\": \"803d40e80721ae61196dc566a15c7ea5\",\n" +
                                                    "                            \"indexed\": false,\n" +
                                                    "                            \"listed\": false,\n" +
                                                    "                            \"modDate\": 1764687322000,\n" +
                                                    "                            \"name\": \"My Custom Field\",\n" +
                                                    "                            \"readOnly\": false,\n" +
                                                    "                            \"required\": false,\n" +
                                                    "                            \"searchable\": false,\n" +
                                                    "                            \"sortOrder\": 3,\n" +
                                                    "                            \"unique\": false,\n" +
                                                    "                            \"values\": \"## This is a VTL comment block" +
                                                    "                            \"variable\": \"myCustomField\"\n" +
                                                    "                        }\n" +
                                                    "                    ]\n" +
                                                    "                }\n" +
                                                    "            ]\n" +
                                                    "        }\n" +
                                                    "    ],\n" +
                                                    "    \"errors\": [],\n" +
                                                    "    \"i18nMessagesMap\": {},\n" +
                                                    "    \"messages\": [],\n" +
                                                    "    \"pagination\": null,\n" +
                                                    "    \"permissions\": []\n" +
                                                    "}"))
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access"),
                    @ApiResponse(responseCode = "404", description = "The ID of the specified Field was not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response updateField(
            @PathParam("typeIdOrVarName") @Parameter(
                    description = "The ID or Velocity Variable Name of the Content Type that the " +
                            "updated field belongs to.",
                    schema = @Schema(type = "String")) final String typeIdOrVarName,
            @PathParam("id") @Parameter(
                    description = "The ID of the Field that is being updated.",
                    schema = @Schema(type = "String")) final String fieldId,
            @Parameter(
                    description = "The object containing the updated attributes of the Field.",
                    schema = @Schema(type = "UpdateFieldForm")) final UpdateFieldForm updateFieldForm,
            @Context final HttpServletRequest httpRequest)
            throws DotDataException, DotSecurityException {
        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requestAndResponse(httpRequest, null)
                        .requiredBackendUser(false)
                        .requiredFrontendUser(false)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        final Field fieldToUpdate = updateFieldForm.getField();
        final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
        final FieldLayout fieldLayout = this.contentTypeFieldLayoutAPI.updateField(contentType, fieldToUpdate, user);
        return Response.ok(new ResponseEntityView<>(fieldLayout.getRows())).build();
    }

    /**
     * Moves a field in a Content Type, and returns the new {@link ContentType}'s layout in the
     * response. This endpoint is used when dragging-and-dropping a new field to a Content Type,
     * and when moving a field around in the Content Type's layout. The JSON body in the request
     * has the follow syntax:
     *
     * <pre>{@code
     *     {
     *         layout: [
     *             {
     *                 divider: {
     *                     // Row field attributes
     *                 },
     *                 columns: [
     *                   {
     *                      columnDivider: {
     *                          // Column field attributes
     *                      },
     *                      fields: [
     *                          {
     *                              // Attributes for field #1
     *                          },
     *                          {
     *                              // Attributes for field #2
     *                          },
     *                          {
     *                              // and so on...
     *                          }
     *                      ]
     *                   }
     *                 ]
     *             }
     *         ]
     *     }
     * }
     * </pre>
     * <p>
     * The {@code sortOrder} attribute sent in the body is ignored, and the array index is taken as
     * the real sort order value. If the Content Type has an invalid layout, it will be fixed before
     * the field is update. If a new field is sent in the field array, it is created in the exact
     * order it is placed.
     *
     * @param typeIdOrVarName The Content Type's Identifier or Velocity Variable Name.
     * @param moveFieldsForm  The {@link MoveFieldsForm} object describing the field layout.
     * @param req             The current instance of the {@link HttpServletRequest}.
     *
     * @return The Content Type's new field layout.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The User performing this action doesn't have the required
     *                              permissions to do so.
     */
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    @Path("/move")
    @Operation(
            operationId = "moveOrAddContentTypeField",
            summary = "Moves or adds a field to a Content Type",
            description = "Moves or adds a field to a Content Type. This endpoint is called when " +
                    "dragging-and-dropping a new field to a Content Type, or when moving such a " +
                    "field around in the Content Type's layout.",
            tags = {"Content Type Field"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Field was added or moved successfully",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "    \"entity\": [\n" +
                                                            "        {\n" +
                                                            "            \"divider\": {\n" +
                                                            "                \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableRowField\",\n" +
                                                            "                \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                            "                \"dataType\": \"SYSTEM\",\n" +
                                                            "                \"fieldContentTypeProperties\": [],\n" +
                                                            "                \"fieldType\": \"Row\",\n" +
                                                            "                \"fieldTypeLabel\": \"Row\",\n" +
                                                            "                \"fieldVariables\": [],\n" +
                                                            "                \"fixed\": false,\n" +
                                                            "                \"forceIncludeInApi\": false,\n" +
                                                            "                \"iDate\": 1763758144000,\n" +
                                                            "                \"id\": \"1b950209ec7b1a5e6901f7fe277e0a6a\",\n" +
                                                            "                \"indexed\": false,\n" +
                                                            "                \"listed\": false,\n" +
                                                            "                \"modDate\": 1763758164000,\n" +
                                                            "                \"name\": \"fields-0\",\n" +
                                                            "                \"readOnly\": false,\n" +
                                                            "                \"required\": false,\n" +
                                                            "                \"searchable\": false,\n" +
                                                            "                \"sortOrder\": 0,\n" +
                                                            "                \"unique\": false,\n" +
                                                            "                \"variable\": \"fields0\"\n" +
                                                            "            },\n" +
                                                            "            \"columns\": [\n" +
                                                            "                {\n" +
                                                            "                    \"columnDivider\": {\n" +
                                                            "                        \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableColumnField\",\n" +
                                                            "                        \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                            "                        \"dataType\": \"SYSTEM\",\n" +
                                                            "                        \"fieldContentTypeProperties\": [],\n" +
                                                            "                        \"fieldType\": \"Column\",\n" +
                                                            "                        \"fieldTypeLabel\": \"Column\",\n" +
                                                            "                        \"fieldVariables\": [],\n" +
                                                            "                        \"fixed\": false,\n" +
                                                            "                        \"forceIncludeInApi\": false,\n" +
                                                            "                        \"iDate\": 1763758144000,\n" +
                                                            "                        \"id\": \"6515a7a25deb3b99711398c14bfe3062\",\n" +
                                                            "                        \"indexed\": false,\n" +
                                                            "                        \"listed\": false,\n" +
                                                            "                        \"modDate\": 1763758164000,\n" +
                                                            "                        \"name\": \"fields-1\",\n" +
                                                            "                        \"readOnly\": false,\n" +
                                                            "                        \"required\": false,\n" +
                                                            "                        \"searchable\": false,\n" +
                                                            "                        \"sortOrder\": 1,\n" +
                                                            "                        \"unique\": false,\n" +
                                                            "                        \"variable\": \"fields1\"\n" +
                                                            "                    },\n" +
                                                            "                    \"fields\": [\n" +
                                                            "                        {\n" +
                                                            "                            \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableTextField\",\n" +
                                                            "                            \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                            "                            \"dataType\": \"TEXT\",\n" +
                                                            "                            \"fieldType\": \"Text\",\n" +
                                                            "                            \"fieldTypeLabel\": \"Text\",\n" +
                                                            "                            \"fieldVariables\": [],\n" +
                                                            "                            \"fixed\": false,\n" +
                                                            "                            \"forceIncludeInApi\": false,\n" +
                                                            "                            \"iDate\": 1763758164000,\n" +
                                                            "                            \"id\": \"ba34f109c9e86793384387e2619267ea\",\n" +
                                                            "                            \"indexed\": false,\n" +
                                                            "                            \"listed\": false,\n" +
                                                            "                            \"modDate\": 1763761740000,\n" +
                                                            "                            \"name\": \"title\",\n" +
                                                            "                            \"readOnly\": false,\n" +
                                                            "                            \"required\": false,\n" +
                                                            "                            \"searchable\": false,\n" +
                                                            "                            \"sortOrder\": 2,\n" +
                                                            "                            \"unique\": false,\n" +
                                                            "                            \"variable\": \"title\"\n" +
                                                            "                        },\n" +
                                                            "                        {\n" +
                                                            "                            \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableTextField\",\n" +
                                                            "                            \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                            "                            \"dataType\": \"TEXT\",\n" +
                                                            "                            \"fieldType\": \"Text\",\n" +
                                                            "                            \"fieldTypeLabel\": \"Text\",\n" +
                                                            "                            \"fieldVariables\": [\n" +
                                                            "                                {\n" +
                                                            "                                    \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableFieldVariable\",\n" +
                                                            "                                    \"fieldId\": \"e39533a92ee05d8c083f7e6a1a5ee5e5\",\n" +
                                                            "                                    \"id\": \"7844f04c-0481-4b0d-aa22-95e1a097bb30\",\n" +
                                                            "                                    \"key\": \"My Field Var\",\n" +
                                                            "                                    \"value\": \"My value\"\n" +
                                                            "                                }\n" +
                                                            "                            ],\n" +
                                                            "                            \"fixed\": false,\n" +
                                                            "                            \"forceIncludeInApi\": false,\n" +
                                                            "                            \"iDate\": 1764008792000,\n" +
                                                            "                            \"id\": \"e39533a92ee05d8c083f7e6a1a5ee5e5\",\n" +
                                                            "                            \"indexed\": true,\n" +
                                                            "                            \"listed\": true,\n" +
                                                            "                            \"modDate\": 1764018597000,\n" +
                                                            "                            \"name\": \"Description\",\n" +
                                                            "                            \"readOnly\": false,\n" +
                                                            "                            \"required\": false,\n" +
                                                            "                            \"searchable\": false,\n" +
                                                            "                            \"sortOrder\": 7,\n" +
                                                            "                            \"unique\": false,\n" +
                                                            "                            \"variable\": \"description\"\n" +
                                                            "                        }\n" +
                                                            "                    ]\n" +
                                                            "                }\n" +
                                                            "            ]\n" +
                                                            "        }\n" +
                                                            "    ],\n" +
                                                            "    \"errors\": [],\n" +
                                                            "    \"i18nMessagesMap\": {},\n" +
                                                            "    \"messages\": [],\n" +
                                                            "    \"pagination\": null,\n" +
                                                            "    \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid JSON body"),
                    @ApiResponse(responseCode = "401", description = "User not specified"),
                    @ApiResponse(responseCode = "404", description = "Content Type ID not found"),
                    @ApiResponse(responseCode = "415", description = "Body must be a JSON object"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response moveFields(
            @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            final MoveFieldsForm moveFieldsForm,
            @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {
        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requestAndResponse(req, null)
                        .requiredBackendUser(false)
                        .requiredFrontendUser(false)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
        final FieldLayout layout = moveFieldsForm.getRows(contentType);
        final FieldLayout fieldLayout = this.contentTypeFieldLayoutAPI.moveFields(contentType, layout, user);
        return Response.ok(new ResponseEntityView<>(fieldLayout.getRows())).build();
    }

    /**
     * Return the {@link ContentType}'s layout, If the content type has a wrong layout then it is fix before return
     * This end point don't make any change in Data Base
     *
     * @param typeIdOrVarName
     * @param req
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public final Response getContentTypeFields(
            @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @Context final HttpServletRequest req
    ) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();

        final FieldLayout fieldLayout = this.contentTypeFieldLayoutAPI.getLayout(typeIdOrVarName, user);

        return Response.ok(new ResponseEntityView<>(fieldLayout.getRows())).build();
    }

    /**
     * Delete a set of fields from the {@link ContentType} and return the new {@link ContentType}'s layout
     * @param typeIdOrVarName
     * @param deleteFieldsForm
     * @param req
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @DELETE
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response deleteFields(
            @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            final DeleteFieldsForm deleteFieldsForm,
            @Context final HttpServletRequest req
    )
            throws DotDataException, DotSecurityException {
        final InitDataObject initData =
                this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();

        final List<String> fieldsID = deleteFieldsForm.getFieldsID();
        final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
        final String publishDateVar = contentType.publishDateVar();
        final String expireDateVar = contentType.expireDateVar();

        final List<Field> filteredFields = contentType.fields().stream().filter(field -> fieldsID.contains(field.id())).collect(Collectors.toList());

        for (final Field field : filteredFields) {
            if ((publishDateVar != null && publishDateVar.equals(field.variable())) ||
                    (expireDateVar != null && expireDateVar.equals(field.variable()))){
                throw new DotDataException("Field is being used as Publish or Expire Field at Content Type Level. Please unlink the field before deleting it.");
            }
        };

        final ContentTypeFieldLayoutAPI.DeleteFieldResult deleteFieldResult =
                this.contentTypeFieldLayoutAPI.deleteField(contentType, fieldsID, user);

        return Response.ok(new ResponseEntityView(
                Map.of(
                   "fields", deleteFieldResult.getLayout().getRows(),
                        "deletedIds", deleteFieldResult.getFieldDeletedIds()
                )
        )).build();
    }

    /**
     * Returns the list of fields in a Content Type that meet the specified criteria. For instance,
     * if you need to retrieve all fields marked as 'required', 'unique'`, and 'system indexed',
     * you can call the endpoint like this:
     * <pre>
     *     {@code
     *     {{serverURL}}/api/v3/contenttype/AAA/fields/allfields?filter=REQUIRED&filter=SYSTEM_INDEXED&filter=UNIQUE
     *     }
     * </pre>
     * Just add the same `filter` parameter for each criterion you want to apply.
     *
     * @param request         The current instance of the {@link HttpServletRequest}.
     * @param response        The current instance of the {@link HttpServletResponse}.
     * @param typeIdOrVarName The Identifier or Velocity Variable Name of a given
     *                        {@link ContentType}.
     * @param criteria        A set of {@link FilteringCriteria} objects that will be used to filter
     *                        the fields.
     *
     * @return A {@link FieldResponseView} object that contains the list of {@link Field} objects
     * that meet the specified criteria.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The specified User does not have the necessary permissions to
     *                              execute this operation.
     */
    @GET
    @Path("/allfields")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "allfields",
            summary = "Returns filtered Content Type fields",
            description = "Returns the list of fields in a Content Type that meet the specified criteria.",
            tags = {"Content Type Field"},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "filter",
                                            value = "REQUIRED,SYSTEM_INDEXED,UNIQUE,SHOW_IN_LIST,USER_SEARCHABLE",
                                            summary = "Filter fields by one or more of the specified criteria"
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content type retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    description = "Returning a list of one Field matching the filtering criteria.",
                                                    value = "{\n" +
                                                            "    \"entity\": [\n" +
                                                            "        {\n" +
                                                            "            \"clazz\": \"com.dotcms.contenttype.model.field.ImmutableTextField\",\n" +
                                                            "            \"contentTypeId\": \"3b70f386cf65117a675f284eea928415\",\n" +
                                                            "            \"dataType\": \"TEXT\",\n" +
                                                            "            \"dbColumn\": \"text2\",\n" +
                                                            "            \"defaultValue\": null,\n" +
                                                            "            \"fixed\": false,\n" +
                                                            "            \"forceIncludeInApi\": false,\n" +
                                                            "            \"hint\": null,\n" +
                                                            "            \"iDate\": 1732992811000,\n" +
                                                            "            \"id\": \"e39533a92ee05d8c083f7e6a1a5ee5e5\",\n" +
                                                            "            \"indexed\": true,\n" +
                                                            "            \"listed\": false,\n" +
                                                            "            \"modDate\": 1732992838000,\n" +
                                                            "            \"name\": \"Description\",\n" +
                                                            "            \"owner\": null,\n" +
                                                            "            \"readOnly\": false,\n" +
                                                            "            \"regexCheck\": null,\n" +
                                                            "            \"relationType\": null,\n" +
                                                            "            \"required\": true,\n" +
                                                            "            \"searchable\": false,\n" +
                                                            "            \"sortOrder\": 3,\n" +
                                                            "            \"unique\": true,\n" +
                                                            "            \"values\": null,\n" +
                                                            "            \"variable\": \"description\"\n" +
                                                            "        }\n" +
                                                            "    ],\n" +
                                                            "    \"errors\": [],\n" +
                                                            "    \"i18nMessagesMap\": {},\n" +
                                                            "    \"messages\": [],\n" +
                                                            "    \"pagination\": null,\n" +
                                                            "    \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request, when using invalid filter names"),
                    @ApiResponse(responseCode = "401", description = "Invalid User"),
                    @ApiResponse(responseCode = "404", description = "Content Type was not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public FieldResponseView allFieldsBy(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         @PathParam("typeIdOrVarName") final String typeIdOrVarName,
                                         @QueryParam("filter") final Set<FilteringCriteria> criteria) throws DotDataException, DotSecurityException {
        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();
        final User user = initDataObject.getUser();
        Logger.debug(this, () -> String.format("Returning filtered fields from Content Type '%s' " +
                "using the criteria: %s", typeIdOrVarName, criteria));
        final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
        final Specification<Field> fieldSpecification = FilteringCriteria.specificationsFrom(criteria);
        final List<Field> filteredFields = contentType.fields().stream().filter(fieldSpecification::isSatisfiedBy)
                .collect(Collectors.toList());
        return new FieldResponseView(filteredFields);
    }

}
