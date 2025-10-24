package com.dotcms.rest.api.v1.grafana;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardDetail;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardSearchResult;
import com.dotcms.rest.api.v1.grafana.client.dto.GrafanaFolder;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * REST Resource for Grafana integration endpoints.
 *
 * This resource provides HTTP endpoints to interact with Grafana dashboards,
 * folders, and perform connectivity tests. All endpoints require authentication
 * and appropriate permissions.
 *
 * Base path: /api/v1/grafana
 *
 * @author dotCMS
 */
@Path("/v1/grafana")
@Tag(name = "Grafana Integration", description = "Endpoints for interacting with Grafana dashboards and folders")
public class GrafanaResource {

    private final WebResource webResource = new WebResource();

    /**
     * Search for Grafana dashboards based on various criteria.
     *
     * This endpoint allows filtering dashboards by query text, type, starred status,
     * folder IDs, tags, and limiting the number of results returned.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param query Search query string to filter dashboards by title
     * @param type Dashboard type filter (e.g., "dash-db")
     * @param starred Filter by starred status (true/false)
     * @param folderIds Comma-separated list of folder IDs to filter by
     * @param tag Tag to filter dashboards by
     * @param limit Maximum number of results to return
     * @return List of matching dashboard search results
     */
    @Operation(
        summary = "Search Grafana dashboards",
        description = "Search for dashboards in Grafana based on various criteria including query text, type, starred status, folders, and tags."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Dashboard search completed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = GrafanaDashboardSearchResultView.class))),
        @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions to access Grafana",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error or Grafana connection failed",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/dashboards/search")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchDashboards(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Search query string to filter dashboards by title", required = false)
            @QueryParam("query") final String query,
            @Parameter(description = "Dashboard type filter (e.g., 'dash-db')", required = false)
            @QueryParam("type") final String type,
            @Parameter(description = "Filter by starred status", required = false)
            @QueryParam("starred") final Boolean starred,
            @Parameter(description = "Comma-separated list of folder IDs", required = false)
            @QueryParam("folderIds") final String folderIds,
            @Parameter(description = "Tag to filter dashboards by", required = false)
            @QueryParam("tag") final String tag,
            @Parameter(description = "Maximum number of results to return", required = false)
            @QueryParam("limit") final Integer limit) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        try {
            Logger.debug(this, String.format("Searching Grafana dashboards for user %s with query: %s",
                                           user.getUserId(), query));

            // Check if user has permission to access Grafana integration
            checkGrafanaPermissions(user);

            final GrafanaAPI grafanaAPI = APILocator.getGrafanaAPI();
            final List<DashboardSearchResult> results = grafanaAPI.searchDashboards(
                query, type, starred, folderIds, tag, limit);

            Logger.info(this, String.format("Found %d dashboards for user %s",
                                          results.size(), user.getUserId()));

            return Response.ok(new GrafanaDashboardSearchResultView(results)).build();

        } catch (final Exception e) {
            Logger.error(this, String.format("Error searching dashboards for user %s: %s",
                                            user.getUserId(), e.getMessage()), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get detailed information about a specific Grafana dashboard by its UID.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param uid Dashboard unique identifier
     * @return Dashboard detailed information
     */
    @Operation(
        summary = "Get dashboard details by UID",
        description = "Retrieve detailed information about a specific Grafana dashboard using its unique identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Dashboard details retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = GrafanaDashboardDetailView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Invalid dashboard UID provided",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions to access Grafana",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Dashboard not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error or Grafana connection failed",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/dashboards/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getDashboardByUid(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Dashboard unique identifier", required = true)
            @PathParam("uid") final String uid) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        try {
            Logger.debug(this, String.format("Getting dashboard details for UID %s, user %s",
                                           uid, user.getUserId()));

            // Check if user has permission to access Grafana integration
            checkGrafanaPermissions(user);

            final GrafanaAPI grafanaAPI = APILocator.getGrafanaAPI();
            final DashboardDetail dashboard = grafanaAPI.getDashboardByUid(uid);

            Logger.info(this, String.format("Retrieved dashboard %s for user %s",
                                          uid, user.getUserId()));

            return Response.ok(new GrafanaDashboardDetailView(dashboard)).build();

        } catch (final IllegalArgumentException e) {
            Logger.warn(this, String.format("Invalid dashboard UID %s for user %s: %s",
                                          uid, user.getUserId(), e.getMessage()));
            return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        } catch (final Exception e) {
            Logger.error(this, String.format("Error getting dashboard %s for user %s: %s",
                                            uid, user.getUserId(), e.getMessage()), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all Grafana folders with optional limit.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param limit Maximum number of folders to return
     * @return List of Grafana folders
     */
    @Operation(
        summary = "Get Grafana folders",
        description = "Retrieve a list of all Grafana folders with optional limit on the number of results."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Folders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = GrafanaFolderView.class))),
        @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions to access Grafana",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error or Grafana connection failed",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/folders")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getFolders(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Maximum number of folders to return", required = false)
            @QueryParam("limit") final Integer limit) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        try {
            Logger.debug(this, String.format("Getting Grafana folders for user %s with limit: %s",
                                           user.getUserId(), limit));

            // Check if user has permission to access Grafana integration
            checkGrafanaPermissions(user);

            final GrafanaAPI grafanaAPI = APILocator.getGrafanaAPI();
            final List<GrafanaFolder> folders = grafanaAPI.getFolders(limit);

            Logger.info(this, String.format("Retrieved %d folders for user %s",
                                          folders.size(), user.getUserId()));

            return Response.ok(new GrafanaFolderView(folders)).build();

        } catch (final Exception e) {
            Logger.error(this, String.format("Error getting folders for user %s: %s",
                                            user.getUserId(), e.getMessage()), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get a specific Grafana folder by its UID.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param uid Folder unique identifier
     * @return Grafana folder information
     */
    @Operation(
        summary = "Get folder details by UID",
        description = "Retrieve detailed information about a specific Grafana folder using its unique identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Folder details retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = GrafanaFolderSingleView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Invalid folder UID provided",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions to access Grafana",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error or Grafana connection failed",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/folders/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getFolderByUid(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Folder unique identifier", required = true)
            @PathParam("uid") final String uid) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        try {
            Logger.debug(this, String.format("Getting folder details for UID %s, user %s",
                                           uid, user.getUserId()));

            // Check if user has permission to access Grafana integration
            checkGrafanaPermissions(user);

            final GrafanaAPI grafanaAPI = APILocator.getGrafanaAPI();
            final GrafanaFolder folder = grafanaAPI.getFolderByUid(uid);

            Logger.info(this, String.format("Retrieved folder %s for user %s",
                                          uid, user.getUserId()));

            return Response.ok(new GrafanaFolderSingleView(folder)).build();

        } catch (final IllegalArgumentException e) {
            Logger.warn(this, String.format("Invalid folder UID %s for user %s: %s",
                                          uid, user.getUserId(), e.getMessage()));
            return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        } catch (final Exception e) {
            Logger.error(this, String.format("Error getting folder %s for user %s: %s",
                                            uid, user.getUserId(), e.getMessage()), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all dashboards contained in a specific Grafana folder.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param folderUid Folder unique identifier
     * @return List of dashboards in the folder
     */
    @Operation(
        summary = "Get dashboards in folder",
        description = "Retrieve all dashboards contained within a specific Grafana folder."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Folder dashboards retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = GrafanaDashboardSearchResultView.class))),
        @ApiResponse(responseCode = "400",
                    description = "Invalid folder UID provided",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions to access Grafana",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404",
                    description = "Folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error or Grafana connection failed",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/folders/{folderUid}/dashboards")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getDashboardsInFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Folder unique identifier", required = true)
            @PathParam("folderUid") final String folderUid) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        try {
            Logger.debug(this, String.format("Getting dashboards in folder %s for user %s",
                                           folderUid, user.getUserId()));

            // Check if user has permission to access Grafana integration
            checkGrafanaPermissions(user);

            final GrafanaAPI grafanaAPI = APILocator.getGrafanaAPI();
            final List<DashboardSearchResult> dashboards = grafanaAPI.getDashboardsInFolder(folderUid);

            Logger.info(this, String.format("Retrieved %d dashboards from folder %s for user %s",
                                          dashboards.size(), folderUid, user.getUserId()));

            return Response.ok(new GrafanaDashboardSearchResultView(dashboards)).build();

        } catch (final IllegalArgumentException e) {
            Logger.warn(this, String.format("Invalid folder UID %s for user %s: %s",
                                          folderUid, user.getUserId(), e.getMessage()));
            return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        } catch (final Exception e) {
            Logger.error(this, String.format("Error getting dashboards in folder %s for user %s: %s",
                                            folderUid, user.getUserId(), e.getMessage()), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Test connectivity to the Grafana API.
     *
     * This endpoint performs a basic connectivity test to verify if the Grafana
     * integration is properly configured and accessible.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return Boolean indicating if connection is successful
     */
    @Operation(
        summary = "Test Grafana connectivity",
        description = "Test connectivity to the configured Grafana instance to verify integration setup."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                    description = "Connectivity test completed",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions to test Grafana connectivity",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500",
                    description = "Internal server error during connectivity test",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/test-connection")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response testConnection(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();

        try {
            Logger.debug(this, String.format("Testing Grafana connectivity for user %s",
                                           user.getUserId()));

            // Check if user has permission to access Grafana integration
            checkGrafanaPermissions(user);

            final GrafanaAPI grafanaAPI = APILocator.getGrafanaAPI();
            final boolean isConnected = grafanaAPI.testConnection();

            Logger.info(this, String.format("Grafana connectivity test result: %s for user %s",
                                          isConnected, user.getUserId()));

            return Response.ok(new ResponseEntityBooleanView(isConnected)).build();

        } catch (final Exception e) {
            Logger.error(this, String.format("Error testing Grafana connectivity for user %s: %s",
                                            user.getUserId(), e.getMessage()), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check if the user has permissions to access Grafana integration.
     *
     * Currently checks if the user is an admin user. This can be extended
     * to check for specific permissions or roles as needed.
     *
     * @param user The user to check permissions for
     * @throws DotDataException if there's an error checking permissions
     * @throws DotSecurityException if the user doesn't have sufficient permissions
     */
    private void checkGrafanaPermissions(final User user) throws DotDataException, DotSecurityException {
        // Check if user is admin - this can be customized based on requirements
        if (!APILocator.getUserAPI().isCMSAdmin(user)) {
            Logger.warn(this, String.format("User %s attempted to access Grafana endpoints without admin permissions",
                                          user.getUserId()));
            throw new ForbiddenException("Grafana integration requires administrator permissions");
        }
    }
}