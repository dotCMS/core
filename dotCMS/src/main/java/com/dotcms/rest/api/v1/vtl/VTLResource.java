package com.dotcms.rest.api.v1.vtl;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache;
import com.dotcms.cache.DotJSONCacheFactory;
import com.dotcms.rendering.util.ScriptingReaderParams;
import com.dotcms.rendering.util.ScriptingUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotmarketing.util.json.JSONException;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.PATCH;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.velocity.exception.MethodInvocationException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/vtl")
@Tag(name = "Templates", description = "Template design and management")
public class VTLResource {

    public static final String IDENTIFIER = "identifier";
    public static final String VELOCITY = "velocity";
    private final MultiPartUtils multiPartUtils;
    private final WebResource webResource;
    @VisibleForTesting
    static final String VTL_PATH = "/application/apivtl";

    public VTLResource() {
        this(new WebResource(), new MultiPartUtils());
    }

    @VisibleForTesting
    VTLResource(final WebResource webResource, final MultiPartUtils multiPartUtils) {
        this.webResource   = webResource;
        this.multiPartUtils = multiPartUtils;
    }

    /**
     * Returns the output of a convention based "get.vtl" file, located under the given {folder} after being evaluated
     * using the velocity engine.
     *
     * "get.vtl" code determines whether the response is a JSON object or anything else (XML, text-plain).
     * 
     * @deprecated This GET method accepts a request body, which is not standard HTTP practice.
     * Consider using POST for operations that require request bodies.
     */
    @GET
    @Path("/{folder}/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        @PathParam("pathParam") final String pathParam, final Map<String, Object> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.GET, bodyMap);
    }

    /**
     * @deprecated This GET method accepts a request body, which is not standard HTTP practice.
     * Consider using POST for operations that require request bodies.
     */
    @GET
    @Path("/{folder}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        final Map<String, Object> bodyMap) {

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
                                   final Map<String, Object> bodyMap) {

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
                               final Map<String, Object> bodyMap) {

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
                               final Map<String, Object> bodyMap) {

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
                              final Map<String, Object> bodyMap) {

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
                              final Map<String, Object> bodyMap) {

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
                                final Map<String, Object> bodyMap) {

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
                               final Map<String, Object> requestJSONMap) {

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
                                 final Map<String, Object> requestJSONMap) {

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
     * 
     * @deprecated This GET method accepts a request body, which is not standard HTTP practice.
     * Consider using POST for operations that require request bodies.
     */
    @GET
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.GET, bodyMap);
    }

    /**
     * @deprecated This GET method accepts a request body, which is not standard HTTP practice.
     * Consider using POST for operations that require request bodies.
     */
    @GET
    @Path("/dynamic")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo,
                               final String bodyMapStr) {

        return dynamicGet(request, response, uriInfo, null,bodyMapStr);
    }

    /**
     * Same as {@link #post} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @POST
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.POST, bodyMap);
    }

    @POST
    @Path("/dynamic")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo,
                                final String bodyMapString) {

        return dynamicPost(request, response, uriInfo, null, bodyMapString);
    }

    /**
     * Same as {@link #put} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @PUT
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPut(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.PUT, bodyMap);
    }

    @PUT
    @Path("/dynamic")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPut(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context final UriInfo uriInfo,
                               final String bodyMapString) {

        return dynamicPut(request, response, uriInfo, null, bodyMapString);
    }

    /**
     * Same as {@link #patch} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @PATCH
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPatch(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                 final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

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
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicDelete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                  final @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                  final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.DELETE, bodyMap);
    }

    private Response processMultiPartRequest(final HttpServletRequest request, final HttpServletResponse response,
                                    final UriInfo uriInfo, final String folderName,
                                    final String pathParam,
                                    final HTTPMethod httpMethod,
                                             final FormDataMultiPart multipart) {
        try {
            final List<File> binaries = getBinariesFromMultipart(multipart);
            final Map<String, Object> bodyMap = getBodyMapFromMultipart(multipart);

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
                                    final Map<String, Object> bodyMap,
                                    final File...binaries) {

        try {

            ScriptingUtil.getInstance().validateBodyMap(bodyMap, httpMethod);

            final InitDataObject initDataObject = this.webResource.init
                    (null, request, response, false, null);

            final User user = initDataObject.getUser();


            final DotJSONCache cache = DotJSONCacheFactory.getCache(httpMethod);
            final Optional<DotJSON> dotJSONOptional = cache.get(request, user);

            if(dotJSONOptional.isPresent()) {
                return Response.ok(dotJSONOptional.get().getMap()).build();
            }

            final ScriptingReaderParams velocityReaderParams = new ScriptingReaderParams.ScriptingReaderParamsBuilder()
                    .setBodyMap(bodyMap)
                    .setFolderName(folderName)
                    .setHttpMethod(httpMethod)
                    .setRequest(request)
                    .setUser(user)
                    .setPageMode(PageMode.get(request))
                    .build();

            final VelocityReader velocityReader = VelocityReaderFactory.getVelocityReader(UtilMethods.isSet(folderName));

            final Map<String, Object> contextParams = new HashMap<>();

            contextParams.put("pathParam", pathParam);
            contextParams.put("queryParams", uriInfo.getQueryParameters());
            contextParams.put("bodyMap", bodyMap);
            contextParams.put("binaries", Arrays.asList(binaries));

            try(Reader reader = velocityReader.getVelocity(velocityReaderParams)){
                return evalVelocity(request, response, reader, contextParams,
                    initDataObject.getUser(), cache);
            }
        }  catch(Exception e) {
            Logger.error(this,"Exception on VTL endpoint. GET method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private Response evalVelocity(final HttpServletRequest request, final HttpServletResponse response,
                                  final Reader velocityReader, final Map<String, Object> contextParams,
                                  final User user, final DotJSONCache cache)
            throws Exception {
        final org.apache.velocity.context.Context context = VelocityUtil.getInstance().getContext(request, response);
        contextParams.forEach(context::put);
        context.put("dotJSON", new DotJSON());

        final StringWriter evalResult = new StringWriter();

        try {
            VelocityUtil.getEngine().evaluate(context, evalResult, "", velocityReader);
        } catch(MethodInvocationException e) {
            if(e.getCause() instanceof DotToolException) {
                Logger.error(this,"Error evaluating velocity: " + (e.getCause()).getCause().getMessage());
                throw (Exception) (e.getCause()).getCause();
            }
        }
        final DotJSON dotJSON = (DotJSON) context.get("dotJSON");

        if(dotJSON.size()==0) { // If dotJSON is not used let's return the raw evaluation of the velocity file
            final HttpServletResponse velocityResponse = (HttpServletResponse) context.get("response");
            
            final String contentType = (velocityResponse!=null && velocityResponse.getContentType()!=null) ? velocityResponse.getContentType() : MediaType.TEXT_PLAIN_TYPE.toString();
            if(velocityResponse!=null && velocityResponse.getHeaderNames()!=null){
              for(final String  headerName : velocityResponse.getHeaderNames()) {
                response.setHeader(headerName, velocityResponse.getHeader(headerName));
              }
            }

            return UtilMethods.isSet(contentType)
                    ? Response.ok(evalResult.toString()).type(contentType).build()
                    : Response.ok(evalResult.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();

        } else {
            // let's add it to cache
            if(UtilMethods.isSet(dotJSON.get("errors"))) {
                return Response.status(Response.Status.BAD_REQUEST).entity(dotJSON.get("errors")).build();
            }

            cache.add(request, user, dotJSON);
            return Response.ok(dotJSON.getMap()).build();
        }
    }

    private Map<String, Object> getBodyMapFromMultipart(final FormDataMultiPart multipart) throws IOException, JSONException {

        return this.multiPartUtils.getBodyMapFromMultipart(multipart);
    }

    private List<File> getBinariesFromMultipart(final FormDataMultiPart multipart) throws IOException {

        return this.multiPartUtils.getBinariesFromMultipart(multipart);
    }

    private Map<String, Object> parseBodyMap(final String bodyMapString) {
        Map<String, Object> bodyMap = new HashMap<>();

        // 1) try parsing as is
        try {
            bodyMap = new ObjectMapper().readValue(bodyMapString, HashMap.class);

            if(bodyMap.containsKey(VELOCITY)){
                bodyMap.put(VELOCITY, ScriptingUtil.getInstance().unescapeValue((String)bodyMap.get(VELOCITY), "\n"));
            }
        } catch (IOException e) {
            // 2) let's try escaping then parsing
            String escapedJsonValues = ScriptingUtil.getInstance().escapeJsonValues(bodyMapString, '\n');

            try {
                bodyMap = new ObjectMapper().readValue(escapedJsonValues, HashMap.class);

                if(bodyMap.containsKey(VELOCITY)){
                    bodyMap.put(VELOCITY, ScriptingUtil.getInstance().unescapeValue((String)bodyMap.get(VELOCITY), "\n"));
                }
            } catch (IOException e1) {
                bodyMap.put(VELOCITY, bodyMapString);
            }
        }

        return bodyMap;
    }



}
