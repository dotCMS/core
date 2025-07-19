package com.dotcms.rest.api.v1.system.redis;

import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientFactory;
import com.dotcms.rest.ResponseEntityMapStringObjectView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Redis Resource
 * @author jsanca
 */
@SwaggerCompliant(value = "System administration and configuration APIs", batch = 4)
@Tag(name = "System")
@VisibleForTesting // This is not expose, visible only for internal testing
@Path("/v1/redis")
public class RedisResource {

    private final WebResource webResource;
    private final RedisClient<String, Object> client;

    public RedisResource() {

        this(new WebResource(), RedisClientFactory.getClient("resource"));
    }

    @VisibleForTesting
    public RedisResource(final WebResource webResource,
                         final RedisClient<String, Object> client) {

        this.webResource = webResource;
        this.client      = client;
    }

    @Operation(
        summary = "Redis ping",
        description = "Tests Redis connectivity by sending a ping command"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Redis ping successful",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/ping")
    @Produces({MediaType.APPLICATION_JSON})
    public Response ping(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(this.client.ping())).build();
    }

    @Operation(
        summary = "Redis echo",
        description = "Tests Redis functionality by echoing a message back"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Message echoed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/echo/{message}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response echo(@Context final HttpServletRequest request,
                      @Context final HttpServletResponse response,
                      @Parameter(description = "Message to echo", required = true)
                      @PathParam("message") final String message) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(this.client.echo(message))).build();
    }

    @Operation(
        summary = "Set Redis key-value",
        description = "Sets a key-value pair in Redis"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Key-value set successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response set(@Context final HttpServletRequest request,
                      @Context final HttpServletResponse response,
                      @RequestBody(description = "Key-value data to set", 
                                 required = true,
                                 content = @Content(schema = @Schema(implementation = SetForm.class)))
                      final SetForm setForm) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(
                this.client.set(setForm.getKey(), setForm.getValue()))).build();
    }

    @Operation(
        summary = "Get Redis value by key",
        description = "Retrieves a value from Redis by its key"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Value retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Key not found",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @Parameter(description = "Redis key", required = true)
                        @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(this.client.get(key))).build();
    }

    @Operation(
        summary = "Delete Redis key",
        description = "Deletes a key and its value from Redis"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Key deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @DELETE
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response delete(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @Parameter(description = "Redis key to delete", required = true)
                        @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(this.client.delete(key))).build();
    }

    /////// HASHES
    @Operation(
        summary = "Set Redis hash",
        description = "Sets hash fields in Redis"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Hash set successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid hash data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @PUT
    @Path("/hash")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response setHash(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @RequestBody(description = "Hash data to set", 
                                   required = true,
                                   content = @Content(schema = @Schema(implementation = SetHashForm.class)))
                        final SetHashForm setForm) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(
                this.client.setHash(setForm.getKey(), setForm.getFields()))).build();
    }

    @Operation(
        summary = "Get Redis hash",
        description = "Retrieves all fields and values of a hash stored at key"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Hash retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Hash key not found",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/hash/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getHash(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @Parameter(description = "Redis hash key", required = true)
                        @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(this.client.getHash(key))).build();
    }

    @Operation(
        summary = "Delete Redis hash",
        description = "Deletes all fields of a hash stored at key"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Hash deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @DELETE
    @Path("/hash/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteHash(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           @Parameter(description = "Redis hash key to delete", required = true)
                           @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();


        final Set<String> fields = this.client.fieldsHash(key);
        final long rowsAffected  = UtilMethods.isSet(fields)?
                this.client.deleteHash(key, fields.toArray(new String[]{})):0;

        return Response.ok(new ResponseEntityView<>(Map.of("rowsAffected", rowsAffected))).build();
    }

    /////// INCREMENT
    @Operation(
        summary = "Increment Redis key (async)",
        description = "Asynchronously increments the value of a key by 1"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Key incremented successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @PUT
    @Path("/incr/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    public void incrementAsync(@Context final HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @Suspended final AsyncResponse asyncResponse,
                            @Parameter(description = "Redis key to increment", required = true)
                            @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        ResponseUtil.handleAsyncResponse(() -> {
            try {

                final Future<Long> resultFuture = this.client.incrementOneAsync(key);
                return Response.ok( new ResponseEntityView<>(resultFuture.get())).build();
            } catch (Exception e) {
                asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
            }
            return null;
        }, asyncResponse);
    }

    @Operation(
        summary = "Get Redis increment value",
        description = "Retrieves the current increment value for a key"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Increment value retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Key not found",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/incr/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getIncrement(@Context final HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @Parameter(description = "Redis increment key", required = true)
                            @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView<>(this.client.getIncrement(key))).build();
    }

}
