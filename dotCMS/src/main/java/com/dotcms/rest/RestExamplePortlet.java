package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerCompliant(value = "Legacy & Utility Resources - Example REST endpoint for testing and demonstration purposes", batch = 8)
@Tag(name = "Administration")
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