package com.dotcms.rest.api.v1.theme;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.page.ResponseEntityPaginatedArrayListMapView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.ThemePaginator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ThemeAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides different methods to access information about Themes in dotCMS.
 */
@SwaggerCompliant(value = "Site architecture and template management APIs", batch = 3)
@Path("/v1/themes")
@Tag(name = "Themes")
public class ThemeResource {

    private final PaginationUtil paginationUtil;
    private final WebResource webResource;
    private final ThemeAPI themeAPI;

    public ThemeResource() {
        this(
                new ThemePaginator(),
                APILocator.getHostAPI(),
                APILocator.getFolderAPI(),
                APILocator.getThemeAPI(),
                new WebResource()
        );
    }

    @VisibleForTesting
    ThemeResource(final ThemePaginator themePaginator, final HostAPI hostAPI,
            final FolderAPI folderAPI,
            final ThemeAPI themeAPI, final WebResource webResource) {
        this.webResource = webResource;
        this.paginationUtil = new PaginationUtil(themePaginator);
        this.themeAPI = themeAPI;
    }

    @Operation(
        summary = "Find themes",
        description = "Returns a paginated list of themes that the user has read permissions on"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Themes retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityPaginatedArrayListMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - hostId is required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response findThemes(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Host identifier", required = true) @QueryParam("hostId") final String hostId,
            @Parameter(description = "Page number for pagination") @QueryParam(PaginationUtil.PAGE) final int page,
            @Parameter(description = "Number of items per page") @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("-1") final int perPage,
            @Parameter(description = "Sort direction (ASC, DESC)") @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
            @Parameter(description = "Search parameter for filtering themes") @QueryParam("searchParam") final String searchParam) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user = initData.getUser();

        if (UtilMethods.isNotSet(hostId)) {
            throw new BadRequestException("HostId is Required");
        }

        Logger.debug(this,
                "Getting the themes for the hostId: " + hostId);

        final Map<String, Object> params = new HashMap<>(Map.of(ThemePaginator.HOST_ID_PARAMETER_NAME, hostId));

        if (UtilMethods.isSet(searchParam)) {
            params.put(ThemePaginator.SEARCH_PARAMETER, searchParam);
        }

        return this.paginationUtil.getPage(request, user, null, page, perPage, null,
                OrderDirection.valueOf(direction), params);
    }

    @Operation(
        summary = "Find theme by ID",
        description = "Returns a specific theme by its folder inode identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Theme retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Theme not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/id/{id}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response findThemeById(@Context final HttpServletRequest request,
            final @Context HttpServletResponse response,
            @Parameter(description = "Theme folder inode identifier", required = true) @PathParam("id") final String themeId) throws DotDataException, DotSecurityException {

        Logger.debug(this, "Getting the theme by identifier: " + themeId);

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        return Response
                .ok(new ResponseEntityView<>(themeAPI.findThemeById(themeId, user, false).getMap()))
                .build();
    }
}
