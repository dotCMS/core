package com.dotcms.contenttype.model.field;


import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

/**
 * This end-point provides access to information associated to dotCMS FieldType.
 */
@Path("/v1/fieldTypes")
public class FieldTypeResource {

    private final WebResource webResource;
    private FieldTypeFactory fieldTypeFactory;

    public FieldTypeResource() {
        this(new WebResource(), FieldTypeFactory.getInstance());
    }

    @VisibleForTesting
    public FieldTypeResource(final WebResource webresource, FieldTypeFactory fieldTypeFactory) {
        this.webResource = webresource;
        this.fieldTypeFactory = fieldTypeFactory;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response getFieldTypes(@Context final HttpServletRequest req) {

        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        User user = initData.getUser();

        return Response.ok( new ResponseEntityView( fieldTypeFactory.getFieldTypes(user) ) ).build();
    }
}