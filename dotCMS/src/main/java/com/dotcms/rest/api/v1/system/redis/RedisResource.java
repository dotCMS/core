package com.dotcms.rest.api.v1.system.redis;

import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientFactory;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @NoCache
    @GET
    @Path("/ping")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response ping(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.ping())).build();
    }

    @NoCache
    @GET
    @Path("/echo/{message}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response echo(@Context final HttpServletRequest request,
                      @Context final HttpServletResponse response,
                      @PathParam("message") final String message) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.echo(message))).build();
    }

    @NoCache
    @PUT
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response set(@Context final HttpServletRequest request,
                      @Context final HttpServletResponse response,
                      final SetForm setForm) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(
                this.client.set(setForm.getKey(), setForm.getValue()))).build();
    }

    @NoCache
    @GET
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response get(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.get(key))).build();
    }

    @NoCache
    @DELETE
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response delete(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.delete(key))).build();
    }

    /////// HASHES
    @NoCache
    @PUT
    @Path("/hash")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response setHash(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        final SetHashForm setForm) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(
                this.client.setHash(setForm.getKey(), setForm.getFields()))).build();
    }

    @NoCache
    @GET
    @Path("/hash/{key}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getHash(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response,
                        @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.getHash(key))).build();
    }

    @NoCache
    @DELETE
    @Path("/hash/{key}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response deleteHash(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
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

        return Response.ok(new ResponseEntityView(Map.of("rowsAffected", rowsAffected))).build();
    }

    /////// INCREMENT
    @NoCache
    @PUT
    @Path("/incr/{key}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public void incrementAsync(@Context final HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @Suspended final AsyncResponse asyncResponse,
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
                return Response.ok( new ResponseEntityView(resultFuture.get())).build();
            } catch (Exception e) {
                asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
            }
            return null;
        }, asyncResponse);
    }

    @NoCache
    @GET
    @Path("/incr/{key}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getIncrement(@Context final HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @PathParam("key") final String key) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.getIncrement(key))).build();
    }

}
