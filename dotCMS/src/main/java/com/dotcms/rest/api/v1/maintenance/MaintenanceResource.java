package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.JSONP;


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
    @Path("/_shutdown")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response shutdown(@Context final HttpServletRequest request,
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

        DotConcurrentFactory.getInstance()
                .getSubmitter()
                .submit(
                        () -> Runtime.getRuntime().exit(0),
                        5,
                        TimeUnit.SECONDS
                );

        return Response.ok(new ResponseEntityView("Shutdown")).build();
    }

    @Path("/_downloadLog/{fileName}")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadLogFile(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("fileName") final String fileName)
            throws IOException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet("maintenance")
                        .init();

            String tailLogFolder = Config
                    .getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
            if (!tailLogFolder.endsWith(File.separator)) {
                tailLogFolder = tailLogFolder + File.separator;
            }

            final File logFile = new File(FileUtil.getAbsolutlePath(tailLogFolder + fileName));
            if(!logFile.exists()){
                throw new DoesNotExistException("Requested LogFile: " + fileName + " does not exist. Under Path: " + tailLogFolder);
            }

            Logger.info(this.getClass(), "Requested logFile: " + logFile.getCanonicalPath());

            response.setHeader( "Content-Disposition", "attachment; filename=" + fileName );
            return Response.ok(logFile, MediaType.APPLICATION_OCTET_STREAM).build();
    }

}
