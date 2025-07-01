package com.dotcms.rest.api.v1.browsertree;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.browser.BrowserQueryForm;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jasontesser on 9/28/16.
 * @deprecated see {@link com.dotcms.rest.api.v1.browser.BrowserResource#getFolderContent(HttpServletRequest, HttpServletResponse, BrowserQueryForm)}
 */
@Deprecated
@Path("/v1/browsertree")
@Tag(name = "Browser Tree", description = "File and folder browser tree operations")
public class BrowserTreeResource implements Serializable {
    private final WebResource webResource;
    private final BrowserTreeHelper browserTreeHelper;
    private final I18NUtil i18NUtil;

    public BrowserTreeResource() {
        this(new WebResource(),
                BrowserTreeHelper.getInstance(),
                I18NUtil.INSTANCE);
    }

    @VisibleForTesting
    public BrowserTreeResource(final WebResource webResource,
                               final BrowserTreeHelper browserTreeHelper,
                               final I18NUtil i18NUtil) {

        this.webResource = webResource;
        this.browserTreeHelper  = browserTreeHelper;
        this.i18NUtil    = i18NUtil;
    }

    /**
     * @deprecated see {@link com.dotcms.rest.api.v1.browser.BrowserResource#getFolderContent(HttpServletRequest, HttpServletResponse, BrowserQueryForm)}
     * @param httpRequest
     * @param httpResponse
     * @param sitename
     * @return
     */
    @Deprecated
    @GET
    @Path ("/sitename/{sitename}/uri/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadAssetsUnder(
            @Context final HttpServletRequest  httpRequest,
            @Context final HttpServletResponse httpResponse,
            @PathParam("sitename")   final String sitename) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpRequest, httpResponse, true, null);
        final User user = initData.getUser();
        final List<Map<String, Object>> assetResults;

        try {
            Locale locale = LocaleUtil.getLocale(user, httpRequest);

            assetResults = browserTreeHelper.getTreeablesUnder(sitename,user,"/")
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
                    (Map.of(   "result",         assetResults
                    ),
                            this.i18NUtil.getMessagesMap(locale, "Invalid-option-selected",
                                    "cancel", "Change-Host"))
            ).build(); // 200
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            Logger.error(this,"Error handling loadAssetsUnder Get Request", e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    /**
     * @deprecated see {@link com.dotcms.rest.api.v1.browser.BrowserResource#getFolderContent(HttpServletRequest, HttpServletResponse, BrowserQueryForm)}
     * @param httpRequest
     * @param httpResponse
     * @param sitename
     * @param uri
     * @return
     */
    @Deprecated
    @GET
    @Path ("/sitename/{sitename}/uri/{uri : .+}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadAssetsUnder(
            @Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @PathParam("sitename")   final String sitename,
            @PathParam("uri") final String uri
    ) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpRequest, httpResponse, true, null);
        final User user = initData.getUser();
        final List<Map<String, Object>> assetResults;

        try {
            final Locale locale = LocaleUtil.getLocale(user, httpRequest);

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
                    .collect(Collectors.toList());

            response = Response.ok(new ResponseEntityView
                    (Map.of(   "result",         assetResults
                    ),
                            this.i18NUtil.getMessagesMap(locale, "Invalid-option-selected",
                                    "cancel", "Change-Host"))
            ).build(); // 200
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            Logger.error(this,"Error handling loadAssetsUnder Get Request", e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

}
