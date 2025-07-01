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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
     * Update a field and return the new {@link ContentType}'s layout in the response.
     * The request body should have the follow sintax:
     *
     * <code>
     *     {
     *         field: {
     *             //All the field attributes including those that we want to keep with the same value
     *         }
     *     }
     * </code>
     *
     * If the sortOrder attribute is sent it is ignore.
     * If the content type has a wrong layout then it is fix first before the field update.
     *
     * @param typeIdOrVarName COntent type's id
     * @param fieldId Field to update id
     * @param updateFieldForm field attributes
     * @param req Http request
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     *
     * @see FieldLayout
     */
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    @Path("/{id}")
    public Response updateField(
            @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @PathParam("id") final String fieldId,
            final UpdateFieldForm updateFieldForm,
            @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();

        final Field fieldFromInput = updateFieldForm.getField();
        final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);

        final FieldLayout fieldLayout = this.contentTypeFieldLayoutAPI.updateField(contentType, fieldFromInput, user);
        return Response.ok(new ResponseEntityView<>(fieldLayout.getRows())).build();
    }

    /**
     * Move field and return the new {@link ContentType}'s layout in the response.
     * The request body should have the follow sintax:
     *
     * <code>
     *     {
     *         layout: [
     *             {
     *                 divider: {
     *                     //All the row field attributes
     *                 },
     *                 columns: [
     *                   {
     *                      columnDivider: {
     *                          //All the column field attributes
     *                      },
     *                      fields: [
     *                          {
     *                              //All the field attributes
     *                          },
     *                          {
     *                              //All the field attributes
     *                          }
     *                      ]
     *                   }
     *                 ]
     *             }
     *         ]
     *     }
     * </code>
     *
     * The sortOrder attribute is sent it is ignore, the array index is take as sortOrder.
     * If the content type has a wrong layout then it is fix first before the field update.
     * If a new field is sent in the set the fields it is created in the order where it is put.
     *
     * @param typeIdOrVarName
     * @param moveFieldsForm
     * @param req
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    @Path("/move")
    public Response moveFields(
            @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            final MoveFieldsForm moveFieldsForm,
            @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                this.webResource.init(null, true, req, true, null);
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
