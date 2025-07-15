package com.dotcms.rest.api.v1.browsertree;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@SwaggerCompliant(value = "Site architecture and template management APIs", batch = 3)
@Deprecated
@Path("/v1/browsertree")
@Tag(name = "Browser Tree")
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

    @Operation(
        summary = "Load assets under site root (deprecated)",
        description = "Loads all treeable assets under the root directory of a specified site. This method is deprecated - use BrowserResource.getFolderContent instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Assets loaded successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Deprecated
    @GET
    @Path ("/sitename/{sitename}/uri/")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response loadAssetsUnder(
            @Context final HttpServletRequest  httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Name of the site to load assets from", required = true) @PathParam("sitename")   final String sitename) {

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

            response = Response.ok(new ResponseEntityMapView
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

    @Operation(
        summary = "Load assets under specific URI (deprecated)",
        description = "Loads all treeable assets under a specific URI path within a site. This method is deprecated - use BrowserResource.getFolderContent instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Assets loaded successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Deprecated
    @GET
    @Path ("/sitename/{sitename}/uri/{uri : .+}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response loadAssetsUnder(
            @Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Name of the site to load assets from", required = true) @PathParam("sitename")   final String sitename,
            @Parameter(description = "URI path within the site to load assets from", required = true) @PathParam("uri") final String uri
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

            response = Response.ok(new ResponseEntityMapView
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
