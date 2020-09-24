package com.dotcms.rest.api.v1.maintenance;

import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;


@Path("/v1/maintenance")
@SuppressWarnings("serial")
public class MaintenanceResource implements Serializable {

    private final WebResource webResource;

    /**
     * Default class constructor.
     */
    public MaintenanceResource() {
        this(new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    public MaintenanceResource(WebResource webResource) {
        this.webResource = webResource;

    }


    /**
     * This me
     * 
     * @param request
     * @param response
     * @return
     */
    @DELETE
    @Path("/shutdown")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteCustomPortlet(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE).requestAndResponse(request, response)
                        .rejectWhenNoUser(true).requiredPortlet("maintenance").init();


        Logger.info(this.getClass(), "User:" + initData.getUser() + " is shutting down dotCMS!");
        SecurityLogger.logInfo(this.getClass(),
                        "User:" + initData.getUser() + " is shutting down dotCMS from ip:" + request.getRemoteAddr());

        if (!Config.getBooleanProperty("ALLOW_DOTCMS_SHUTDOWN_FROM_CONSOLE", true)) {
            return Response.status(Status.FORBIDDEN).build();
        }

        Runtime.getRuntime().exit(0);
        return Response.ok(new ResponseEntityView("Shutdown")).build();



    }

}
