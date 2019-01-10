package com.dotcms.rest.api.v1.system;

import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.startup.StartupAPI;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

@Path("/v1/startup")
public class StartupResource {

    private final WebResource webResource;
    private final StartupAPI  startupAPI;
    private final UserAPI     userAPI;

    public StartupResource() {

        this(new WebResource(), APILocator.getStartupAPI(), APILocator.getUserAPI());
    }

    @VisibleForTesting
    public StartupResource(final WebResource webResource,
                           final StartupAPI  startupAPI,
                           final UserAPI     userAPI) {

        this.webResource = webResource;
        this.startupAPI  = startupAPI;
        this.userAPI     = userAPI;
    }

    /**
     * Runs an upgrade task if exists, the user must be an admin.
     * @param request {@link HttpServletRequest}
     * @return Response
     */
    @POST()
    @Path("/{startupClass}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response run(@Context final HttpServletRequest request,
                        @PathParam("startupClass") final String startupClassName) {

        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final User           user     = initData.getUser();

        try {

            Logger.debug(this, ()-> "Running the upgrade task: " + startupClassName);

            if (this.userAPI.isCMSAdmin(user)) {

                final Class startupClass = ReflectionUtils.getClassFor(startupClassName);
                if (null != startupClass) {

                    this.startupAPI.runStartup(startupClass);
                } else {

                    throw new DoesNotExistException("The class name: " + startupClassName + ", can not be executed because it does not exists");
                }
            } else {

                throw new DotSecurityException("An upgrade task can be only executed by user with admin role");
            }
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this.getClass(),
                    "Exception on run exception message: " + e
                            .getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return Response.ok(new ResponseEntityView("ok")).build();
    } // run.

    @GET()
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response listTasks (@Context final HttpServletRequest request) {

        this.webResource.init(null, true, request, true, null);
        Response             response = null;

        try {

            Logger.debug(this, ()-> "Getting the upgrade tasks");

            response = Response.ok(new ResponseEntityView(
                    CollectionsUtils.map(
                            "alwaystasks", this.startupAPI.getStartupRunAlwaysTaskClasses(),
                            "onetasks",    this.startupAPI.getStartupRunOnceTaskClasses())))
                    .build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on run exception message: " + e
                            .getMessage(), e);
            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    } // listTask.
}