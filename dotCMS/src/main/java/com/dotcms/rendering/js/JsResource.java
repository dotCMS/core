package com.dotcms.rendering.js;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.cache.DotJSONCache;
import com.dotcms.cache.DotJSONCacheFactory;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotcms.rendering.js.proxy.JsUser;
import com.dotcms.rendering.velocity.viewtools.exception.DotToolException;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.PATCH;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.apache.velocity.exception.MethodInvocationException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyHashMap;

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
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements a rest endpoints the allows to run javascript code (js).
 * @author jsanca
 */
@Path("/js")
public class JsResource {

    public static final String IDENTIFIER = "identifier";
    private final MultiPartUtils multiPartUtils;
    private final WebResource webResource;
    @VisibleForTesting
    static final String JS_PATH = "/application/apijs";

    public JsResource() {
        this(new WebResource(), new MultiPartUtils());
    }

    @VisibleForTesting
    JsResource(final WebResource webResource, final MultiPartUtils multiPartUtils) {
        this.webResource   = webResource;
        this.multiPartUtils = multiPartUtils;
    }

    /**
     * Returns the output of a convention based "get.js" file, located under the given {folder} after being evaluated
     * using the javascript engine.
     *
     * "get.js" code determines whether the response is a JSON object or anything else (XML, text-plain).
     */
    @GET
    @Path("/{folder}/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        @PathParam("pathParam") final String pathParam, final Map<Object, Object> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.GET, bodyMap);
    }

    @GET
    @Path("/{folder}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response get(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                        @Context UriInfo uriInfo, @PathParam("folder") final String folderName,
                        final Map<Object, Object> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.GET, bodyMap);
    }

    /**
     * Returns the output of a convention based "post.js" file, located under the given {folder} after being evaluated
     * using the javascript engine.
     *
     * "post.js" code determines whether the response is a JSON object or anything else (XML, text-plain).
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
                               final Map<Object, Object> bodyMap) {

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
                               final Map<Object, Object> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.POST, bodyMap);
    }

    /**
     * Returns the output of a convention based "put.js" file, located under the given {folder} after being evaluated
     * using the javascript engine.
     *
     * "put.js" code determines whether the response is a JSON object or anything else (XML, text-plain).
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
                              final Map<Object, Object> bodyMap) {

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
                              final Map<Object, Object> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.PUT, bodyMap);
    }

    /**
     * Returns the output of a convention based "patch.js" file, located under the given {folder} after being evaluated
     * using the javascript engine.
     *
     * "patch.js" code determines whether the response is a JSON object or anything else (XML, text-plain).
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
                                final Map<Object, Object> bodyMap) {

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
                                final Map<Object, Object> bodyMap) {

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.PATCH, bodyMap);
    }

    /**
     * Returns the output of a convention based "delete.js" file, located under the given {folder} after being evaluated
     * using the javascript engine.
     *
     * "delete.js" code determines whether the response is a JSON object or anything else (XML, text-plain).
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
                                 final Map<Object, Object> requestJSONMap) {

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
                                 final Map<Object, Object> requestJSONMap) {

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
     * in a "javascript" property
     */
    @GET
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final String bodyMapString) {

        final Map<Object, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.GET, bodyMap);
    }

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
     * in a "javascript" property
     */
    @POST
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                final String bodyMapString) {

        final Map<Object, Object> bodyMap = parseBodyMap(bodyMapString);

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
     * in a "javascript" property
     */
    @PUT
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPut(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final String bodyMapString) {

        final Map<Object, Object> bodyMap = parseBodyMap(bodyMapString);

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
     * Same as {@link #patch} but supporting sending the javascript to be rendered embedded (properly escaped) in the JSON
     * in a "javascript" property
     */
    @PATCH
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPatch(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                 final String bodyMapString) {

        final Map<Object, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.PATCH, bodyMap);
    }


    /**
     * Same as {@link #delete} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "javascript" property
     */
    @DELETE
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicDelete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                  final @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                  final String bodyMapString) {

        final Map<Object, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.DELETE, bodyMap);
    }

    private Response processMultiPartRequest(final HttpServletRequest request, final HttpServletResponse response,
                                             final UriInfo uriInfo, final String folderName,
                                             final String pathParam,
                                             final HTTPMethod httpMethod,
                                             final FormDataMultiPart multipart) {
        try {
            final List<File> binaries = getBinariesFromMultipart(multipart);
            final Map<Object, Object> bodyMap = getBodyMapFromMultipart(multipart);

            return processRequest(request, response, uriInfo, folderName, pathParam, httpMethod, bodyMap,
                    binaries.toArray(new File[0]));
        }  catch(Exception e) {
            Logger.error(this,"Exception on JS endpoint. POST method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private void validateBodyMap(final Map bodyMap, final HTTPMethod httpMethod) {

        // if it is an update method (not get) and the
        if (UtilMethods.isSet(bodyMap) && bodyMap.containsKey(IDENTIFIER) &&
                UtilMethods.isSet(bodyMap.get(IDENTIFIER))
                // an non-existing identifier could be just on get or post, put, patch or delete needs an existing id.
                && httpMethod != HTTPMethod.GET && httpMethod != HTTPMethod.POST) {

            final Optional<ShortyId> shortyId = APILocator.getShortyAPI().getShorty(bodyMap.get(IDENTIFIER).toString());
            final boolean isIdentifier = shortyId.isPresent() && shortyId.get().type == ShortType.IDENTIFIER;

            if (!isIdentifier) {

                throw new DoesNotExistException("The identifier: " + bodyMap.get(IDENTIFIER) + " does not exists");
            }
        }
    }


    private Response processRequest(final HttpServletRequest request, final HttpServletResponse response,
                                    final UriInfo uriInfo, final String folderName,
                                    final String pathParam,
                                    final HTTPMethod httpMethod,
                                    final Map<Object, Object> bodyMap,
                                    final File...binaries) {

        try {

            this.validateBodyMap(bodyMap, httpMethod);

            final InitDataObject initDataObject = this.webResource.init
                    (null, request, response, false, null);

            final User user = initDataObject.getUser();
            final DotJSONCache cache = DotJSONCacheFactory.getCache(httpMethod);
            final Optional<DotJSON> dotJSONOptional = cache.get(request, user);

            if(dotJSONOptional.isPresent()) {
                return Response.ok(dotJSONOptional.get().getMap()).build();
            }

            final JavascriptReaderParams javascriptReaderParams = new JavascriptReaderParams.JavascriptReaderParamsBuilder()
                    .setBodyMap(bodyMap)
                    .setFolderName(folderName)
                    .setHttpMethod(httpMethod)
                    .setRequest(request)
                    .setUser(user)
                    .setPageMode(PageMode.get(request))
                    .build();

            final JavascriptReader javascriptReader = JavascriptReaderFactory.getJavascriptReader(UtilMethods.isSet(folderName));
            final Map queryParams = uriInfo.getQueryParameters();
            final Map<String, Object> contextParams = CollectionsUtils.map(
                    "pathParam", pathParam,
                    "queryParams", ProxyHashMap.from(queryParams),
                    "bodyMap", ProxyHashMap.from(bodyMap),
                    "binaries", ProxyArray.fromList(Arrays.asList(binaries)));

            try(Reader reader = javascriptReader.getJavaScriptReader(javascriptReaderParams)){
                return evalJavascript(request, response, reader, contextParams,
                        initDataObject.getUser(), cache);
            }
        }  catch(Exception e) {

            Logger.error(this,"Exception on Javascript endpoint. " + httpMethod.name() + "  method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private Response evalJavascript(final HttpServletRequest request, final HttpServletResponse response,
                                    final Reader javascriptReader, final Map<String, Object> contextParams,
                                    final User user, final DotJSONCache cache)
            throws Exception {

        final ScriptEngine scriptEngine = ScriptEngineFactory.getInstance().getEngine(ScriptEngineFactory.JAVASCRIPT_ENGINE);

        final Map<String,Object> context = new HashMap<>();
        contextParams.forEach(context::put);
        final DotJSON dotJSON = new DotJSON();
        context.put("dotJSON", dotJSON);
        context.put("user", new JsUser(user));

        try {

            final Object result = scriptEngine.eval(request, response, javascriptReader, context);

            if (Objects.nonNull(result)) {
                return JsResponseStrategyFactory.getInstance().get(result).apply(request, response, user, cache, context, result);
            }
        } catch(MethodInvocationException e) {

            if (e.getCause() instanceof DotToolException) {

                Logger.error(this,"Error evaluating javascript: " + (e.getCause()).getCause().getMessage());
                throw (Exception) (e.getCause()).getCause();
            }
        }

        return Response.serverError().build();
    }


    private String resultToString(final Object result) {
        if (result instanceof Map) {

            final Map map = Map.class.cast(result);
            if (map.containsKey("output")) {
                return map.get("output").toString();
            }
        }
        return result.toString();
    }

    private Map getBodyMapFromMultipart(final FormDataMultiPart multipart) throws IOException, JSONException {

        return this.multiPartUtils.getBodyMapFromMultipart(multipart);
    }

    private List<File> getBinariesFromMultipart(final FormDataMultiPart multipart) throws IOException {

        return this.multiPartUtils.getBinariesFromMultipart(multipart);
    }


    private Map<Object, Object> parseBodyMap(final String bodyMapString) {
        Map<Object, Object> bodyMap = new HashMap<>();

        // 1) try parsing as is
        try {
            bodyMap = new ObjectMapper().readValue(bodyMapString, HashMap.class);

            if(bodyMap.containsKey("javascript")){
                bodyMap.put("javascript", unescapeValue((String)bodyMap.get("javascript"), "\n"));
            }
        } catch (IOException e) {
            // 2) let's try escaping then parsing
            String escapedJsonValues = escapeJsonValues(bodyMapString, '\n');

            try {
                bodyMap = new ObjectMapper().readValue(escapedJsonValues, HashMap.class);

                if(bodyMap.containsKey("javascript")){
                    bodyMap.put("javascript", unescapeValue((String)bodyMap.get("javascript"), "\n"));
                }
            } catch (IOException e1) {
                bodyMap.put("javascript", bodyMapString);
            }
        }

        return bodyMap;
    }

    private final String ESCAPE_ME_VALUE= " THIS_ESCAPES_LINE_BREAKS ";

    private String escapeJsonValues(final String jsonStr, final char escapeFrom) {

        StringWriter sw = new StringWriter();
        char[] charArr = jsonStr.toCharArray();
        boolean inQuotes=false;
        for(int i=0;i <charArr.length;i++) {
            char c = charArr[i];
            if(c=='"' && charArr[i-1] != '\\') {
                inQuotes=!inQuotes;
            }
            if(inQuotes && c==escapeFrom) {
                sw.append(ESCAPE_ME_VALUE);
            }else {
                sw.append(c);
            }
        }
        return sw.toString();
    }

    private String unescapeValue(String escapedValue, final String escapeFrom) {
        return escapedValue.replace(ESCAPE_ME_VALUE, escapeFrom);
    }
}
