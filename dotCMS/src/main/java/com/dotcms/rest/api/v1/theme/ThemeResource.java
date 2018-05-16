package com.dotcms.rest.api.v1.theme;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectWriter;
import com.dotcms.util.PaginationUtil;
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
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.htmlpages.theme.business.ThemeAPI;
import com.dotmarketing.util.Logger;
import com.dotcms.repackage.com.fasterxml.jackson.databind.module.SimpleModule;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Provides different methods to access information about Themes in dotCMS.
 */
@Path("/v1/themes")
public class ThemeResource {


    private final ThemeAPI themeAPI;
    private final WebResource webResource;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final UserAPI userAPI;

    public ThemeResource() {
        this(APILocator.getThemeAPI(), APILocator.getHostAPI(), APILocator.getFolderAPI(), APILocator.getUserAPI(),
                new WebResource());
    }

    @VisibleForTesting
    ThemeResource(final ThemeAPI themeAPI, final HostAPI hostAPI, final FolderAPI folderAPI,
                         final UserAPI userAPI, final WebResource webResource ) {

        this.themeAPI = themeAPI;
        this.webResource = webResource;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
        this.userAPI = userAPI;
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
                                     //@QueryParam(PaginationUtil.PAGE) final int page,
                                     @QueryParam(PaginationUtil.PER_PAGE) @DefaultValue("-1") final int perPage,
                                     @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) String direction) {

        Logger.debug(this,
                "Getting the themes for the hostId: " + hostId);

        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final User user = initData.getUser();

        final String hostIdToSearch = hostId != null ?
                hostId :
                (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

        Response res = null;

        try {
            final List<Contentlet> themes = themeAPI.findAll(user, hostIdToSearch);
            return Response.ok(this.asJson(themes)).build();
        } catch (DotSecurityException e) {
            final String errorMsg = "The user does not have the required permissions (" + e
                    .getMessage() + ")";
            Logger.error(this, errorMsg, e);
            throw new ForbiddenException(e);
        } catch (DotDataException | JsonProcessingException e) {
            final String errorMsg = "An error occurred when accessing the page information (" + e
                    .getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        }

        return res;
    }

    private String asJson(final  List<Contentlet> themes) throws JsonProcessingException {
        final  ObjectMapper mapper = new ObjectMapper();
        final  SimpleModule module = new SimpleModule();
        module.addSerializer(Contentlet.class, new ThemeSerializer(hostAPI, folderAPI, userAPI));
        mapper.registerModule(module);

        final ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        return objectWriter.writeValueAsString(themes);
    }
}
