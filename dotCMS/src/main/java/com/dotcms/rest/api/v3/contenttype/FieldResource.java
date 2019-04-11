package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.field.layout.FieldLayoutValidationException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Path("/v3/contenttype/{typeIdOrVarName}/fields")
public class FieldResource {
    private WebResource webResource;
    private FieldAPI fieldAPI;

    public FieldResource() {
        this.webResource = new WebResource();
        this.fieldAPI = APILocator.getContentTypeFieldAPI();
    }

    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response updateFields(
            @PathParam("typeIdOrVarName") final String typeIdOrVarName,
            final UpdateFieldForm updateFieldForm,
            @Context final HttpServletRequest req)
                throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();

        final List<Field> fieldsToUpdate = updateFieldForm.getFields();
        final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
        final FieldLayout fieldLayout = new FieldLayout(contentType.fields());
        final FieldLayout fieldLayoutUpdated = fieldLayout.update(fieldsToUpdate);

        fieldLayoutUpdated.validate();
        this.saveFields(fieldsToUpdate, user);
        return Response.ok(new ResponseEntityView(fieldLayoutUpdated.getRows())).build();
    }

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

        final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
        final FieldLayout fieldLayout = new FieldLayout(contentType.fields());

        return Response.ok(new ResponseEntityView(fieldLayout.getRows())).build();
    }

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
        final FieldLayout fieldLayout = new FieldLayout(contentType.fields());
        final FieldLayout fieldLayoutUpdated = fieldLayout.remove(fieldsID);

        fieldLayoutUpdated.validate();
        this.deleteFields(fieldsID, user);
        return Response.ok(new ResponseEntityView(fieldLayoutUpdated.getRows())).build();
    }

    private void deleteFields(final List<String> fieldsID, final User user)
            throws DotDataException, DotSecurityException {

        for (final String fieldId : fieldsID) {
            try {
                final Field field = fieldAPI.find(fieldId);
                fieldAPI.delete(field, user);
            } catch (NotFoundInDbException e) {
                continue;
            }
        }
    }

    private void saveFields(final List<Field> fields, final User user) throws DotSecurityException, DotDataException {
        for (final Field field : fields) {
            fieldAPI.save(field, user);
        }
    }
}
