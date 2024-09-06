package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.business.ContentTypeFieldLayoutAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resource for handle fields operations, all this end point check if the {@link ContentType}'s layout is valid
 *
 * @see FieldLayout
 */
@Path("/v3/contenttype/{typeIdOrVarName}/fields")
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
}
