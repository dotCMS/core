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
import java.util.ArrayList;
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
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.VelocityException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/vtl")
@Tag(name = "Templates", description = "Template design and management")
public class VTLResource {

    public static final String IDENTIFIER = "identifier";
    public static final String VELOCITY = "velocity";

    private static final String DYNAMIC_DESCRIPTION =
            "Evaluates Velocity (VTL) code supplied directly in the request body — no `.vtl` file on "
            + "disk is required — and returns the result. The code is read from a `velocity` property "
            + "of the JSON body (properly escaped), or the body may be the raw VTL itself.\n\n"
            + "The caller requires the **Scripting Developer** role.\n\n"
            + "**Response shape** is decided by the submitted code:\n"
            + "- If the code populates `$dotJSON` (e.g. `$dotJSON.put(\"key\", ...)`), the response is that "
            + "JSON object.\n"
            + "- Otherwise the raw evaluated output is returned, with the content type set by the script "
            + "(defaults to `text/plain`).\n\n"
            + "**Velocity errors** (syntax/parse errors, method-invocation failures, missing resources) are "
            + "reported as a `400` with a structured body so an automated caller can locate and fix the "
            + "offending code instead of receiving partial output. Application-level errors set by the "
            + "script via `$dotJSON.put(\"errors\", ...)` are also returned as `400`.\n\n"
            + "**Warnings** — dotCMS evaluates Velocity in non-strict mode, so an undefined reference "
            + "(`$noSuchVar`) renders as literal text and a method returning `null` produces no output. "
            + "These likely-typos are collected and, on a successful response, returned in the "
            + "`X-Dot-Velocity-Warnings` header (a JSON array); on a `400` they appear in the `warnings` "
            + "field of the body.";

    private static final String DYNAMIC_400_DESCRIPTION =
            "The submitted Velocity failed to parse or evaluate, or the script reported errors. The body "
            + "carries the Velocity error detail (message, error type, and line/column when available).";

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

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.GET, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.GET, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.POST, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.POST, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.PUT, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.PUT, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.PATCH, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.PATCH, false, bodyMap);
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

        return processRequest(request, response, uriInfo, folderName, pathParam, HTTPMethod.DELETE, false, requestJSONMap);
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

        return processRequest(request, response, uriInfo, folderName, null, HTTPMethod.DELETE, false, requestJSONMap);
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
    @Operation(operationId = "dynamicGet", summary = "Evaluate inline Velocity code (GET)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
    @GET
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicGet(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.GET, true, bodyMap);
    }

    /**
     * @deprecated This GET method accepts a request body, which is not standard HTTP practice.
     * Consider using POST for operations that require request bodies.
     */
    @Operation(operationId = "dynamicGetNoPath", summary = "Evaluate inline Velocity code (GET)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
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
    @Operation(operationId = "dynamicPost", summary = "Evaluate inline Velocity code (POST)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
    @POST
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPost(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.POST, true, bodyMap);
    }

    @Operation(operationId = "dynamicPostNoPath", summary = "Evaluate inline Velocity code (POST)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
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
    @Operation(operationId = "dynamicPut", summary = "Evaluate inline Velocity code (PUT)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
    @PUT
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPut(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                               @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                               final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.PUT, true, bodyMap);
    }

    @Operation(operationId = "dynamicPutNoPath", summary = "Evaluate inline Velocity code (PUT)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
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
    @Operation(operationId = "dynamicPatch", summary = "Evaluate inline Velocity code (PATCH)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
    @PATCH
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicPatch(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                 final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.PATCH, true, bodyMap);
    }


    /**
     * Same as {@link #delete} but supporting sending the velocity to be rendered embedded (properly escaped) in the JSON
     * in a "velocity" property
     */
    @Operation(operationId = "dynamicDelete", summary = "Evaluate inline Velocity code (DELETE)",
            description = DYNAMIC_DESCRIPTION, tags = {"Templates"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Velocity evaluated successfully; body is the "
                    + "raw output or the JSON object produced by the script"),
            @ApiResponse(responseCode = "400", description = DYNAMIC_400_DESCRIPTION,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VelocityErrorResponseView.class))),
            @ApiResponse(responseCode = "403", description = "User lacks the Scripting Developer role")
    })
    @DELETE
    @Path("/dynamic/{pathParam:.*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response dynamicDelete(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                  final @Context UriInfo uriInfo, @PathParam("pathParam") final String pathParam,
                                  final String bodyMapString) {

        final Map<String, Object> bodyMap = parseBodyMap(bodyMapString);

        return processRequest(request, response, uriInfo, null, pathParam, HTTPMethod.DELETE, true, bodyMap);
    }

    private Response processMultiPartRequest(final HttpServletRequest request, final HttpServletResponse response,
                                    final UriInfo uriInfo, final String folderName,
                                    final String pathParam,
                                    final HTTPMethod httpMethod,
                                             final FormDataMultiPart multipart) {
        try {
            final List<File> binaries = getBinariesFromMultipart(multipart);
            final Map<String, Object> bodyMap = getBodyMapFromMultipart(multipart);

            return processRequest(request, response, uriInfo, folderName, pathParam, httpMethod, false, bodyMap,
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
                                    final boolean dynamic,
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
                    initDataObject.getUser(), cache, dynamic);
            }
        }  catch(Exception e) {
            Logger.error(this,"Exception on VTL endpoint. GET method: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private Response evalVelocity(final HttpServletRequest request, final HttpServletResponse response,
                                  final Reader velocityReader, final Map<String, Object> contextParams,
                                  final User user, final DotJSONCache cache, final boolean dynamic)
            throws Exception {
        final org.apache.velocity.context.Context context = VelocityUtil.getInstance().getContext(request, response);
        contextParams.forEach(context::put);
        context.put("dotJSON", new DotJSON());

        // For the /dynamic endpoints, attach a per-evaluation handler that collects invalid
        // references (undefined vars, null method results) as warnings. dotCMS runs Velocity in
        // non-strict mode, so these otherwise fail silently — catching them here helps a caller
        // spot typos. The handler never substitutes a value, so output is unchanged.
        final CollectingInvalidReferenceHandler warningsHandler =
                dynamic ? new CollectingInvalidReferenceHandler() : null;
        if (warningsHandler != null) {
            final EventCartridge eventCartridge = new EventCartridge();
            eventCartridge.addEventHandler(warningsHandler);
            eventCartridge.attachToContext(context);
        }

        final StringWriter evalResult = new StringWriter();
        // A meaningful log tag so parse errors reference the submitted script rather than an empty name.
        final String logTag = dynamic ? "dynamic velocity" : "";

        try {
            VelocityUtil.getEngine().evaluate(context, evalResult, logTag, velocityReader);
        } catch(MethodInvocationException e) {
            // For the /dynamic endpoints the caller owns the submitted code, so surface the Velocity
            // error instead of returning whatever partial output was rendered before it failed.
            if (dynamic) {
                return velocityErrorResponse(e, warningsHandler);
            }
            if(e.getCause() instanceof DotToolException) {
                Logger.error(this,"Error evaluating velocity: " + (e.getCause()).getCause().getMessage());
                throw (Exception) (e.getCause()).getCause();
            }
        } catch(VelocityException e) {
            // Parse errors, resource-not-found and other engine failures propagate as-is for the
            // convention-based endpoints, but the /dynamic endpoints report them so the caller can fix
            // the code it submitted rather than getting a generic 500.
            if (dynamic) {
                return velocityErrorResponse(e, warningsHandler);
            }
            throw e;
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

            final Response.ResponseBuilder builder = UtilMethods.isSet(contentType)
                    ? Response.ok(evalResult.toString()).type(contentType)
                    : Response.ok(evalResult.toString()).type(MediaType.TEXT_PLAIN_TYPE);
            return withWarningsHeader(builder, warningsHandler).build();

        } else {
            // let's add it to cache
            if(UtilMethods.isSet(dotJSON.get("errors"))) {
                return Response.status(Response.Status.BAD_REQUEST).entity(dotJSON.get("errors")).build();
            }

            cache.add(request, user, dotJSON);
            return withWarningsHeader(Response.ok(dotJSON.getMap()), warningsHandler).build();
        }
    }

    /**
     * Maximum serialized length of the {@code X-Dot-Velocity-Warnings} header value.
     *
     * <p>Tomcat's default {@code maxHttpHeaderSize} is 8 KB for the whole header block, and a
     * reverse proxy may enforce a tighter limit. {@link CollectingInvalidReferenceHandler#MAX_WARNINGS}
     * warnings carrying long chained references serialize to well over that, which would make the
     * container reject or truncate an otherwise successful 200. Cap the value at a conservative
     * budget that leaves room for the rest of the response headers.</p>
     */
    private static final int MAX_WARNINGS_HEADER_LENGTH = 4096;

    /**
     * Attaches collected Velocity warnings to a successful response as the {@code X-Dot-Velocity-Warnings}
     * header (JSON array), keeping the response body byte-for-byte the script's output. No-op when
     * there are no warnings or the handler is absent (non-dynamic requests).
     *
     * <p>The serialized value is bounded by {@link #MAX_WARNINGS_HEADER_LENGTH}: warnings are dropped
     * from the tail until the JSON fits, and a trailing marker records how many were omitted so a
     * caller never mistakes a truncated list for the complete one. The full set is always logged.</p>
     */
    private Response.ResponseBuilder withWarningsHeader(final Response.ResponseBuilder builder,
                                                        final CollectingInvalidReferenceHandler warningsHandler) {
        if (warningsHandler == null || warningsHandler.getWarnings().isEmpty()) {
            return builder;
        }
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final List<VelocityWarningView> warnings = warningsHandler.getWarnings();
            String serialized = objectMapper.writeValueAsString(warnings);

            if (serialized.length() > MAX_WARNINGS_HEADER_LENGTH) {
                // Drop from the tail until the payload (plus its truncation marker) fits. The
                // earliest warnings are the most useful — they point at the first mistake.
                final List<VelocityWarningView> kept = new ArrayList<>(warnings);
                String candidate = serialized;
                while (!kept.isEmpty() && candidate.length() > MAX_WARNINGS_HEADER_LENGTH) {
                    kept.remove(kept.size() - 1);
                    final List<Object> withMarker = new ArrayList<>(kept);
                    withMarker.add(Map.of(
                            "type", "TRUNCATED",
                            "message", (warnings.size() - kept.size())
                                    + " additional warning(s) omitted to stay within the response "
                                    + "header size limit; see the server log for the full list"));
                    candidate = objectMapper.writeValueAsString(withMarker);
                }
                Logger.warn(this, "Velocity warnings header truncated: kept " + kept.size()
                        + " of " + warnings.size() + " warnings. Full list: " + warnings);
                serialized = candidate;
            }

            return builder.header("X-Dot-Velocity-Warnings", serialized);
        } catch (final IOException e) {
            Logger.warn(this, "Unable to serialize Velocity warnings header: " + e.getMessage());
            return builder;
        }
    }

    /**
     * Builds a {@code 400 Bad Request} carrying a structured description of a Velocity error, so the
     * caller (typically an automated agent that submitted the VTL) can locate and fix the offending
     * code instead of receiving partial output or a generic {@code 500}.
     *
     * <p>The {@code errorType} is normalized to the public Velocity exception (dotCMS-internal
     * subclasses such as {@code PreviewEditParseErrorException} are reported as their
     * {@link ParseErrorException} parent). {@code message} is a concise one-liner; the full engine
     * output — including the exhaustive grammar-token list a parse error carries — is kept in
     * {@code detail}. Any warnings collected before the failure are included too.</p>
     *
     * @param e the Velocity engine exception thrown while evaluating the submitted code
     * @param warningsHandler collected invalid-reference warnings, or {@code null}
     * @return a bad-request {@link Response} with a {@link VelocityErrorResponseView} body
     */
    private Response velocityErrorResponse(final VelocityException e,
                                           final CollectingInvalidReferenceHandler warningsHandler) {
        Logger.warn(this, "Velocity error on /dynamic endpoint: " + e.getMessage());

        final String fullMessage = e.getMessage();
        String templateName = null;
        Integer line = null;
        Integer column = null;

        if (e instanceof ParseErrorException) {
            final ParseErrorException parseError = (ParseErrorException) e;
            templateName = parseError.getTemplateName();
            line = parseError.getLineNumber() > 0 ? parseError.getLineNumber() : null;
            column = parseError.getColumnNumber() > 0 ? parseError.getColumnNumber() : null;
        } else if (e instanceof MethodInvocationException) {
            final MethodInvocationException methodError = (MethodInvocationException) e;
            templateName = methodError.getTemplateName();
            line = methodError.getLineNumber() > 0 ? methodError.getLineNumber() : null;
            column = methodError.getColumnNumber() > 0 ? methodError.getColumnNumber() : null;
        }

        // The actionable message for tool/runtime failures lives on the root cause; use it as the
        // concise summary when present.
        final String rootCauseMessage = (e instanceof MethodInvocationException && e.getCause() != null
                && UtilMethods.isSet(e.getCause().getMessage())) ? e.getCause().getMessage() : null;

        final String summary = rootCauseMessage != null ? firstLine(rootCauseMessage) : firstLine(fullMessage);
        // Only keep `detail` when it adds something beyond the one-line summary (parse errors do).
        final String detail = (fullMessage != null && !fullMessage.trim().equals(summary)) ? fullMessage : null;

        final VelocityErrorView error = new VelocityErrorView(summary, normalizeErrorType(e),
                UtilMethods.isSet(templateName) ? templateName : null, line, column, detail);
        final List<VelocityErrorView> errors = new ArrayList<>();
        errors.add(error);

        final List<VelocityWarningView> warnings =
                warningsHandler != null ? warningsHandler.getWarnings() : List.of();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new VelocityErrorResponseView(errors, warnings))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    /**
     * Reports the public Velocity exception name rather than a dotCMS-internal subclass. For example
     * {@code PreviewEditParseErrorException} — an internal wrapper that only marks preview/edit mode —
     * is reported as {@code ParseErrorException}, which is meaningful to an API consumer.
     */
    private static String normalizeErrorType(final VelocityException e) {
        if (e instanceof ParseErrorException) {
            return ParseErrorException.class.getSimpleName();
        }
        if (e instanceof MethodInvocationException) {
            return MethodInvocationException.class.getSimpleName();
        }
        return e.getClass().getSimpleName();
    }

    /** Returns the first non-blank line of a (possibly multi-line) message, trimmed. */
    private static String firstLine(final String message) {
        if (message == null) {
            return "";
        }
        for (final String line : message.split("\\r?\\n")) {
            if (UtilMethods.isSet(line.trim())) {
                return line.trim();
            }
        }
        return message.trim();
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
