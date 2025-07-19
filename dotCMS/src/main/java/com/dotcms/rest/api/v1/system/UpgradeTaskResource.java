package com.dotcms.rest.api.v1.system;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * This Resource has the ability to run an upgrade task, only for ADMIN logged users
 * @author jsanca
 */
@SwaggerCompliant(value = "System administration and configuration APIs", batch = 4)
@Tag(name = "Maintenance")
@Path("/v1/upgradetask")
public class UpgradeTaskResource {

    /**
     * Post to run an upgrade task.
     * Must be logged backend admin user to ran it
     * If the class can not be upload, 404
     * If the upgrade exists, but does not need to ran, 304
     * If upgrande ran ok, 200
     * @param request
     * @param response
     * @param upgradeTaskForm
     * @return
     */
    @Operation(
        summary = "Run upgrade task",
        description = "Run an upgrade task. Must be logged in as backend admin user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Upgrade task ran successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "304", 
                    description = "Not modified - upgrade task does not need to be executed",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Upgrade task class not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public final Response upgrade(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                      description = "Upgrade task configuration", 
                                      required = true,
                                      content = @Content(schema = @Schema(implementation = UpgradeTaskForm.class))
                                  )
                                  final UpgradeTaskForm upgradeTaskForm) {

        Response res = null;

        final InitDataObject initData = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        try {

            if (!APILocator.getUserAPI().isCMSAdmin(initData.getUser())) {

                throw new ForbiddenException("User must be an admin to run an upgrade task");
            }

            final String upgradeTaskClassName = upgradeTaskForm.getUpgradeTaskClass();

            Logger.info(this, "Running the upgrade task: " + upgradeTaskClassName);

            final StartupTask startupTask     = (StartupTask) ReflectionUtils.newInstance(upgradeTaskClassName);

            if (null != startupTask) {

                if (startupTask.forceRun()) {

                    startupTask.executeUpgrade();
                    HibernateUtil.closeAndCommitTransaction();
                    Logger.info(this, "Ran the upgrade task: " + upgradeTaskClassName);
                    return Response.ok(new ResponseEntityStringView(
                            "Ran the upgrade task: " + upgradeTaskClassName)).build(); // 200
                } else {

                    Logger.info(this, "The the upgrade task: " + upgradeTaskClassName + " does not need to be execute");
                    return Response.notModified().build();
                }
            } else {

                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            Logger.error(this, e.getMessage(), e);
            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        return res;
    }

}
