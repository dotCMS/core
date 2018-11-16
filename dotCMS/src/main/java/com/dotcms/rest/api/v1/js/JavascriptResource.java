package com.dotcms.rest.api.v1.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache;
import com.dotcms.javascript.app.util.JavaScriptUtil;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.UriInfo;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.cache.DotJSONCacheFactory.getCacheStrategy;
import static com.dotmarketing.util.StringUtils.builder;

/**
 * Endpoint to evaluate Javascripts
 * @author jsanca
 */
@Path("/js")
public class JavascriptResource {

    private final WebResource webResource;
    @VisibleForTesting
    static final String JS_PATH = "/application/apijs";
    private final HostAPI hostAPI;
    private final IdentifierAPI identifierAPI;
    private final ContentletAPI contentletAPI;
    private final String FILE_EXTENSION = ".js";

    public JavascriptResource() {
        this(APILocator.getHostAPI(), APILocator.getIdentifierAPI(), APILocator.getContentletAPI(),
                new WebResource());
    }

    @VisibleForTesting
    JavascriptResource(final HostAPI hostAPI, final IdentifierAPI identifierAPI, final ContentletAPI contentletAPI,
                       final WebResource webResource) {
        this.hostAPI = hostAPI;
        this.identifierAPI = identifierAPI;
        this.contentletAPI = contentletAPI;
        this.webResource = webResource;
    }

    /**
     * Returns the output of a convention based "get.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "get.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */

    @GET
    @Path("/{folder}/{path: .*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        @PathParam("path") final String pathParams) {

        final InitDataObject initDataObject = this.webResource.init
                (pathParams, false, request, false, null);

        setUserInSession(request.getSession(false), initDataObject.getUser());

        final DotJSONCache cache = getCacheStrategy(HTTPMethod.GET);
        final Optional<DotJSON> dotJSONOptional = cache.get(request, initDataObject.getUser());

        if(dotJSONOptional.isPresent()) {
            return Response.ok(dotJSONOptional.get()).build();
        }

        try {

            final FileAsset getJavascriptFile       = getJavascriptFile(HTTPMethod.GET, request, folderName, initDataObject.getUser());
            final Map<String, Object> contextParams = CollectionsUtils.map(
                    "urlParams", initDataObject.getParamsMap(),
                    "queryParams", uriInfo.getQueryParameters());

            return evalJavascriptFile(request, response, getJavascriptFile, contextParams,
                    HTTPMethod.GET, initDataObject.getUser(), cache);
        } catch(DotContentletStateException e) {
            final String errorMessage = "Unable to find javascript file '" +
                    HTTPMethod.GET.fileName() + FILE_EXTENSION + "' under path '" + JS_PATH +
                    StringPool.SLASH + folderName + StringPool.SLASH + "'";
            Logger.error(this, errorMessage, e);
            return ResponseUtil.mapExceptionResponse(new DotDataException(errorMessage));
        } catch(Exception e) {
            Logger.error(this,"Exception on JS endpoint. GET method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    }

    /**
     * @param request HttpServletRequest
     * @return Response
     */
    @POST
    @Path("/{folder}/{path: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response post(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                               @PathParam("path") final String pathParams,
                                   final Map<String, String> properties) {

        final InitDataObject initDataObject = this.webResource.init
                (pathParams, false, request, false, null);

        setUserInSession(request.getSession(false), initDataObject.getUser());

        try {

            final FileAsset postJavascriptFile      = getJavascriptFile(HTTPMethod.POST, request, folderName, initDataObject.getUser());
            final Map<String, Object> contextParams = CollectionsUtils.map(
                    "urlParams", initDataObject.getParamsMap(),
                    "queryParams", uriInfo.getQueryParameters(),
                    "postParams", properties,
                    "user", initDataObject.getUser());

            return evalJavascriptFile(request, response, postJavascriptFile, contextParams,
                    HTTPMethod.GET, initDataObject.getUser(), getCacheStrategy(HTTPMethod.POST));

        } catch(DotContentletStateException e) {
            final String errorMessage = "Unable to find javascript file '" + HTTPMethod.POST.fileName() + FILE_EXTENSION
                    + "' under path '" + JS_PATH + StringPool.SLASH + folderName + StringPool.SLASH + "'";
            Logger.warn(this, errorMessage, e);
            return ResponseUtil.mapExceptionResponse(new DotDataException(errorMessage));
        } catch(Exception e) {
            Logger.error(this,"Exception on Javascript endpoint. POST method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private void setUserInSession(final HttpSession session, final User user) {
        DotPreconditions.checkNotNull(session);
        DotPreconditions.checkNotNull(user);
        session.setAttribute(WebKeys.CMS_USER, user);
    }

    private Response evalJavascriptFile(final HttpServletRequest request, final HttpServletResponse response,
                                        final FileAsset getFileAsset, final Map<String, Object> context,
                                        final HTTPMethod httpMethod, final User user, final DotJSONCache cache)
            throws IOException {

        context.put("dotJSON", new DotJSON());

        try (final InputStream input = getFileAsset.getInputStream()) {

            JavaScriptUtil.getEngine().invokeFunction (new InputStreamReader(input), httpMethod.fileName(),
                    context, request, response);
            final DotJSON dotJSON = (DotJSON) context.get("dotJSON");

            // let's add it to cache
            cache.add(request, user, dotJSON);
            return Response.ok(dotJSON.getMap()).build();
        }
    }

    private FileAsset getJavascriptFile(final HTTPMethod httpMethod, final HttpServletRequest request, final String folderName,
                                        final User user) throws DotDataException, DotSecurityException {

        final Language currentLanguage  = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final Host     site             = this.hostAPI.resolveHostName(request.getServerName(), APILocator.systemUser(), false);
        final String   jsFilePath       = builder(JS_PATH, StringPool.SLASH, folderName, StringPool.SLASH,
                httpMethod.fileName(), FILE_EXTENSION).toString();
        final Identifier identifier     = identifierAPI.find(site, jsFilePath);
        final Contentlet getFileContent = contentletAPI.findContentletByIdentifier(identifier.getId(), true,
                currentLanguage.getId(), user, true);
        return APILocator.getFileAssetAPI().fromContentlet(getFileContent);
    }

}