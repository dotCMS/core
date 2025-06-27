package com.dotcms.rest.api.v1.theme;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
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
@Path("/v1/themes")
@Tag(name = "Templates", description = "Template design and management")
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

    /**
     * Returns all themes
     *
     * @param hostId Optional param, when it is not specified the default host is used
     * @param page Page number
     * @param perPage Items to be returned per page
     * @param direction Sort order (ASC, DESC)
     * @param searchParam Lucene criteria used to filter as +catchall:*searchParam*
     * @return a paginated list of templates that the user has READ permissions and comply with the
     * params provided.
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findThemes(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("hostId") final String hostId,
            @QueryParam(PaginationUtil.PAGE) final int page,
            @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("-1") final int perPage,
            @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
            @QueryParam("searchParam") final String searchParam) {

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

    /**
     * Returns a theme given its ID (folder theme inode)
     *
     * @param themeId folder inode
     * @returnTh
     */
    @GET
    @Path("/id/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findThemeById(@Context final HttpServletRequest request,
            final @Context HttpServletResponse response,
            @PathParam("id") final String themeId) throws DotDataException, DotSecurityException {

        Logger.debug(this, "Getting the theme by identifier: " + themeId);

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        return Response
                .ok(new ResponseEntityView(themeAPI.findThemeById(themeId, user, false).getMap()))
                .build();
    }
}
