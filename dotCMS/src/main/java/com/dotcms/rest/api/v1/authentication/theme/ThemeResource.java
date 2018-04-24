package com.dotcms.rest.api.v1.authentication.theme;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.*;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides different methods to access information about Themes in dotCMS.
 */
@Path("/v1/theme")
public class ThemeResource {

    private static final String LUCENE_QUERY = "+parentpath:/application/themes/* +title:template.vtl host:%s";

    private final ContentletAPI contentletAPI;
    private final UserAPI userAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final WebResource webResource;

    public ThemeResource() {
        this(APILocator.getContentletAPI(), APILocator.getUserAPI(), APILocator.getHostAPI(), APILocator.getFolderAPI(),
                new WebResource());
    }

    @VisibleForTesting
    public ThemeResource(final ContentletAPI contentletAPI, final UserAPI userAPI, final HostAPI hostAPI,
                         final FolderAPI folderAPI, final WebResource webResource) {

        this.contentletAPI = contentletAPI;
        this.userAPI = userAPI;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
        this.webResource = webResource;
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
                                     @QueryParam("hostId") final String hostId) {

        final InitDataObject initData = this.webResource.init(null, true, request, true, null);
        final User user = initData.getUser();

        final String hostIdForSearch = hostId != null ?
                hostId :
                (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

        final String query = String.format(LUCENE_QUERY, hostIdForSearch);
        Response res = null;

        try {
            final Collection<ThemeView> themes = new ArrayList<>();
            final User systemUser = userAPI.getSystemUser();
            final List<Contentlet> contentlets = contentletAPI.search(query, 0, -1, null, user, false);

            for (final Contentlet contentlet : contentlets) {
                final String folderId = contentlet.getFolder();
                final String themeHostId = contentlet.getHost();

                final Host host = hostAPI.find(themeHostId, systemUser, false);
                final Folder folder = folderAPI.find(folderId, systemUser, false);

                themes.add(new ThemeView(folder.getName(), host));
            }

            return Response.ok(themes).build();
        } catch (DotSecurityException e) {
            final String errorMsg = "The user does not have the required permissions (" + e
                    .getMessage() + ")";
            Logger.error(this, errorMsg, e);
            throw new ForbiddenException(e);
        } catch (DotDataException e) {
            final String errorMsg = "An error occurred when accessing the page information (" + e
                    .getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        }

        return res;
    }
}
