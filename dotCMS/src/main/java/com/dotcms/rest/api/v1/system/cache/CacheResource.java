package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.workflow.ResponseEntityWorkflowHistoryCommentsView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.PortletID;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Cache Resource for diff provides
 * @author jsanca
 */
@Path("/v1/caches")
@Tag(name = "Cache Management", description = "Cache provider management and operations")
public class CacheResource {

    private final WebResource          webResource;
    private final CacheProviderAPIImpl providerAPI;
    private Method method = null;

    public CacheResource() {

        this(new WebResource(), (CacheProviderAPIImpl) APILocator.getCacheProviderAPI());
    }

    @VisibleForTesting
    public CacheResource(final WebResource webResource,
                         final CacheProviderAPIImpl providerAPI) {

        this.webResource = webResource;
        this.providerAPI = providerAPI;
    }

    private final List<CacheProvider> getProviders(final String group) {

        if (method == null) {

            try {

                method = providerAPI.getClass().getDeclaredMethod("getProvidersForRegion", new Class[] {String.class});
                method.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e) {
                try {

                    method = providerAPI.getClass().getDeclaredMethod("b", new Class[] {String.class});
                    method.setAccessible(true);
                } catch (NoSuchMethodException | SecurityException ex) {
                    throw new DotStateException(ex);
                }
            }
        }

        try {

            return (List<CacheProvider>) method.invoke(this.providerAPI, group);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new DotStateException(e);
        }
    }

    private final CacheProvider getProvider(final String group, final String provider) {

        final List<CacheProvider> providers = this.getProviders(group);
        for (final CacheProvider cache : providers) {

            if (cache.getName().equals(group)) {

                return cache;
            }
        }

        throw new DotStateException("Unable to find " + provider + " provider for :" + group);
    }

    /**
     * Returns the providers associated to a group
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     * @param group {@link String}
     * @return Response
     */
    @NoCache
    @GET
    @Path("/providers/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response showProviders(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @PathParam("group") final String group) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Showing cache providers, group: " + group);

        return Response.ok(new ResponseEntityView<>(this.getProviders(group))).build();
    }

    /**
     * Returns the provider associated to a group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     *
     * @return Response
     */
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response showProviders(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @PathParam("provider") final String provider,
                                  @PathParam("group") final String group) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Showing cache providers, group: " + group + ", provider = " + provider);

        return Response.ok(new ResponseEntityView(this.getProvider(provider, group))).build();
    }

    /**
     * Get keys for a provider and group
     *
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     *  @return Response
     */
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/keys/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getKeys(@Context final HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @PathParam("provider") final String provider,
                            @PathParam("group") final String group) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Showing cache Key providers, group: " + group + ", provider = " + provider);

        return Response.ok(new ResponseEntityView(
                this.getProvider(provider, group).getKeys(group))).build();
    }

    /**
     * Shows an specific object by id in cache provider and group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     *  @param id {@link String}
     *  @return Response
     */
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/object/{group: .*}/{id: .*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response showObject(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam("provider") final String provider,
                               @PathParam("group") final String group,
                               @PathParam("id") final String id) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Showing cache Object providers, group: " + group
                + ", provider = " + provider + ", id = " + id);

        final Object obj = this.getProvider(provider, group).get(group, id);
        return Response.ok(new ResponseEntityView(obj == null? "NOPE" : obj)).build();
    }

    /**
     * Show all object for a provider and group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     * @return Response
     */
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/objects/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response showObjects(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam("provider") final String provider,
                               @PathParam("group") final String group) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Showing cache Objects providers, group: " + group
                + ", provider = " + provider);

        /// todo: do paginator
        final Set<String> keys = this.getProvider(provider, group).getKeys(group);
        final Map<String, Object> objectMap = new TreeMap<>();
        for (final String key : keys) {

            final Object obj = this.getProvider(provider, group).get(group, key);
            objectMap.put(key, obj == null? "NOPE" : obj);
        }
        return Response.ok(new ResponseEntityView(objectMap)).build();
    }

    /**
     * Show all objects for a provider and group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     * @return Response
     */
    @NoCache
    @DELETE
    @Path("/provider/{provider: .*}/flush/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response flushGroup(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam("provider") final String provider,
                               @PathParam("group") final String group) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Deletes objects on cache providers, group: " + group
                + ", provider = " + provider);

        this.getProvider(provider, group).remove(group);
        return Response.ok(new ResponseEntityView("flushed")).build();
    }

    /**
     * Deletes an object for a provider and group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     * @return Response
     */
    @NoCache
    @DELETE
    @Path("/provider/{provider: .*}/flush/{group: .*}/{id: .*}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response flushObject(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @PathParam("provider") final String provider,
                                @PathParam("group") final String group,
                                @PathParam("id") final String id) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Deletes object on  cache providers, group: " + group
                + ", provider = " + provider);

        this.getProvider(provider, group).remove(group, id);
        return Response.ok(new ResponseEntityView("flushed")).build();
    }

    /**
     * Deletes an objects for a provider (will clean all group and generates a new key)
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     * @return Response
     */
    @NoCache
    @DELETE
    @Path("/provider/{provider: .*}/flush")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response flushAll(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @PathParam("provider") final String provider) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Deletes all objects on  cache provider = " + provider);

        MaintenanceUtil.flushCache();
        return Response.ok(new ResponseEntityView("flushed all")).build();
    }


    /**
     * Deletes the menu cache
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     * @return ResponseEntityStringView
     */
    @NoCache
    @DELETE
    @Path("/menucache")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "deleteMenuCache", summary = "Deletes the menu cache",
            description = "Just deletes the menu cache by request",
            tags = {"Maintenance"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "500", description = "General Error")
            }
    )
    public ResponseEntityStringView deleteMenuCache(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Deleting menu cache");

        MaintenanceUtil.deleteMenuCache();
        return new ResponseEntityStringView("flushed menucache");
    }
}
