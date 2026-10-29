package com.dotcms.rest.api.v1.system.redis;

import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientFactory;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.apps.view.SecretView;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.system.cache.CachedValues;
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

    /**
     * Pings the Redis server and returns its reply.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @return the Redis ping reply wrapped in a {@link ResponseEntityView}
     */
    @NoCache
    @GET
    @Path("/ping")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response ping(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.ping())).build();
    }

    /**
     * Sends a message to the Redis server and returns the echoed value.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param message  the message to echo
     * @return the echoed message wrapped in a {@link ResponseEntityView}
     */
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
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.echo(message))).build();
    }

    /**
     * Stores a value in Redis under the given key.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param setForm  the key and the value to store
     * @return the result of the set operation wrapped in a {@link ResponseEntityView}
     */
    @NoCache
    @PUT
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response set(@Context final HttpServletRequest request,
                      @Context final HttpServletResponse response,
                      final SetForm setForm) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(
                this.client.set(setForm.getKey(), setForm.getValue()))).build();
    }

    /**
     * Returns the value stored in Redis under the given key, as {@link #responseValue(String, Object)} writes it.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param key      the key to read
     * @return the stored value wrapped in a {@link ResponseEntityView}
     */
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
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(responseValue(key, this.client.get(key)))).build();
    }

    /**
     * Deletes the given key from Redis.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param key      the key to delete
     * @return the result of the delete operation wrapped in a {@link ResponseEntityView}
     */
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
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.delete(key))).build();
    }

    /////// HASHES
    /**
     * Stores a set of fields as a hash in Redis under the given key.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param setForm  the key and the fields to store
     * @return the result of the hash set operation wrapped in a {@link ResponseEntityView}
     */
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
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(
                this.client.setHash(setForm.getKey(), setForm.getFields()))).build();
    }

    /**
     * Returns the hash stored in Redis under the given key, as {@link #responseValue(String, Object)} writes it.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param key      the key of the hash to read
     * @return the hash fields and values wrapped in a {@link ResponseEntityView}
     */
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
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(responseValue(key, this.client.getHash(key)))).build();
    }

    /**
     * Deletes every field of the hash stored in Redis under the given key.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param key      the key of the hash to delete
     * @return the number of deleted fields, as {@code rowsAffected}, wrapped in a {@link ResponseEntityView}
     */
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
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();


        final Set<String> fields = this.client.fieldsHash(key);
        final long rowsAffected  = UtilMethods.isSet(fields)?
                this.client.deleteHash(key, fields.toArray(new String[]{})):0;

        return Response.ok(new ResponseEntityView(Map.of("rowsAffected", rowsAffected))).build();
    }

    /////// INCREMENT
    /**
     * Increments by one the counter stored in Redis under the given key and resumes the
     * asynchronous response with the new value.
     * Requires a CMS Administrator.
     *
     * @param request       the current {@link HttpServletRequest}
     * @param response      the current {@link HttpServletResponse}
     * @param asyncResponse the asynchronous response resumed with the incremented value
     * @param key           the key of the counter to increment
     */
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
                .requireAdmin(true)
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

    /**
     * Returns the current value of the counter stored in Redis under the given key.
     * Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @param key      the key of the counter to read
     * @return the counter value wrapped in a {@link ResponseEntityView}
     */
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
                .requireAdmin(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        return Response.ok(new ResponseEntityView(this.client.getIncrement(key))).build();
    }

    /**
     * Returns what the read routes send back for a stored value. The Redis cache shares this server, so a key it
     * wrote for the Apps secrets group is answered with the hidden-value mask, and any other value goes through
     * {@link CachedValues#forResponse(Object)}, which leaves out the credentials of users and the key of companies.
     *
     * @param key   the Redis key that was read
     * @param value the stored value, or {@code null}
     * @return the mask for an Apps secrets key, otherwise the value as {@link CachedValues#forResponse(Object)}
     *         returns it
     */
    private static Object responseValue(final String key, final Object value) {
        if (CachedValues.isAppsSecretsKey(key)) {
            return SecretView.SecretViewSerializer.HIDDEN_SECRET_MASK;
        }
        return CachedValues.forResponse(value);
    }

}
