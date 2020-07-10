package com.dotcms.rest.api.v1.folder;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.liferay.util.StringPool;
import org.glassfish.jersey.server.JSONP;

/**
 * Created by jasontesser on 9/28/16.
 */
@Path("/v1/folder")
public class FolderResource implements Serializable {
    private final WebResource webResource;
    private final FolderHelper folderHelper;
    private final I18NUtil i18NUtil;

    public FolderResource() {
        this(new WebResource(),
                FolderHelper.getInstance(),
                I18NUtil.INSTANCE);
    }

    @VisibleForTesting
    public FolderResource(final WebResource webResource,
                          final FolderHelper folderHelper,
                          final I18NUtil i18NUtil) {

        this.webResource = webResource;
        this.folderHelper = folderHelper;
        this.i18NUtil = i18NUtil;
    }

    @POST
    @Path("/createfolders")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response createFolders(@Context final HttpServletRequest httpServletRequest,
                                        @Context final HttpServletResponse httpServletResponse,
                                        final List<String> paths,
                                        @PathParam("siteName") final String siteName) {
        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        final User user = initData.getUser();
        final List<Map<String, Object>> folderResults;

        try {
            Locale locale = LocaleUtil.getLocale(user, httpServletRequest);
            FolderHelper.FolderResults results = folderHelper.createFolders(paths, siteName, user);
            folderResults = results.folders
                    .stream()
                    .map(folder -> {
                        try {
                            return folder.getMap();
                        } catch (Exception e) {
                            Logger.error(this, "Data Exception while converting to map", e);
                            throw new DotRuntimeException("Data Exception while converting to map", e);
                        }
                    })
                    .collect(Collectors.toList());
            ;

            response = Response.ok(new ResponseEntityView
                    (map("result", folderResults),
                            results.getErrorEntities(), null,
                            this.i18NUtil.getMessagesMap(locale, "Invalid-option-selected",
                                    "cancel")
                    )).build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            Logger.error(this, "Error handling Save Folder Post Request", e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @GET
    @Path ("/sitename/{siteName}/uri/{uri : .+}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadFolderByURI(@Context final HttpServletRequest httpServletRequest,
                                          @Context final HttpServletResponse httpServletResponse,
                                          @PathParam("siteName") final String siteName,
                                          @PathParam("uri") final String uri){
        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        final User user = initData.getUser();
        try{
            final String uriParam = !uri.startsWith(StringPool.FORWARD_SLASH) ? StringPool.FORWARD_SLASH.concat(uri) : uri;
            final Folder folder = folderHelper.loadFolderByURI(siteName,user,uriParam);
            response = Response.ok( new ResponseEntityView(folder) ).build();
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            Logger.error(this, "Error gettign folder for URI", e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

}
