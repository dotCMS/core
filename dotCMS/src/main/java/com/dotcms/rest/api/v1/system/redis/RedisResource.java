package com.dotcms.rest.api.v1.system.redis;

import com.dotcms.cache.lettuce.RedisClient;
import com.dotcms.cache.lettuce.RedisClientProvider;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotcms.dotpubsub.RedisPubSubImpl;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;

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
import java.util.Set;
import java.util.concurrent.Future;

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

        return Response.ok(new ResponseEntityView(CollectionsUtils.map("rowsAffected", rowsAffected))).build();
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

    @VisibleForTesting
    @NoCache
    @GET
    @Path("/test-subscribe/{channel}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response testSubscribe(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                 @PathParam("channel") final String channel) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        this.client.subscribe(msg -> Logger.info(this,
                "Receiving from redis on channel: " + channel +
                            ", msg = " + msg), channel);

        return Response.ok(new ResponseEntityView("Subscribed")).build();
    }

    @VisibleForTesting
    @NoCache
    @PUT
    @Path("/test-publish/{channel}/{message}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response testPublish(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @PathParam("channel") final String channel,
                                  @PathParam("message") final String message) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        this.client.publishMessage(message, channel);

        return Response.ok(new ResponseEntityView("Sent")).build();
    }

    @VisibleForTesting
    @NoCache
    @PUT
    @Path("/test-subscribe/{channel}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response testUnSubscribe(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @PathParam("channel") final String channel) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        this.client.unsubscribe(channel);

        return Response.ok(new ResponseEntityView("Unsubscribe")).build();
    }

    //////

    @VisibleForTesting
    final static DotPubSubProvider pubsub = new RedisPubSubImpl();

    @VisibleForTesting
    @NoCache
    @PUT
    @Path("/test-subpub-publish/{channel}/{message}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response testSubPubPublish(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @PathParam("channel") final String channel,
                                @PathParam("message") final String message) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        pubsub.publish(new DotPubSubEvent.Builder()
                .addPayload("message", message)
                .withTopic(channel)
                .build());

        return Response.ok(new ResponseEntityView("Sent")).build();
    }

    @VisibleForTesting
    @NoCache
    @PUT
    @Path("/test-subpub-subscribe/{channel}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response testSubPub(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response,
                               @PathParam("channel") final String channel) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        final DotPubSubTopic topic = new DotPubSubTopicImpl(channel);
        pubsub.subscribe(topic);

        return Response.ok(new ResponseEntityView("Subscribe")).build();
    }

    private class DotPubSubTopicImpl implements DotPubSubTopic {

        final String channel;

        public DotPubSubTopicImpl(final String channel) {

            this.channel = channel;
        }

        @Override
        public void notify(final DotPubSubEvent event) {

            Logger.info(this, "msg: "     + event.getMessage());
            Logger.info(this, "payload: " + event.getPayload().toString());
        }

        @Override
        public Comparable getKey() {
            return this.channel;
        }
    }
}
