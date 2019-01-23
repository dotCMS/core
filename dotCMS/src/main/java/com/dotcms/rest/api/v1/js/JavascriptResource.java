package com.dotcms.rest.api.v1.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache;
import com.dotcms.cache.DotJSONCacheFactory;
import com.dotcms.javascript.app.util.JavaScriptUtil;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.UriInfo;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.BodyPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.ContentDisposition;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataBodyPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataMultiPart;
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
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;

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
    @Path("/{folder}/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        @PathParam("pathParam") final String pathParam, final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.GET, bodyMap);
    }

    @GET
    @Path("/{folder}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.GET, bodyMap);
    }

    /**
     * Returns the output of a convention based "post.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "post.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */
    @POST
    @Path("/{folder}/{pathParam: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response post(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                               @PathParam("pathParam") final String pathParam,
                               final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.POST, bodyMap);
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
    @Path("/{folder}/{pathParam: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response put(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                              @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                              @PathParam("pathParam") final String pathParam,
                              final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.PUT, bodyMap);
    }

    @PUT
    @Path("/{folder}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response put(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                              @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                              final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.PUT, bodyMap);
    }

    /**
     * Returns the output of a convention based "patch.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "patch.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */
    @PATCH
    @Path("/{folder}/{pathParam: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response patch(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                                @PathParam("pathParam") final String pathParam,
                                final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.PATCH, bodyMap);
    }

    @PATCH
    @Path("/{folder}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response patch(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                                final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.PATCH, bodyMap);
    }

    /**
     * Returns the output of a convention based "delete.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "delete.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */
    @DELETE
    @Path("/{folder}/{pathParam: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response delete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                                 @PathParam("pathParam") final String pathParam,
                                 final Map<String, String> requestJSONMap) {

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.DELETE, requestJSONMap);
    }

    @DELETE
    @Path("/{folder}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public final Response delete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                                 final Map<String, String> requestJSONMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.DELETE, requestJSONMap);
    }

    /**
     * Same as {@link #post} but supporting a multipart request
     */
    @POST
    @Path("/{folder}/{pathParam: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response postMultipart(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                        FormDataMultiPart multipart,
                                        @PathParam("pathParam") final String pathParam,
                                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName) {

        return processMultiPartRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.POST, multipart);

    }

    @POST
    @Path("/{folder}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response postMultipart(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                        FormDataMultiPart multipart,
                                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName) {

        return processMultiPartRequest(request, response, uriInfo, folderName, null, HTTPMethod.POST, multipart);

    }

    /**
     * Same as {@link #put} but supporting a multipart request
     */
    @PUT
    @Path("/{folder}/{pathParam: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response putMultipart(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       FormDataMultiPart multipart,
                                       @PathParam("pathParam") final String pathParam,
                                       @Context UriInfo uriInfo, @PathParam("folder") final String folderName) {

        return processMultiPartRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.PUT, multipart);

    }

    @PUT
    @Path("/{folder}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response putMultipart(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       final FormDataMultiPart multipart,
                                       @Context final UriInfo uriInfo, @PathParam("folder") final String folderName) {

        return processMultiPartRequest(request, response, uriInfo, folderName, null, HTTPMethod.PUT, multipart);
    }

    /**
     * Same as {@link #patch} but supporting a multipart request
     */
    @PATCH
    @Path("/{folder}/{pathParam: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response patchMultipart(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                         FormDataMultiPart multipart,
                                         @PathParam("pathParam") final String pathParam,
                                         @Context UriInfo uriInfo, @PathParam("folder") final String folderName) {

        return processMultiPartRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.PATCH, multipart);

    }

    @PATCH
    @Path("/{folder}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response patchMultipart(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                         final FormDataMultiPart multipart,
                                         @Context final UriInfo uriInfo, @PathParam("folder") final String folderName) {

        return processMultiPartRequest(request, response, uriInfo, folderName, null, HTTPMethod.PATCH, multipart);
    }

    /**
     * Same as {@link #get} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @GET
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.GET, bodyMap);
    }

    @GET
    @Path("/dynamic")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo,
                               final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, null, HTTPMethod.GET, bodyMap);
    }

    /**
     * Same as {@link #post} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @POST
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.POST, bodyMap);
    }

    @POST
    @Path("/dynamic")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo,
                                final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, null, HTTPMethod.POST, bodyMap);
    }

    /**
     * Same as {@link #put} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @PUT
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicPut(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.PUT, bodyMap);
    }

    @PUT
    @Path("/dynamic")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicPut(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context final UriInfo uriInfo,
                               final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, null, HTTPMethod.PUT, bodyMap);
    }

    /**
     * Same as {@link #patch} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @PATCH
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicPatch(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                 final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.PATCH, bodyMap);
    }


    /**
     * Same as {@link #delete} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @DELETE
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response dynamicDelete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                  final @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                  final Map<String, String> bodyMap) {

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.DELETE, bodyMap);
    }

    private Response processMultiPartRequest(final HttpServletRequest request, final HttpServletResponse response,
                                             final UriInfo uriInfo, final String folderName,
                                             final String pathParam,
                                             final HTTPMethod httpMethod,
                                             final FormDataMultiPart multipart) {
        try {
            final List<File> binaries = getBinariesFromMultipart(multipart);
            final Map<String, String> bodyMap = getBodyMapFromMultipart(multipart);

            return processRequest(request, response, uriInfo, folderName, pathParam, httpMethod, bodyMap,
                    binaries.toArray(new File[0]));
        }  catch(Exception e) {
            Logger.error(this,"Exception on VTL endpoint. POST method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private Response processRequest(final HttpServletRequest request, final HttpServletResponse response,
                                    final UriInfo uriInfo, final String folderName,
                                    final String pathParam,
                                    final HTTPMethod httpMethod,
                                    final Map<String, String> bodyMap,
                                    final File...binaries) {
        final InitDataObject initDataObject = this.webResource.init
                (null, false, request, false, null);

        final User user = initDataObject.getUser();
        setUserInSession(request.getSession(false), user);

        final DotJSONCache cache = DotJSONCacheFactory.getCache(httpMethod);
        final Optional<DotJSON> dotJSONOptional = cache.get(request, user);

        if(dotJSONOptional.isPresent()) {
            return Response.ok(dotJSONOptional.get().getMap()).build();
        }

        try {

            final FileAsset getJavascriptFile       = getJavascriptFile(HTTPMethod.GET, request, folderName, initDataObject.getUser());
            final Map<String, Object> contextParams = CollectionsUtils.map(
                    "pathParam", pathParam,
                    "urlParams", initDataObject.getParamsMap(),
                    "user", user,
                    "queryParams", uriInfo.getQueryParameters(),
                    "bodyMap", bodyMap,
                    "console", JavaScriptUtil.getConsole(),
                    "binaries", Arrays.asList(binaries));

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

    private FileAsset getJavascriptFile(final HTTPMethod httpMethod, final HttpServletRequest request, final String folderName,
                                        final User user) throws DotDataException, DotSecurityException {

        final Language currentLanguage  = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        final Host site             = this.hostAPI.resolveHostName(request.getServerName(), APILocator.systemUser(), false);
        final String   jsFilePath       = builder(JS_PATH, StringPool.SLASH, folderName, StringPool.SLASH,
                httpMethod.fileName(), FILE_EXTENSION).toString();
        final Identifier identifier     = identifierAPI.find(site, jsFilePath);
        final Contentlet getFileContent = contentletAPI.findContentletByIdentifier(identifier.getId(), true,
                currentLanguage.getId(), user, true);
        return APILocator.getFileAssetAPI().fromContentlet(getFileContent);
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

            if(!response.isCommitted()) {

                return Response.ok(dotJSON.getMap()).build();
            }
        }

        return null;
    }

    private void setUserInSession(final HttpSession session, final User user) {
        DotPreconditions.checkNotNull(session);
        DotPreconditions.checkNotNull(user);
        session.setAttribute(WebKeys.CMS_USER, user);
    }

    private Map<String, String> getBodyMapFromMultipart(final FormDataMultiPart multipart) throws IOException, JSONException {
        Map<String, String> bodyMap = null;

        for (final BodyPart part : multipart.getBodyParts()) {
            final ContentDisposition contentDisposition = part.getContentDisposition();
            final String name = contentDisposition != null && contentDisposition.getParameters().containsKey("name")
                    ? contentDisposition.getParameters().get("name")
                    : "";

            if (part.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE) || name
                    .equals("json")) {

                bodyMap = WebResource.processJSON(part.getEntityAs(InputStream.class));
            }
        }

        return bodyMap;
    }

    private List<File> getBinariesFromMultipart(final FormDataMultiPart multipart) throws IOException {
        final List<File> binaries = new ArrayList<>();

        for (final FormDataBodyPart part : multipart.getFields("file")) {
            final File tmpFolder = new File(
                    APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + UUIDUtil.uuid());

            if(!tmpFolder.mkdirs()) {
                throw new IOException("Unable to create temp folder to save binaries");
            }

            final String filename = part.getContentDisposition().getFileName();
            final File tempFile = new File(
                    tmpFolder.getAbsolutePath() + File.separator + filename);

            Files.deleteIfExists(tempFile.toPath());

            FileUtils.copyInputStreamToFile(part.getEntityAs(InputStream.class), tempFile);
            binaries.add(tempFile);
        }

        return binaries;
    }

}