package com.dotcms.rest.api.v1.vtl;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
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
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.cache.DotJSONCacheFactory.getCacheStrategy;

@Path("/vtl")
public class VTLResource {

    private final WebResource webResource;
    @VisibleForTesting
    static final String VTL_PATH = "/application/apivtl";
    private final HostAPI hostAPI;
    private final IdentifierAPI identifierAPI;
    private final ContentletAPI contentletAPI;
    private final String FILE_EXTENSION = ".vtl";

    public VTLResource() {
        this(APILocator.getHostAPI(), APILocator.getIdentifierAPI(), APILocator.getContentletAPI(),
                new WebResource());
    }

    @VisibleForTesting
    VTLResource(final HostAPI hostAPI, final IdentifierAPI identifierAPI, final ContentletAPI contentletAPI,
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
            final FileAsset getFileAsset = getVTLFile(HTTPMethod.GET, request, folderName, initDataObject.getUser());

            final Map<String, Object> contextParams = CollectionsUtils.map(
                    "urlParams", initDataObject.getParamsMap(),
                    "queryParams", uriInfo.getQueryParameters());

            return evalVTLFile(request, response, getFileAsset, contextParams,
                    initDataObject.getUser(), cache);
        } catch(DotContentletStateException e) {
            final String errorMessage = "Unable to find velocity file '" +
                    HTTPMethod.GET.fileName() + FILE_EXTENSION + "' under path '" + VTL_PATH +
                    StringPool.SLASH + folderName + StringPool.SLASH + "'";
            Logger.error(this, errorMessage, e);
            return ResponseUtil.mapExceptionResponse(new DotDataException(errorMessage));
        } catch(Exception e) {
            Logger.error(this,"Exception on VTL endpoint. GET method: " + e.getMessage(), e);
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
            final FileAsset vtlFile = getVTLFile(HTTPMethod.POST, request, folderName, initDataObject.getUser());
            final Map<String, Object> contextParams = CollectionsUtils.map(
                    "urlParams", initDataObject.getParamsMap(),
                    "queryParams", uriInfo.getQueryParameters(),
                    "postParams", properties,
                    "user", initDataObject.getUser());

            return evalVTLFile(request, response, vtlFile, contextParams,
                    initDataObject.getUser(), getCacheStrategy(HTTPMethod.POST));

        } catch(DotContentletStateException e) {
            final String errorMessage = "Unable to find velocity file '" + HTTPMethod.POST.fileName() + FILE_EXTENSION
                    + "' under path '" + VTL_PATH + StringPool.SLASH + folderName + StringPool.SLASH + "'";
            Logger.warn(this, errorMessage, e);
            return ResponseUtil.mapExceptionResponse(new DotDataException(errorMessage));
        } catch(Exception e) {
            Logger.error(this,"Exception on VTL endpoint. POST method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private void setUserInSession(final HttpSession session, final User user) {
        DotPreconditions.checkNotNull(session);
        DotPreconditions.checkNotNull(user);
        session.setAttribute(WebKeys.CMS_USER, user);
    }

    private Response evalVTLFile(final HttpServletRequest request, final HttpServletResponse response,
                                 final FileAsset getFileAsset, final Map<String, Object> contextParams,
                                 final User user, final DotJSONCache cache)
            throws IOException {
        final org.apache.velocity.context.Context context = VelocityUtil.getInstance().getContext(request, response);
        contextParams.forEach(context::put);
        context.put("dotJSON", new DotJSON());

        final StringWriter evalResult = new StringWriter();

        try (final InputStream fileAssetIputStream = getFileAsset.getInputStream()) {
            VelocityUtil.getEngine().evaluate(context, evalResult, "", fileAssetIputStream);
            final DotJSON dotJSON = (DotJSON) context.get("dotJSON");

            if(dotJSON.size()==0) { // If dotJSON is not used let's return the raw evaluation of the velocity file
                return Response.ok(evalResult.toString()).build();
            } else {
                // let's add it to cache
                cache.add(request, user, dotJSON);
                return Response.ok(dotJSON.getMap()).build();
            }
        }
    }

    private FileAsset getVTLFile(final HTTPMethod httpMethod, final HttpServletRequest request, final String folderName,
                                 final User user) throws DotDataException, DotSecurityException {
        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final Host site = this.hostAPI.resolveHostName(request.getServerName(), APILocator.systemUser(), false);
        final String vtlFilePath = VTL_PATH + StringPool.SLASH + folderName + StringPool.SLASH
                + httpMethod.fileName() + FILE_EXTENSION;
        final Identifier identifier = identifierAPI.find(site, vtlFilePath);
        final Contentlet getFileContent = contentletAPI.findContentletByIdentifier(identifier.getId(), true,
                currentLanguage.getId(), user, true);
        return APILocator.getFileAssetAPI().fromContentlet(getFileContent);
    }

}