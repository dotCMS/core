package com.dotcms.rest.api.v1.vtl;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache;
import com.dotcms.cache.DotJSONCacheFactory;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.UriInfo;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.PATCH;
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
import com.dotmarketing.util.UtilMethods;
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
    @Path("/{folder}/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        @PathParam("pathParam") final String pathParams, final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParams, HTTPMethod.GET, bodyMap);
    }

    @GET
    @Path("/{folder}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        final Map<String, String> bodyMap) {

        return get(request, response, uriInfo, folderName, null, bodyMap);
    }

    /**
     * Returns the output of a convention based "post.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "post.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
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
                                   final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParams, HTTPMethod.POST, bodyMap);
    }

    @POST
    @Path("/{folder}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response post(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                               final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.POST, bodyMap);
    }

    /**
     * Returns the output of a convention based "put.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "put.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */
    @PUT
    @Path("/{folder}/{path: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response put(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                               @PathParam("path") final String pathParams,
                               final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParams, HTTPMethod.PUT, bodyMap);
    }

    /**
     * Returns the output of a convention based "patch.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "patch.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */
    @PATCH
    @Path("/{folder}/{path: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response patch(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                              @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                              @PathParam("path") final String pathParams,
                              final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParams, HTTPMethod.PATCH, bodyMap);
    }

    /**
     * Returns the output of a convention based "delete.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "delete.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */
    @DELETE
    @Path("/{folder}/{path: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response delete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                               @PathParam("path") final String pathParams,
                               final Map<String, String> requestJSONMap) {

        return processRequest(request, response, uriInfo, folderName, pathParams, HTTPMethod.DELETE, requestJSONMap);
    }

    private Response processRequest(@Context HttpServletRequest request, @Context HttpServletResponse response,
                                    @Context UriInfo uriInfo, @PathParam("folder") String folderName,
                                    @PathParam("path") String pathParam,
                                    final HTTPMethod httpMethod,
                                    final Map<String, String> bodyMap) {
        final InitDataObject initDataObject = this.webResource.init
                (null, false, request, false, null);

        setUserInSession(request.getSession(false), initDataObject.getUser());

        final DotJSONCache cache = DotJSONCacheFactory.getCache(httpMethod);
        final Optional<DotJSON> dotJSONOptional = cache.get(request, initDataObject.getUser());

        if(dotJSONOptional.isPresent()) {
            return Response.ok(dotJSONOptional.get()).build();
        }

        try {
            final FileAsset getFileAsset = getVTLFile(httpMethod, request, folderName, initDataObject.getUser());

            final Map<String, Object> contextParams = CollectionsUtils.map(
                    "pathParam", pathParam,
                    "queryParams", uriInfo.getQueryParameters(),
                    "bodyMap", bodyMap);

            return evalVTLFile(request, response, getFileAsset, contextParams,
                    initDataObject.getUser(), cache);
        } catch(DotContentletStateException e) {
            final String errorMessage = "Unable to find velocity file '" +
                    httpMethod.fileName() + FILE_EXTENSION + "' under path '" + VTL_PATH +
                    StringPool.SLASH + folderName + StringPool.SLASH + "'";
            Logger.error(this, errorMessage, e);
            return ResponseUtil.mapExceptionResponse(new DotDataException(errorMessage));
        } catch(Exception e) {
            Logger.error(this,"Exception on VTL endpoint. GET method: " + e.getMessage(), e);
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
                if(UtilMethods.isSet(dotJSON.get("errors"))) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(dotJSON.get("errors")).build();
                }

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