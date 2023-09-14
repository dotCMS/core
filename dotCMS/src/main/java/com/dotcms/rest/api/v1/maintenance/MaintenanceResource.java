package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.AssetExporterUtil;
import com.dotcms.util.DbExporterUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.*;
import com.liferay.portal.model.Portlet;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.JSONP;


/**
 * Maintenance (portlet) resource.
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

        return Response.ok(new ResponseEntityView("Shutdown")).build();
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
        return Response.ok(new ResponseEntityView("Shutdown")).build();
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
    @Path("/_downloadLog/{fileName}")
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

        final File logFile = new File(FileUtil.getAbsolutlePath(tailLogFolder + fileName));
        if(!logFile.exists()){
            throw new DoesNotExistException("Requested LogFile: " + logFile.getCanonicalPath() + " does not exist.");
        }

        Logger.info(this.getClass(), "Requested logFile: " + logFile.getCanonicalPath());

        response.setHeader( "Content-Disposition", "attachment; filename=" + fileName );
        return Response.ok(logFile, MediaType.APPLICATION_OCTET_STREAM).build();
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
     * @throws IOException
     */
    @Path("/_downloadDb")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadDb(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response) throws IOException {
        User user = assertBackendUser(request, response).getUser();

        final String hostName = Try.of(()-> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname()).getOrElse("dotcms");

        final SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        final String fileName = StringUtils.sanitizeFileName(hostName)  + "_db_" + dateToString.format(new Date()) + ".sql.gz";

        SecurityLogger.logInfo(this.getClass(), "User : " + user.getEmailAddress() + " downloading database");

        response.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        return Response.ok(new PGDumpStreamingOutput()).build();
    }


    public static class PGDumpStreamingOutput implements StreamingOutput {

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {

            synchronized (PGDumpStreamingOutput.class) {
                try (InputStream input = DbExporterUtil.exportSql()) {
                    IOUtils.copy(input, output);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    /**
     * This method attempts to send resolved DB dump file using an octet stream http response.
     *
     * @param request  http request
     * @param response http response
     * @return octet stream response with file contents
     * @throws IOException
     */
    @Path("/_downloadAssets")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadAssets(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response) throws IOException {
        final String assetsFile = AssetExporterUtil.resolveFileName();
        response.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + assetsFile + "\"");

        final User user = Try.of(() -> assertBackendUser(request, response).getUser()).get();
        SecurityLogger.logInfo(AssetExporterUtil.class, "User : " + user.getEmailAddress() + " downloading assets");

        AssetExporterUtil.exportAssets(response.getOutputStream());
        Logger.info(this.getClass(), "Requested assets file: " + assetsFile);

        return Response.ok(assetsFile, MediaType.APPLICATION_OCTET_STREAM).build();
    }

    /**
     * This method attempts to download a zip file containing the starter with assets.
     *
     * @param request  http request
     * @param response http response
     * @return octet stream response with octet stream
     * @throws IOException
     */
    @Path("/_downloadStarter")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadStarter(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response) throws IOException {
        return downloadStarter(request, response, false, false);
    }

    /**
     * This method attempts to download a zip file containing the starter with just data.
     *
     * @param request  http request
     * @param response http response
     * @return octet stream response with octet stream
     * @throws IOException
     */
    @Path("/_downloadStarterWithAssets")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadStarterWithAssets(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response) throws IOException {
        return downloadStarter(request, response, true, false);
    }

    /**
     * This method attempts to download a zip file containing the starter whether it is with just data or assets.
     *
     * @param request http request
     * @param response http response
     * @param withAssets flag telling to include or not assets
     * @param download flag telling to download or to stream
     * @return octet stream response with octet stream
     * @throws IOException
     */
    private Response downloadStarter(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final boolean withAssets,
                                     final boolean download) throws IOException {
        assertBackendUser(request, response);

        final ExportStarterUtil exportStarterUtil = new ExportStarterUtil();
        final Optional<File> starterFile = exportStarterUtil.zipStarter(
                response.getOutputStream(),
                withAssets,
                download);
        if (!starterFile.isPresent()) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        final File file = starterFile.get();
        Logger.info(this.getClass(), "Requested starter file: " + file.getCanonicalPath());

        response.setHeader("Content-type", download ? "application/zip" : MediaType.APPLICATION_OCTET_STREAM);
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());

        return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM).build();
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
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(Portlet.MAINTENANCE)
                .init();
    }

}
