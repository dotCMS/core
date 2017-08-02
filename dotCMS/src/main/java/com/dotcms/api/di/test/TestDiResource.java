package com.dotcms.api.di.test;

import com.dotcms.api.di.DotBean;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@SuppressWarnings("serial")
@DotBean
@Path("/v1/testresource")
public class TestDiResource {


    private final TestIocService testIocService;
    private final ResponseUtil responseUtil;


    @Inject
    public TestDiResource(final TestIocService testIocService) {
        this (ResponseUtil.INSTANCE, testIocService);
    }

    @VisibleForTesting
    protected TestDiResource(final ResponseUtil responseUtil, final TestIocService testIocService) {
        this.responseUtil = responseUtil;
        this.testIocService = testIocService;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response test(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response) {

        Response res = null;

        try {

            final String msg = this.testIocService.testMe();
            res = (null != msg)?
                    Response.ok(new ResponseEntityView(msg)).build():
                    this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED,
                        request.getLocale(), "", "authentication-failed");
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    } // test



} // E:O:F:AuthenticationResource.
