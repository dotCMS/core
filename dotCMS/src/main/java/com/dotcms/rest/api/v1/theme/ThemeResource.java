package com.dotcms.rest.api.v1.theme;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.JsonProcessingRuntimeException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.ThemePaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
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
import javax.ws.rs.core.Response.Status;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Provides different methods to access information about Themes in dotCMS.
 */
@Path("/v1/themes")
public class ThemeResource {

    private final PaginationUtil paginationUtil;
    private final WebResource webResource;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
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
    ThemeResource(final ThemePaginator themePaginator, final HostAPI hostAPI, final FolderAPI folderAPI,
                         final ThemeAPI themeAPI, final WebResource webResource ) {
        this.webResource  = webResource;
        this.hostAPI      = hostAPI;
        this.paginationUtil = new PaginationUtil(themePaginator);
        this.folderAPI = folderAPI;
        this.themeAPI = themeAPI;
    }

    /**
     * Returns all themes
     * @param request
     * @param hostId Optional param, when it is not specified the default host is used
     * @param page Page number
     * @param perPage Items to be returned per page
     * @param direction Sort order (ASC, DESC)
     * @param searchParam Lucene criteria used to filter as +catchall:*searchParam*
     * @return
     * @throws Throwable
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
                                     @QueryParam("searchParam") final String searchParam)
            throws Throwable {

        Logger.debug(this,
                "Getting the themes for the hostId: " + hostId);

        final InitDataObject initData = this.webResource.init(null, request, response, true, null);
        final User user = initData.getUser();
        Host host = null;


        if (UtilMethods.isSet(hostId)){
            //Validate hostId is valid
            host = hostAPI.find(hostId, user, false);
        }else{
            return ExceptionMapperUtil
                    .createResponse(map("message", "Host ID is required"), "Host ID is required",
                            Status.BAD_REQUEST);
        }

        if (!UtilMethods.isSet(host)){
            return ExceptionMapperUtil
                    .createResponse(map("message", "Invalid Host ID"), "Invalid Host ID",
                            Status.NOT_FOUND);
        }

        try {
            final Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, hostId
            );

            if (UtilMethods.isSet(searchParam)){
                params.put(ThemePaginator.SEARCH_PARAMETER, searchParam);
            }

            return this.paginationUtil.getPage(request, user, null, page, perPage, null,
                    OrderDirection.valueOf(direction), params);
        } catch (PaginationException e) {
            throw e.getCause();
        } catch (JsonProcessingRuntimeException e) {
            Logger.error(this, e.getMessage(), e);
            return ExceptionMapperUtil.createResponse(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns a theme given its ID (folder theme inode)
     * @param request
     * @param themeId folder inode
     * @returnTh
     * @throws Throwable
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

        final InitDataObject initData = this.webResource.init(null, request, response, true, null);
        final User user   = initData.getUser();
        return Response.ok(new ResponseEntityView(themeAPI.findThemeById(themeId, user, false).getMap())).build();
    }
}