package com.dotcms.rest.api.v1.browsertree;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
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
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;
import static com.dotcms.util.CollectionsUtils.map;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jasontesser on 9/28/16.
 */
@Path("/v1/browsertree")
public class BrowserTreeResource implements Serializable {
    private final WebResource webResource;
    private final BrowserTreeHelper browserTreeHelper;
    private final LayoutAPI layoutAPI;
    private final I18NUtil i18NUtil;

    public BrowserTreeResource() {
        this(new WebResource(),
                BrowserTreeHelper.getInstance(),
                APILocator.getLayoutAPI(),
                I18NUtil.INSTANCE);
    }

    @VisibleForTesting
    public BrowserTreeResource(final WebResource webResource,
                               final BrowserTreeHelper browserTreeHelper,
                               final LayoutAPI layoutAPI,
                               final I18NUtil i18NUtil) {

        this.webResource = webResource;
        this.browserTreeHelper  = browserTreeHelper;
        this.layoutAPI   = layoutAPI;
        this.i18NUtil    = i18NUtil;
    }

    @GET
    @Path ("/sitename/{sitename}/uri/{uri : .+}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadAssetsUnder(
            @Context final HttpServletRequest req,
            @PathParam("sitename")   final String sitename,
            @PathParam("uri") final String uri
    ) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();
        final List<Map<String, Object>> assetResults;

        try {
            Locale locale = LocaleUtil.getLocale(user, req);

            assetResults = browserTreeHelper.getTreeablesUnder(sitename,user,uri)
                    .stream()
                    .map(treeable -> {
                        try {
                            return treeable.getMap();
                        } catch (Exception e) {
                            Logger.error(this,"Data Exception while converting to map", e);
                            throw new DotRuntimeException("Data Exception while converting to map",e);
                        }
                    })
                    .collect(Collectors.toList());;

            response = Response.ok(new ResponseEntityView
                    (map(   "result",         assetResults
                    ),
                            this.i18NUtil.getMessagesMap(locale, "Invalid-option-selected",
                                    "cancel", "Change-Host"))
            ).build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            Logger.error(this,"Error handling loadAssetsUnder Get Request", e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

}
