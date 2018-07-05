package com.dotcms.rest.api.v1.theme;

import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.JsonProcessingRuntimeException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.ThemePaginator;
import com.dotmarketing.beans.Host;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotcms.repackage.com.fasterxml.jackson.databind.module.SimpleModule;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
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

    public ThemeResource() {
         this(
            new ThemePaginator(),
            APILocator.getHostAPI(),
                 APILocator.getFolderAPI(),
            new WebResource()
        );
    }

    @VisibleForTesting
    ThemeResource(final ThemePaginator themePaginator, final HostAPI hostAPI, final FolderAPI folderAPI,
                         final WebResource webResource ) {
        this.webResource  = webResource;
        this.hostAPI      = hostAPI;
        this.paginationUtil = new PaginationUtil(themePaginator, this.getMapper());
        this.folderAPI = folderAPI;
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
                                     @QueryParam("hostId") final String hostId,
                                     @QueryParam(PaginationUtil.PAGE) final int page,
                                     @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("-1") final int perPage,
                                     @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
                                     @QueryParam("searchParam") final String searchParam)
            throws Throwable {

        Logger.debug(this,
                "Getting the themes for the hostId: " + hostId);

        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final User user = initData.getUser();
        Host host;

        String hostIdToSearch = hostId != null ?
                hostId :
                (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);


        if (UtilMethods.isSet(hostIdToSearch)){
            //Validate hostId is valid
            host = hostAPI.find(hostIdToSearch, user, false);

            if (!UtilMethods.isSet(host)){
                return ExceptionMapperUtil
                        .createResponse(map("message", "Invalid Host ID"), "Invalid Host ID",
                                Status.BAD_REQUEST);
            }
        }else{
            //Validate hostId is valid
            host = hostAPI.findDefaultHost(user, false);
            hostIdToSearch = host.getIdentifier();
        }

        try {
            final Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, hostIdToSearch
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
     * @param id folder inode
     * @return
     * @throws Throwable
     */
    @GET
    @Path("/id/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findThemeById(@Context final HttpServletRequest request,
            @PathParam("id") final String id) throws DotDataException, DotSecurityException {

        Logger.debug(this,
                "Getting the theme by identifier: " + id);

        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final User user = initData.getUser();

        final Folder folder = folderAPI.find(id, user, false);

        if (folder == null) {
            return Response.status(Status.NOT_FOUND).build();
        } else {
            return Response.ok(new ResponseEntityView(folder.getMap())).build();
        }
    }

    private ObjectMapper getMapper() {
        final  ObjectMapper mapper = new ObjectMapper();
        final  SimpleModule module = new SimpleModule();
        mapper.registerModule(module);

        return mapper;
    }
}
