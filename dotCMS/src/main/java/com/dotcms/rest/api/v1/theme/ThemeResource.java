package com.dotcms.rest.api.v1.theme;

import com.dotcms.util.JsonProcessingRuntimeException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.ThemePaginator;
import com.dotmarketing.portlets.htmlpages.theme.business.ThemeSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Logger;
import com.dotcms.repackage.com.fasterxml.jackson.databind.module.SimpleModule;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
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
    private final UserAPI userAPI;

    public ThemeResource() {
        this(
            new ThemePaginator(),
            APILocator.getHostAPI(),
            APILocator.getFolderAPI(),
            APILocator.getUserAPI(),
            new WebResource()
        );
    }

    @VisibleForTesting
    ThemeResource(final ThemePaginator themePaginator, final HostAPI hostAPI, final FolderAPI folderAPI,
                         final UserAPI userAPI, final WebResource webResource ) {
        this.webResource = webResource;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
        this.userAPI = userAPI;
        this.paginationUtil = new PaginationUtil(themePaginator, this.getMapper());
    }

    /**
     * Returns all themes
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findThemes(@Context final HttpServletRequest request,
                                     @QueryParam("hostId") final String hostId,
                                     @QueryParam(PaginationUtil.PAGE) final int page,
                                     @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("-1") final int perPage,
                                     @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction)
            throws Throwable {

        Logger.debug(this,
                "Getting the themes for the hostId: " + hostId);

        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final User user = initData.getUser();

        final String hostIdToSearch = hostId != null ?
                hostId :
                (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

        try {
            final Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, hostIdToSearch
            );

            return this.paginationUtil.getPage(request, user, null, page, perPage, null,
                    OrderDirection.valueOf(direction), params);
        } catch (PaginationException e) {
            throw e.getCause();
        } catch (JsonProcessingRuntimeException e) {
            final String errorMsg = "An error occurred when accessing the page information (" + e
                    .getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            return ExceptionMapperUtil.createResponse(null, errorMsg);
        }
    }

    private ObjectMapper getMapper() {
        final  ObjectMapper mapper = new ObjectMapper();
        final  SimpleModule module = new SimpleModule();
        module.addSerializer(Contentlet.class, new ThemeSerializer(hostAPI, folderAPI, userAPI));
        mapper.registerModule(module);

        return mapper;
    }
}
