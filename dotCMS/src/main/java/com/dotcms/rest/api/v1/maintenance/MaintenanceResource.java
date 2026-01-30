package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.DbExporterUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.starter.ExportStarterUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * This REST Endpoint exposes all the different features displayed in the <b>Maintenance</b> portlet
 * inside the dotCMS back-end.
 *
 * @author Will Ezell
 * @since Oct 21st, 2020
 */
@Path("/v1/maintenance")
@SuppressWarnings("serial")
public class MaintenanceResource implements Serializable {

    private final WebResource webResource;

    protected static final Lazy<Boolean> ALLOW_DOTCMS_SHUTDOWN_FROM_CONSOLE =
            Lazy.of(() -> Config.getBooleanProperty("ALLOW_DOTCMS_SHUTDOWN_FROM_CONSOLE", true));

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
     * This method is meant to shut down the current DotCMS instance.
     * It will pass the control to catalina.sh (Tomcat) script to deal with any exit code.
     * 
     * @param request http request
     * @param response http response
     * @return string response
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
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(Portlet.MAINTENANCE)
                .init();

        Logger.info(this.getClass(), String.format("User '%s' is shutting down dotCMS!", initData.getUser()));
        SecurityLogger.logInfo(
                this.getClass(),
                String.format("User '%s' is shutting down dotCMS from ip: %s", initData.getUser(), request.getRemoteAddr()));

        if (!ALLOW_DOTCMS_SHUTDOWN_FROM_CONSOLE.get()) {
            return Response.status(Status.FORBIDDEN).build();
        }

        DotConcurrentFactory.getInstance()
                .getSubmitter()
                .submit(
                        () -> Runtime.getRuntime().exit(0),
                        5,
                        TimeUnit.SECONDS
                );

        return Response.ok(new ResponseEntityView<>("Shutdown")).build();
    }

    /**
     * This method is meant to shut down the current DotCMS instance.
     * It will pass the control to catalina.sh (Tomcat) script to deal with any exit code.
     * 
     * @param request http request
     * @param response http response
     * @return string response
     */
    @DELETE
    @Path("/_shutdownCluster")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response shutdownCluster(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   @DefaultValue("60") @QueryParam("rollingDelay") int rollingDelay) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(Portlet.MAINTENANCE)
                .init();
        final String statusMsg = String.format("User '%s' is shutting down dotCMS Cluster with a rolling delay of %s"
                , initData.getUser(), rollingDelay);
        Logger.info(this.getClass(), statusMsg);
        SecurityLogger.logInfo(this.getClass(), statusMsg);
        if (!ALLOW_DOTCMS_SHUTDOWN_FROM_CONSOLE.get()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        ClusterManagementTopic.getInstance().restartCluster(rollingDelay);
        return Response.ok(new ResponseEntityView<>("Shutdown")).build();
    }
    
    /**
     * This method attempts to send resolved log file using an octet stream http response.
     *
     * @param request  http request
     * @param response http response
     * @param fileName name to give to file
     * @return octet stream response with file contents
     * @throws IOException
     */
    @Path("/_downloadLog/{fileName:.+}")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadLogFile(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("fileName") final String fileName)
            throws IOException {

        assertBackendUser(request, response);

        String tailLogFolder = Config
                .getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
        if (!tailLogFolder.endsWith(File.separator)) {
            tailLogFolder = tailLogFolder + File.separator;
        }

        //This should prevent any attack
        if(!FileUtil.isValidFilePath(fileName)){
            throw new BadRequestException("Requested LogFile: " + fileName + " is not valid.");
        }

        final File logFile = new File(FileUtil.getAbsolutlePath(tailLogFolder + fileName));
        if(!logFile.exists()){
            throw new DoesNotExistException("Requested LogFile: " + logFile.getCanonicalPath() + " does not exist.");
        }

        Logger.info(this.getClass(), "Requested logFile: " + logFile.getCanonicalPath());
        return this.buildFileResponse(response, logFile, fileName);
    }

    /**
     * Returns a text/plain response flag telling whether the pg_dump binary is available (and callable) or not.
     *
     * @param request http request
     * @param response http response
     * @return a text/plain boolean response
     */
    @Path("/_pgDumpAvailable")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.TEXT_PLAIN})
    public final Response isPgDumpAvailable(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response) {
        assertBackendUser(request, response);
        return Response.ok(DbExporterUtil.isPgDumpAvailable().isPresent(), MediaType.TEXT_PLAIN).build();
    }

    /**
     * This method attempts to send resolved DB dump file using an octet stream http response.
     *
     * @param request  http request
     * @param response http response
     * @return octet stream response with file contents
     */
    @Path("/_downloadDb")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadDb(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response) {
        final User user = assertBackendUser(request, response).getUser();
        
        final String hostName = Try.of(()-> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname()).getOrElse("dotcms");

        final SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        final String fileName = StringUtils.sanitizeFileName(hostName)  + "_db_" + dateToString.format(new Date()) + ".sql.gz";

        SecurityLogger.logInfo(this.getClass(), "User : " + user.getEmailAddress() + " downloading database");
        return this.buildFileResponse(response, new PGDumpStreamingOutput(), fileName);
    }
    
    public static class PGDumpStreamingOutput implements StreamingOutput {

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {

            synchronized (PGDumpStreamingOutput.class) {
                try (InputStream input = DbExporterUtil.exportSql()) {
                    IOUtils.copy(input, output);

                } catch (Exception e) {
                    throw new DotRuntimeException(e);
                }
            }

        }
    }

    /**
     * Provides a compressed file with all the assets living in the current dotCMS instance.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param response The current instance of the {@link HttpServletResponse}.
     * @param oldAssets If the resulting file must have absolutely all versions of all assets, set this to {@code true}.
     *
     * @return The {@link StreamingOutput} with the compressed file.
     */
    @Path("/_downloadAssets")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadAssets(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         @DefaultValue("true") @QueryParam("oldAssets") boolean oldAssets) {
        final User user = Try.of(() -> this.assertBackendUser(request, response).getUser()).get();
        final ExportStarterUtil exportStarterUtil = new ExportStarterUtil();
        final String zipName = exportStarterUtil.resolveAssetsFileName();
        Logger.info(this, String.format("User '%s' is generating compressed Assets file '%s' with [ oldAssets = %s]", user.getUserId(),
                zipName, oldAssets));
        final StreamingOutput stream = output -> {

            exportStarterUtil.streamCompressedAssets(output, oldAssets);
            output.flush();
            output.close();
            Logger.info(this, String.format("Compressed Assets file '%s' has been generated successfully!", zipName));

        };
        Logger.debug(this, "Returning StreamingOutput response for compressed asset data");
        return this.buildFileResponse(response, stream, zipName);
    }

    /**
     * This method attempts to download a zip file containing the starter data only.
     *
     * @param request  http request
     * @param response http response
     * @return octet stream response with octet stream
     */
    @Path("/_downloadStarter")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadStarter(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response) {
        return downloadStarter(request, response, false, true);
    }

    /**
     * This method attempts to download a zip file containing the starter with both data and assets.
     *
     * @param request  http request
     * @param response http response
     * @return octet stream response with octet stream
     */
    @Path("/_downloadStarterWithAssets")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadStarterWithAssets(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @DefaultValue("true") @QueryParam("oldAssets") boolean oldAssets) {
        return downloadStarter(request, response, true, oldAssets);
    }

    /**
     * Generates and download a ZIP file with all the data structures and their respective records that are required to
     * create a Starter Site in dotCMS.
     *
     * @param request       The current instance of the {@link HttpServletRequest}.
     * @param response      The current instance of the {@link HttpServletResponse}.
     * @param includeAssets If the generated Starter must include all assets as well, set this to {@code true}.
     * @param oldAssets     If the resulting file must have absolutely all versions of all assets, set this to {@code true}.
     *
     * @return The streamed Starter ZIP file.
     */
    private Response downloadStarter(final HttpServletRequest request, final HttpServletResponse response,
                                     final boolean includeAssets, final boolean oldAssets) {
        final User user = Try.of(() -> this.assertBackendUser(request, response).getUser()).get();
        final ExportStarterUtil exportStarterUtil = new ExportStarterUtil();
        final String zipName = exportStarterUtil.resolveStarterFileName();
        Logger.info(this, String.format("User '%s' is generating compressed Starter file '%s' with [ includeAssets = %s ] [ oldAssets = %s ]", user.getUserId(),
                zipName, includeAssets, oldAssets));

        final StreamingOutput stream = output -> {

            exportStarterUtil.streamCompressedStarter(output, includeAssets, oldAssets);
            output.flush();
            output.close();
            Logger.info(this, String.format("Compressed Starter file '%s' has been generated successfully!", zipName));

        };
        Logger.debug(this, "Returning StreamingOutput response for compressed starter data");
        return this.buildFileResponse(response, stream, zipName);
    }

    /**
     * Verifies that calling user is a backend user required to access the Maintenance portlet.
     *
     * @param request http request
     * @param response http response
     * @return {@link InitDataObject} instance associated to defined criteria.
     */
    private InitDataObject assertBackendUser(HttpServletRequest request, HttpServletResponse response) {
        return new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requireAdmin(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(Portlet.MAINTENANCE)
                .init();
    }

    /**
     * Builds a file response with the provided information. The {@code entity} parameter can be an actual File, or the
     * Streaming Output of it.
     *
     * @param response The current {@link HttpServletResponse} instance.
     * @param entity   The Entity being sent back as the response.
     * @param fileName The name of the file in the response.
     *
     * @return The {@link Response} object.
     */
    private Response buildFileResponse(final HttpServletResponse response, final Object entity, final String fileName) {
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        return Response.ok(entity).build();
    }

}
