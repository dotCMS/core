package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.listeners.SessionMonitor;
import com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DbExporterUtil;
import com.dotcms.util.SizeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.starter.ExportStarterUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.rest.ResponseEntityJobStatusView;

/**
 * This REST Endpoint exposes all the different features displayed in the <b>Maintenance</b> portlet
 * inside the dotCMS back-end.
 *
 * @author Will Ezell
 * @since Oct 21st, 2020
 */
@Path("/v1/maintenance")
@Tag(name = "Maintenance", description = "System maintenance and administration operations")
@SuppressWarnings("serial")
public class MaintenanceResource implements Serializable {

    private final WebResource webResource;

    protected static final Lazy<Boolean> ALLOW_DOTCMS_SHUTDOWN_FROM_CONSOLE =
            Lazy.of(() -> Config.getBooleanProperty("ALLOW_DOTCMS_SHUTDOWN_FROM_CONSOLE", true));

    /**
     * Resolved lazily via CDI the first time a fix/clean-assets endpoint is invoked. We avoid
     * constructor injection so the no-arg and {@code @VisibleForTesting} constructors used by
     * Jersey and existing integration tests keep working unchanged.
     */
    private volatile MaintenanceJobHelper jobHelper;

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
        SecurityLogger.logInfo(this.getClass(), "Requested logFile: " + logFile.getCanonicalPath());

        return this.buildFileResponse(response, logFile, fileName);
    }

    /**
     * Downloads the requested log file from <b>all</b> servers in the cluster and
     * returns them as a single ZIP archive. Each entry in the ZIP is prefixed
     * with the originating server's identifier so that logs from different nodes
     * can be easily distinguished.
     * <p>
     * The local server's log is always included. For every other alive server in
     * the cluster, this method creates a short-lived API token and calls the
     * peer's {@code _downloadLog} endpoint over HTTP using the Java native
     * {@link java.net.http.HttpClient}.
     * <p>
     * If a peer server is unreachable or returns an error, the ZIP will still
     * contain all successfully retrieved logs plus an {@code _errors.txt} entry
     * describing any failures.
     *
     * @param request  http request
     * @param response http response
     * @param fileName name of the log file to download
     * @return ZIP octet stream containing log files from all cluster nodes
     */
    @Path("/_downloadClusterLog/{fileName:.+}")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadClusterLogFile(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("fileName") final String fileName) throws IOException {

        final InitDataObject initData = assertBackendUser(request, response);
        final User user = initData.getUser();

        // Validate the file name
        if (!FileUtil.isValidFilePath(fileName)) {
            throw new BadRequestException("Requested LogFile: " + fileName + " is not valid.");
        }

        // Resolve local log file
        String tailLogFolder = Config
                .getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
        if (!tailLogFolder.endsWith(File.separator)) {
            tailLogFolder = tailLogFolder + File.separator;
        }
        final File localLogFile = new File(FileUtil.getAbsolutlePath(tailLogFolder + fileName));
        if (!localLogFile.exists()) {
            throw new DoesNotExistException(
                    "Requested LogFile: " + localLogFile.getCanonicalPath() + " does not exist.");
        }

        // Discover peer servers
        final ServerAPI serverAPI = APILocator.getServerAPI();
        final String myServerId = serverAPI.readServerId();
        final Server myServer = Try.of(serverAPI::getCurrentServer).getOrNull();
        final List<Server> peerServers = Try.of(() ->
                serverAPI.getAliveServers(List.of(myServerId))
        ).getOrElse(List::of);

        SecurityLogger.logInfo(this.getClass(), "Cluster log download requested by user " + user.getUserId()
                + " for file '" + fileName + "'. Found " + peerServers.size() + " peer server(s).");

        // Create a short-lived API token for authenticating against peer servers
        final ApiTokenAPI tokenApi = APILocator.getApiTokenAPI();
        ApiToken token = null;
        String jwt = null;
        try {
            if (!peerServers.isEmpty()) {
                try {
                    final int tokenTtlSeconds = Config.getIntProperty(
                            "CLUSTER_LOG_DOWNLOAD_TOKEN_TTL_SECONDS", 600);
                    token = ApiToken.builder()
                            .withUser(user)
                            .withExpires(Date.from(
                                    Instant.now().plus(tokenTtlSeconds, ChronoUnit.SECONDS)))
                            .withIssueDate(new Date())
                            .withRequestingUserId(user.getUserId())
                            .withRequestingIp(request.getRemoteAddr())
                            .withClaims(Map.of("label", "cluster-log-download"))
                            .build();
                    token = tokenApi.persistApiToken(token, user);
                    jwt = tokenApi.getJWT(token, user);
                } catch (Exception e) {
                    Logger.error(this, "Failed to create API token for cluster log download: "
                            + e.getMessage(), e);
                    throw new DotRuntimeException(
                            "Unable to create authentication token for cluster communication", e);
                }
            }


            final int localPort = request.getLocalPort();
            final ClusterLogCollector collector = new ClusterLogCollector(
                    peerServers, fileName, jwt, localPort, localLogFile, myServer);

            final String zipFileName = fileName + "_cluster.zip";
            return this.buildFileResponse(response, collector.collect(), zipFileName);
        } finally {
            // Revoke the short-lived token
            if (token != null) {
                final ApiToken tokenToRevoke = token;
                Try.run(() -> tokenApi.revokeToken(tokenToRevoke, user));
            }
        }
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
                final long startTime = System.currentTimeMillis();
                long bytesWritten = 0;
                
                try (InputStream input = DbExporterUtil.exportSql()) {
                    Logger.info(this.getClass(), "Starting database dump stream to client...");
                    bytesWritten = IOUtils.copyLarge(input, output);
                    
                    final long durationMs = System.currentTimeMillis() - startTime;
                    final String sizeFormatted = ConversionUtils.toHumanReadableByteSize(bytesWritten);
                    
                    Logger.info(this.getClass(), "=== DATABASE DUMP STREAM COMPLETED ===");
                    Logger.info(this.getClass(), "Bytes streamed: " + sizeFormatted + " (" + bytesWritten + " bytes)");
                    Logger.info(this.getClass(), "Duration: " + DateUtil.humanReadableFormat(Duration.of(durationMs, ChronoUnit.MILLIS)));
                    Logger.info(this.getClass(), "==========================================");

                } catch (Exception e) {
                    Logger.error(this.getClass(), "Database dump streaming failed after " + 
                               (System.currentTimeMillis() - startTime) + "ms, " + bytesWritten + " bytes written", e);
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
     * @param maxSize  The maximum size of the assets to include in the ZIP file. If the assets exceed this size, they will not be included.
     * @return The {@link StreamingOutput} with the compressed file.
     */
    @Path("/_downloadAssets")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadAssets(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         @DefaultValue("true") @QueryParam("oldAssets") boolean oldAssets,
                                         @QueryParam("maxSize") String maxSize) {



        final long maxFileSize = SizeUtil.convertToBytes(maxSize);
        final User user = Try.of(() -> this.assertBackendUser(request, response).getUser()).get();
        final ExportStarterUtil exportStarterUtil = new ExportStarterUtil();
        final String zipName = exportStarterUtil.resolveAssetsFileName();
        Logger.info(this, String.format("User '%s' is generating compressed Assets file '%s' with [ oldAssets = %s]", user.getUserId(),
                zipName, oldAssets));
        final StreamingOutput stream = output -> {

            exportStarterUtil.streamCompressedAssets(output, oldAssets, maxFileSize);
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
     *  @param maxSize  The maximum size of the assets to include in the ZIP file. If the assets exceed this size, they will not be included.
     * @return octet stream response with octet stream
     */
    @Path("/_downloadStarter")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response downloadStarter(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response,
                                            @QueryParam("maxSize") String maxSize) {
        return downloadStarter(request, response, false, true, maxSize);
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
                                                    @DefaultValue("true") @QueryParam("oldAssets") boolean oldAssets,
                                                    @QueryParam("maxSize") String maxSize) {
        return downloadStarter(request, response, true, oldAssets, maxSize);
    }

    /**
     * Generates and download a ZIP file with all the data structures and their respective records that are required to
     * create a Starter Site in dotCMS.
     *
     * @param request       The current instance of the {@link HttpServletRequest}.
     * @param response      The current instance of the {@link HttpServletResponse}.
     * @param includeAssets If the generated Starter must include all assets as well, set this to {@code true}.
     * @param oldAssets     If the resulting file must have absolutely all versions of all assets, set this to {@code true}.
     * @param maxSize  The maximum size of the assets to include in the ZIP file. If the assets exceed this size, they will not be included.
     *
     * @return The streamed Starter ZIP file.
     */
    private Response downloadStarter(final HttpServletRequest request, final HttpServletResponse response,
                                     final boolean includeAssets, final boolean oldAssets, final String maxSize) {

        final long maxFileSize = SizeUtil.convertToBytes(maxSize);
        final User user = Try.of(() -> this.assertBackendUser(request, response).getUser()).get();
        final ExportStarterUtil exportStarterUtil = new ExportStarterUtil();
        final String zipName = exportStarterUtil.resolveStarterFileName();
        Logger.info(this, String.format("User '%s' is generating compressed Starter file '%s' with [ includeAssets = %s ] [ oldAssets = %s ]", user.getUserId(),
                zipName, includeAssets, oldAssets));

        final StreamingOutput stream = output -> {

            exportStarterUtil.streamCompressedStarter(output, includeAssets, oldAssets, maxFileSize);
            output.flush();
            output.close();
            Logger.info(this, String.format("Compressed Starter file '%s' has been generated successfully!", zipName));

        };
        Logger.debug(this, "Returning StreamingOutput response for compressed starter data");
        return this.buildFileResponse(response, stream, zipName);
    }

    // -------------------------------------------------------------------------
    //  Maintenance Tools endpoints
    // -------------------------------------------------------------------------

    /**
     * Performs a database-wide find/replace across text content in working and live versions of
     * contentlets, containers, templates, fields, and links. This is a dangerous, irreversible
     * operation that should only be used by CMS Administrators.
     *
     * @param request  The current {@link HttpServletRequest}
     * @param response The current {@link HttpServletResponse}
     * @param form     The search and replace parameters
     * @return The operation result indicating success and whether errors occurred
     */
    @Operation(
            summary = "Database-wide search and replace",
            description = "Performs a find/replace across text content in contentlets, containers, "
                    + "templates, fields, and links. Only affects working/live versions. "
                    + "This is a dangerous, irreversible operation. "
                    + "Returns 200 with hasErrors=true if some tables failed — check the response body."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Search and replace completed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySearchAndReplaceResultView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - searchString is empty or missing",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/_searchAndReplace")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public final ResponseEntitySearchAndReplaceResultView searchAndReplace(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Search and replace parameters",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SearchAndReplaceForm.class))
            )
            final SearchAndReplaceForm form) {

        final User user = assertBackendUser(request, response).getUser();

        if (form == null) {
            throw new BadRequestException("Request body is required");
        }

        SecurityLogger.logInfo(this.getClass(),
                String.format("User '%s' executing search and replace from ip: %s",
                        user.getUserId(), request.getRemoteAddr()));

        Logger.info(this, String.format("User '%s' starting database search and replace",
                user.getUserId()));

        final boolean hasErrors = MaintenanceUtil.DBSearchAndReplace(
                form.getSearchString(), form.getReplaceString());

        MaintenanceUtil.flushCache();

        return new ResponseEntitySearchAndReplaceResultView(
                SearchAndReplaceResultView.builder()
                        .success(!hasErrors)
                        .hasErrors(hasErrors)
                        .build());
    }

    /**
     * Deletes all versions of versionable objects older than the specified date. Affects
     * contentlets, containers, templates, links, and workflow history. Iterates in 30-day
     * chunks and flushes all caches when done. Can take minutes on large datasets.
     *
     * @param request  The current {@link HttpServletRequest}
     * @param response The current {@link HttpServletResponse}
     * @param dateStr  Date in yyyy-MM-dd ISO format. All versions older than this date are deleted.
     * @return The operation result with the count of deleted versions
     */
    @Operation(
            summary = "Drop old asset versions",
            description = "Deletes all versions of versionable objects (contentlets, containers, "
                    + "templates, links, workflow history) older than the specified date. "
                    + "Can take minutes on large datasets."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Old versions deleted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityDropOldVersionsResultView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - missing or invalid date format",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/_oldVersions")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final ResponseEntityDropOldVersionsResultView dropOldVersions(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(description = "Cutoff date in yyyy-MM-dd format. Versions older than this are deleted.",
                    required = true, example = "2025-06-15")
            @QueryParam("date") final String dateStr) {

        final User user = assertBackendUser(request, response).getUser();

        if (!UtilMethods.isSet(dateStr)) {
            throw new BadRequestException("date query parameter is required (format: yyyy-MM-dd)");
        }

        final Date assetsOlderThan;
        try {
            final java.time.LocalDate localDate = java.time.LocalDate.parse(dateStr);
            assetsOlderThan = Date.from(
                    localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        } catch (final java.time.format.DateTimeParseException e) {
            throw new BadRequestException(
                    "Invalid date format. Expected yyyy-MM-dd, got: " + dateStr);
        }

        SecurityLogger.logInfo(this.getClass(),
                String.format("User '%s' dropping old asset versions before %s from ip: %s",
                        user.getUserId(), dateStr, request.getRemoteAddr()));

        Logger.info(this, String.format("User '%s' dropping asset versions older than %s",
                user.getUserId(), dateStr));

        final int deleted = CMSMaintenanceFactory.deleteOldAssetVersions(assetsOlderThan);

        if (deleted < 0) {
            throw new DotRuntimeException(
                    "Failed to delete old asset versions before " + dateStr
                            + " — check server logs for details");
        }

        return new ResponseEntityDropOldVersionsResultView(
                DropOldVersionsResultView.builder()
                        .deletedCount(deleted)
                        .success(true)
                        .build());
    }

    /**
     * Deletes all records from the pushed assets tracking table. Clears push publishing history,
     * making all assets appear as never pushed to any endpoint. Used when resetting push
     * publishing state.
     *
     * @param request  The current {@link HttpServletRequest}
     * @param response The current {@link HttpServletResponse}
     * @return A success message
     */
    @Operation(
            summary = "Delete all pushed assets records",
            description = "Deletes ALL records from the pushed assets tracking table. "
                    + "Clears push publishing history, making all assets appear as "
                    + "\"never pushed\" to all endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Pushed assets deleted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/_pushedAssets")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final ResponseEntityStringView deletePushedAssets(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        final User user = assertBackendUser(request, response).getUser();

        SecurityLogger.logInfo(this.getClass(),
                String.format("User '%s' deleting all pushed assets from ip: %s",
                        user.getUserId(), request.getRemoteAddr()));

        Logger.info(this, String.format("User '%s' deleting all pushed assets records",
                user.getUserId()));

        Try.run(() -> APILocator.getPushedAssetsAPI().deleteAllPushedAssets())
                .getOrElseThrow(e -> new DotRuntimeException(
                        "Failed to delete pushed assets: " + e.getMessage(), e));

        return new ResponseEntityStringView("success");
    }

    // -------------------------------------------------------------------------
    //  Fix Assets & Clean Assets endpoints — backed by JobQueueManagerAPI
    // -------------------------------------------------------------------------

    /**
     * Enqueues a fix-assets job. Runs all registered FixTask classes asynchronously on the
     * cluster's job queue. Returns immediately with a job id; poll the job status via
     * {@code GET /api/v1/jobs/{jobId}/status} or {@link #getLatestFixAssetsJob}.
     */
    @Operation(
            summary = "Request a fix-assets job",
            description = "Enqueues a fix-assets inconsistencies job on the cluster job queue. "
                    + "Returns immediately with {jobId, statusUrl}. Rejects with 409 Conflict if "
                    + "a fix-assets job is already pending or running anywhere in the cluster."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Job enqueued",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityJobStatusView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409",
                    description = "Conflict - a fix-assets job is already running",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/assets/_fix")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityJobStatusView requestFixAssetsJob(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        final User user = assertBackendUser(request, response).getUser();
        return new ResponseEntityJobStatusView(
                jobHelper().createFixAssetsJob(user, request));
    }

    /**
     * Returns the most recent fix-assets job — the currently active one if any, otherwise the
     * most recently completed. Intended for "page reload" or "open in a second tab" scenarios
     * where the client has lost the original job id.
     */
    @Operation(
            summary = "Get latest fix-assets job",
            description = "Returns the most recent fix-assets job (active, or most recently "
                    + "completed). Returns null entity if no fix-assets job has ever run."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Latest job status",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "ResponseEntityView wrapping the latest Job "
                                            + "(id, state, progress, result) or null if none exists"))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/assets/_fix")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityView<Job> getLatestFixAssetsJob(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        assertBackendUser(request, response);
        return new ResponseEntityView<>(
                jobHelper().getLatestJob(MaintenanceJobHelper.FIX_ASSETS_QUEUE));
    }

    // -------------------------------------------------------------------------
    //  Bulk delete contentlets endpoint
    // -------------------------------------------------------------------------

    /**
     * Takes a list of contentlet identifiers, retrieves all language siblings for each, and
     * permanently destroys them (bypasses trash). Returns the count of deleted contentlets
     * and a list of any identifiers that failed.
     */
    @Operation(
            summary = "Bulk delete contentlets by identifier",
            description = "Permanently destroys contentlets and all their language siblings. "
                    + "Bypasses trash. Each contentlet is destroyed independently — "
                    + "one failure does not block others."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Bulk deletion completed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityDeleteContentletsResultView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - missing or empty identifiers",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/_contentlets")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityDeleteContentletsResultView deleteContentlets(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "List of contentlet identifiers to permanently destroy",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DeleteContentletsForm.class))
            )
            final DeleteContentletsForm form) {

        final User user = assertBackendUser(request, response).getUser();

        if (form == null) {
            throw new BadRequestException("Request body is required");
        }
        form.checkValid();

        final List<String> identifiers = form.getIdentifiers();

        SecurityLogger.logInfo(this.getClass(),
                String.format("User '%s' destroying %d contentlet identifier(s) from ip: %s",
                        user.getUserId(), identifiers.size(), request.getRemoteAddr()));

        Logger.info(this, String.format("User '%s' starting bulk destroy of %d contentlet identifier(s)",
                user.getUserId(), identifiers.size()));

        final ContentletAPI conAPI = APILocator.getContentletAPI();
        final List<Contentlet> contentlets = new ArrayList<>();

        for (final String id : identifiers) {
            final String trimmedId = id.trim();
            if (UtilMethods.isSet(trimmedId)) {
                try {
                    contentlets.addAll(conAPI.getSiblings(trimmedId));
                } catch (final Exception e) {
                    Logger.warn(this, String.format("Failed to get siblings for identifier '%s': %s",
                            trimmedId, e.getMessage()));
                }
            }
        }

        int deleted = 0;
        final List<String> errors = new ArrayList<>();

        for (final Contentlet contentlet : contentlets) {
            try {
                if (conAPI.destroy(contentlet, user, false)) {
                    deleted++;
                } else {
                    errors.add(contentlet.getIdentifier());
                }
            } catch (final Exception e) {
                errors.add(contentlet.getIdentifier());
                Logger.warn(this, String.format("Failed to destroy contentlet '%s': %s",
                        contentlet.getIdentifier(), e.getMessage()));
            }
        }

        return new ResponseEntityDeleteContentletsResultView(
                DeleteContentletsResultView.builder()
                        .deleted(deleted)
                        .errors(errors)
                        .build());
    }

    /**
     * Enqueues a clean-assets job. The job walks the assets directory and deletes orphan
     * binary folders whose contentlet inode is no longer in the database. Returns immediately
     * with a job id; poll the job status via {@code GET /api/v1/jobs/{jobId}/status} or
     * {@link #getLatestCleanAssetsJob}.
     */
    @Operation(
            summary = "Request a clean-assets job",
            description = "Enqueues a clean orphan assets job on the cluster job queue. Returns "
                    + "immediately with {jobId, statusUrl}. Rejects with 409 Conflict if a "
                    + "clean-assets job is already pending or running anywhere in the cluster."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Job enqueued",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityJobStatusView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409",
                    description = "Conflict - a clean-assets job is already running",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/assets/_clean")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityJobStatusView requestCleanAssetsJob(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        final User user = assertBackendUser(request, response).getUser();
        return new ResponseEntityJobStatusView(
                jobHelper().createCleanAssetsJob(user, request));
    }

    /**
     * Returns the most recent clean-assets job — active if any, otherwise most recently
     * completed. Intended for page-reload / second-tab scenarios.
     */
    @Operation(
            summary = "Get latest clean-assets job",
            description = "Returns the most recent clean-assets job (active, or most recently "
                    + "completed). Returns null entity if no clean-assets job has ever run."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Latest job status",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "ResponseEntityView wrapping the latest Job "
                                            + "(id, state, progress, result) or null if none exists"))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/assets/_clean")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityView<Job> getLatestCleanAssetsJob(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        assertBackendUser(request, response);
        return new ResponseEntityView<>(
                jobHelper().getLatestJob(MaintenanceJobHelper.CLEAN_ASSETS_QUEUE));
    }

    // -------------------------------------------------------------------------
    //  Session management endpoints — back the Logged Users tab
    // -------------------------------------------------------------------------

    /**
     * HTTP session attribute that holds the CSRF token issued by
     * {@link #listSessions} and required by {@link #killSession}.
     */
    @VisibleForTesting
    static final String CSRF_TOKEN_ATTRIBUTE = "maintenanceSessionCsrf";

    /**
     * HTTP session attribute that holds the {@link Instant} at which the
     * {@link #CSRF_TOKEN_ATTRIBUTE} was issued. Used to enforce the 15-minute expiry.
     */
    @VisibleForTesting
    static final String CSRF_TOKEN_TIMESTAMP_ATTRIBUTE = "maintenanceSessionCsrfTimestamp";

    /**
     * Lifetime of a CSRF token issued by {@link #listSessions}. After this window
     * a subsequent call to {@link #killSession} will return 403 and the client
     * must call {@link #listSessions} again to refresh the token.
     */
    private static final Duration CSRF_TOKEN_EXPIRY = Duration.ofMinutes(15);

    /**
     * Lists all active HTTP sessions tracked by {@link SessionMonitor}. Issues a fresh
     * CSRF token, stores it in the caller's HTTP session, and uses it to HMAC each
     * real session id before returning. Real session ids are never sent to the client.
     *
     * @param request  The current {@link HttpServletRequest}
     * @param response The current {@link HttpServletResponse}
     * @return All active sessions with HMAC-obfuscated tokens.
     */
    @Operation(
            summary = "List active HTTP sessions",
            description = "Returns every active HTTP session tracked by SessionMonitor. "
                    + "Real session ids are never exposed; each entry instead carries a "
                    + "short HMAC-derived token that must be passed back to "
                    + "DELETE /v1/maintenance/_sessions/{token} to invalidate the session. "
                    + "The CSRF secret used to derive these tokens is stored in the caller's "
                    + "HTTP session and is valid for 15 minutes — re-call this endpoint to refresh."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of active sessions",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySessionListView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/_sessions")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final ResponseEntitySessionListView listSessions(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        assertBackendUser(request, response);

        final HttpSession callingSession = request.getSession();
        final String csrfToken = UUID.randomUUID().toString();
        callingSession.setAttribute(CSRF_TOKEN_ATTRIBUTE, csrfToken);
        callingSession.setAttribute(CSRF_TOKEN_TIMESTAMP_ATTRIBUTE, Instant.now());

        final SessionMonitor sessionMonitor = new SessionMonitor();
        final List<SessionView> sessions = new ArrayList<>();

        for (final HttpSession session : sessionMonitor.getUserSessions().values()) {
            User sessionUser = Try.of(() -> PortalUtil.getUser(session)).getOrNull();
            if (sessionUser == null) {
                sessionUser = Try.of(() -> APILocator.getUserAPI().getAnonymousUser())
                        .getOrElseThrow(e -> new DotRuntimeException(
                                "Unable to resolve anonymous user", e));
            }

            sessions.add(SessionView.builder()
                    .token(SessionTokenUtil.obfuscateSessionId(session.getId(), csrfToken))
                    .isCurrent(callingSession.getId().equals(session.getId()))
                    .userId(sessionUser.getUserId())
                    .userEmail(sessionUser.getEmailAddress())
                    .userFullName(sessionUser.getFullName())
                    .address((String) session.getAttribute(SessionMonitor.USER_REMOTE_ADDR))
                    .sessionTime(DateUtil.prettyDateSince(
                            new Date(session.getCreationTime()),
                            PublicCompanyFactory.getDefaultCompany().getLocale()))
                    .build());
        }

        return new ResponseEntitySessionListView(sessions);
    }

    /**
     * Invalidates a single session identified by its HMAC-obfuscated token.
     * <p>
     * Requires a fresh CSRF secret stored in the caller's HTTP session (issued by
     * {@link #listSessions}); the caller cannot invalidate its own session.
     *
     * @param request  The current {@link HttpServletRequest}
     * @param response The current {@link HttpServletResponse}
     * @param token    HMAC-obfuscated session token returned by {@link #listSessions}
     * @return A success message when the session was invalidated.
     */
    @Operation(
            summary = "Invalidate a single session",
            description = "Invalidates the session whose HMAC-obfuscated token is supplied. "
                    + "The caller's own session cannot be invalidated this way. The CSRF secret "
                    + "must have been issued by GET /v1/maintenance/_sessions within the last "
                    + "15 minutes — otherwise this endpoint returns 403 and the client must "
                    + "re-list to refresh."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Session invalidated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - attempting to invalidate caller's own session",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - missing or expired CSRF token, or insufficient role",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "No active session matches the supplied token",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/_sessions/{token}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final ResponseEntityStringView killSession(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(description = "HMAC-obfuscated session token returned by GET /_sessions",
                    required = true)
            @PathParam("token") final String token) {

        final User user = assertBackendUser(request, response).getUser();

        if (!UtilMethods.isSet(token)) {
            throw new BadRequestException("token path parameter is required");
        }

        final HttpSession callingSession = request.getSession();
        final String csrfSecret = getValidCsrfSecret(callingSession);

        final SessionMonitor sessionMonitor = new SessionMonitor();
        for (final HttpSession session : sessionMonitor.getUserSessions().values()) {
            if (!SessionTokenUtil.validateSessionId(session.getId(), csrfSecret, token)) {
                continue;
            }
            if (callingSession.getId().equals(session.getId())) {
                throw new BadRequestException("Cannot invalidate your own session");
            }

            SecurityLogger.logInfo(this.getClass(),
                    String.format("User '%s' invalidating session of user '%s' from ip: %s",
                            user.getUserId(),
                            Try.of(() -> PortalUtil.getUser(session).getUserId())
                                    .getOrElse("unknown"),
                            request.getRemoteAddr()));

            session.setAttribute(SessionMonitor.IGNORE_REMEMBER_ME_ON_INVALIDATION, true);
            session.invalidate();
            return new ResponseEntityStringView("Session invalidated");
        }

        throw new NotFoundException("No active session matches the supplied token");
    }

    /**
     * Invalidates every active HTTP session except the caller's own.
     *
     * @param request  The current {@link HttpServletRequest}
     * @param response The current {@link HttpServletResponse}
     * @return The number of sessions invalidated.
     */
    @Operation(
            summary = "Invalidate all sessions except the caller's",
            description = "Walks every active HTTP session tracked by SessionMonitor, "
                    + "skips the caller's own session, and invalidates the rest. "
                    + "Returns the count of invalidated sessions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Sessions invalidated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityKillSessionsResultView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/_sessions")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final ResponseEntityKillSessionsResultView killAllSessions(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        final User user = assertBackendUser(request, response).getUser();
        final HttpSession callingSession = request.getSession();
        final SessionMonitor sessionMonitor = new SessionMonitor();

        SecurityLogger.logInfo(this.getClass(),
                String.format("User '%s' is invalidating all other sessions from ip: %s",
                        user.getUserId(), request.getRemoteAddr()));

        int killed = 0;
        for (final HttpSession session : sessionMonitor.getUserSessions().values()) {
            if (callingSession.getId().equals(session.getId())) {
                continue;
            }
            session.setAttribute(SessionMonitor.IGNORE_REMEMBER_ME_ON_INVALIDATION, true);
            session.invalidate();
            killed++;
        }

        return new ResponseEntityKillSessionsResultView(
                KillSessionsResultView.builder().killedCount(killed).build());
    }

    /**
     * Returns the unexpired CSRF secret previously stored by {@link #listSessions},
     * or throws {@link ForbiddenException} if it is missing or older than
     * {@link #CSRF_TOKEN_EXPIRY}.
     */
    private static String getValidCsrfSecret(final HttpSession callingSession) {
        final String csrf = (String) callingSession.getAttribute(CSRF_TOKEN_ATTRIBUTE);
        final Object rawTimestamp = callingSession.getAttribute(CSRF_TOKEN_TIMESTAMP_ATTRIBUTE);
        if (csrf == null || !(rawTimestamp instanceof Instant)
                || Instant.now().isAfter(((Instant) rawTimestamp).plus(CSRF_TOKEN_EXPIRY))) {
            throw new ForbiddenException("CSRF token is missing or expired; call GET /_sessions to refresh");
        }
        return csrf;
    }

    /**
     * Lazily resolves the {@link MaintenanceJobHelper} via CDI on first use. Uses the full
     * double-checked locking pattern (volatile field + synchronized inner block) so that at
     * most one CDI bean instantiation occurs under concurrent access.
     */
    private MaintenanceJobHelper jobHelper() {
        MaintenanceJobHelper local = jobHelper;
        if (local == null) {
            synchronized (this) {
                local = jobHelper;
                if (local == null) {
                    jobHelper = local = resolveJobHelperBean();
                }
            }
        }
        return local;
    }

    /**
     * Resolves the {@link MaintenanceJobHelper} CDI bean. Extracted to a protected method so
     * unit tests can override it without requiring a live CDI container.
     */
    @VisibleForTesting
    protected MaintenanceJobHelper resolveJobHelperBean() {
        return CDIUtils.getBean(MaintenanceJobHelper.class).orElseThrow(() ->
                new DotRuntimeException("MaintenanceJobHelper CDI bean not available"));
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
