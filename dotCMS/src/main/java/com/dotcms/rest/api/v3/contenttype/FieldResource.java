package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.layout.FieldLayout;
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

@Path("/v2/contenttype/{typeIdOrVarName}/fields")
public class FieldResource {
    private WebResource webResource;

    public FieldResource() {
        this.webResource = new WebResource();
    }

    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response updateFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName, final String fieldsJson,
                                 @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {
        /*List<Field> fieldsToUpdate = form.getField();
        ContentType contenType = APILocator.getContentTypeAPI().find(typeIdOrVarName);
        FieldLayout fieldLayout = new FieldLayout(contenType.fields());
        FieldLayout fieldLayoutUpdated = fieldLayout.update(fieldsToUpdate);

        try {
            fieldLayoutUpdated.validate();
            this.saveFields(fieldsToUpdate);
            return Response.ok(new ResponseEntityView(fieldLayoutUpdated.getFields()));
        } catch (FieldLayoutValidationException e) {
            throw new BadRequestException(e);
        }*/

        return null;
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

        ContentType contenType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
        final FieldLayout fieldLayout = new FieldLayout(contenType.fields());

        return Response.ok(new ResponseEntityView(fieldLayout.getFields())).build();
    }

    @DELETE
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response deleteFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName, final String[] fieldsID, @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {
        /*String[] fieldsID = form.getFieldIds();
        ContentType contenType = APILocator.getContentTypeAPI().find(typeIdOrVarName);
        FieldLayout fieldLayout = new FieldLayout(contenType.fields());
        FieldLayout fieldLayoutUpdated = fieldLayout.remove(fieldsID);

        try {
            fieldLayoutUpdated.validate();
            this.deleteFields(fieldsID);
            return Response.ok(new ResponseEntityView(fieldLayoutUpdated.getFields()));
        } catch (FieldLayoutValidationException e) {
            throw new BadRequestException(e);
        }*/

        return null;
    }
}
