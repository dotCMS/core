package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import javax.servlet.http.HttpServletRequest;

@Path("/v2/contenttype/{typeIdOrVarName}/fields")
public class FieldResource {

    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response updateFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName, final String fieldsJson,
                                 @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {
        return null;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public final Response getContentTypeFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName,
                                               @Context final HttpServletRequest req) {
        return null;
    }

    @DELETE
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response deleteFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName, final String[] fieldsID, @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {
        return null;
    }
}
