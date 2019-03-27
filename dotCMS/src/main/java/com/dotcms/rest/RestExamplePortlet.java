package com.dotcms.rest;

import javax.servlet.http.HttpServletRequest;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.CacheControl;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

@Path("/restexample")
public class RestExamplePortlet extends BaseRestPortlet {

        @GET
        @Path("/test/{params:.*}")
        @Produces("application/json")
        public Response loadJson(@Context HttpServletRequest request,
                        @PathParam("params") String params) throws DotStateException,
                        DotDataException, DotSecurityException {

                CacheControl cc = new CacheControl();
                cc.setNoCache(true);

                ResponseBuilder builder = Response
                                .ok("{\"test\":\"test\"}", "application/json");
                return builder.cacheControl(cc).build();

        }



}