package com.dotcms.rest.api.v1.system.redis;

import com.dotcms.cache.lettuce.MasterReplicaLettuceClient;
import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientProvider;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.util.PortletID;
import com.google.common.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Redis Resource
 * @author jsanca
 */
@Path("/v1/redis")
public class RedisResource {

    private final WebResource webResource;
    private final RedisClient<String, Object> client;

    public RedisResource() {

        this(new WebResource(), RedisClientProvider.getInstance());
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

}
