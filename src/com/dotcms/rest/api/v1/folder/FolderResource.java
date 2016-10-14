package com.dotcms.rest.api.v1.folder;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Arrays;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Created by jasontesser on 9/28/16.
 */
@Path("/v1/folder")
public class FolderResource implements Serializable {
    private final WebResource webResource;
    private final FolderHelper folderHelper;
    private final LayoutAPI layoutAPI;
    private final I18NUtil i18NUtil;

    public FolderResource() {
        this(new WebResource(),
                FolderHelper.getInstance(),
                APILocator.getLayoutAPI(),
                I18NUtil.INSTANCE);
    }

    @VisibleForTesting
    public FolderResource(final WebResource webResource,
                          final FolderHelper folderHelper,
                          final LayoutAPI layoutAPI,
                          final I18NUtil i18NUtil) {

        this.webResource = webResource;
        this.folderHelper = folderHelper;
        this.layoutAPI = layoutAPI;
        this.i18NUtil = i18NUtil;
    }

    @POST
    @Path("/createfolders")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response createFolders(@Context final HttpServletRequest req,
                                        final List<String> paths,
                                        @PathParam("siteName") final String siteName) {
        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();
        final List<Map<String, Object>> folderResults;

        try {
            Locale locale = LocaleUtil.getLocale(user, req);
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
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

}
