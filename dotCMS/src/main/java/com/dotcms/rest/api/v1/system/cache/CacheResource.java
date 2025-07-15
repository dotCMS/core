package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotcms.rest.ResponseEntityMapStringObjectView;
import com.dotcms.rest.ResponseEntitySetStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.PortletID;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@SwaggerCompliant(value = "System administration and configuration APIs", batch = 4)
@Path("/v1/caches")
@Tag(name = "Cache Management")
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
    @Operation(
        summary = "Show cache providers for group",
        description = "Returns the providers associated to a group"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cache providers retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListCacheProviderView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/providers/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response showProviders(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @Parameter(description = "Cache group", required = true)
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
    @Operation(
        summary = "Show specific cache provider",
        description = "Returns the provider associated to a group"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cache provider retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityCacheProviderView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response showProviders(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  @Parameter(description = "Cache provider name", required = true)
                                  @PathParam("provider") final String provider,
                                  @Parameter(description = "Cache group", required = true)
                                  @PathParam("group") final String group) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Showing cache providers, group: " + group + ", provider = " + provider);

        return Response.ok(new ResponseEntityView<>(this.getProvider(provider, group))).build();
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
    @Operation(
        summary = "Get cache keys",
        description = "Get keys for a provider and group"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cache keys retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySetStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/keys/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getKeys(@Context final HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @Parameter(description = "Cache provider name", required = true)
                            @PathParam("provider") final String provider,
                            @Parameter(description = "Cache group", required = true)
                            @PathParam("group") final String group) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Showing cache Key providers, group: " + group + ", provider = " + provider);

        return Response.ok(
                        new ResponseEntitySetStringView(
                                this.getProvider(provider, group).getKeys(group)))
                .build();
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
    @Operation(
        summary = "Show cache object",
        description = "Shows a specific object by id in cache provider and group"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cache object retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityCacheObjectView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/object/{group: .*}/{id: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response showObject(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @Parameter(description = "Cache provider name", required = true)
                               @PathParam("provider") final String provider,
                               @Parameter(description = "Cache group", required = true)
                               @PathParam("group") final String group,
                               @Parameter(description = "Object ID", required = true)
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
        return Response.ok(new ResponseEntityView<>(obj == null? "NOPE" : obj)).build();
    }

    /**
     * Show all object for a provider and group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     * @return Response
     */
    @Operation(
        summary = "Show all cache objects",
        description = "Show all objects for a provider and group"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cache objects retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/provider/{provider: .*}/objects/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response showObjects(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @Parameter(description = "Cache provider name", required = true)
                               @PathParam("provider") final String provider,
                               @Parameter(description = "Cache group", required = true)
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
        return Response.ok(new ResponseEntityView<>(objectMap)).build();
    }

    /**
     * Show all objects for a provider and group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     * @return Response
     */
    @Operation(
        summary = "Flush cache group",
        description = "Flush all objects for a provider and group"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cache group flushed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @DELETE
    @Path("/provider/{provider: .*}/flush/{group: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response flushGroup(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @Parameter(description = "Cache provider name", required = true)
                               @PathParam("provider") final String provider,
                               @Parameter(description = "Cache group", required = true)
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
        return Response.ok(new ResponseEntityView<>("flushed")).build();
    }

    /**
     * Deletes an object for a provider and group
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     *  @param group {@link String}
     * @return Response
     */
    @Operation(
        summary = "Flush cache object",
        description = "Deletes a specific object for a provider and group"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cache object flushed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @DELETE
    @Path("/provider/{provider: .*}/flush/{group: .*}/{id: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response flushObject(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @Parameter(description = "Cache provider name", required = true)
                                @PathParam("provider") final String provider,
                                @Parameter(description = "Cache group", required = true)
                                @PathParam("group") final String group,
                                @Parameter(description = "Object ID", required = true)
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
        return Response.ok(new ResponseEntityView<>("flushed")).build();
    }

    /**
     * Deletes an objects for a provider (will clean all group and generates a new key)
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String}
     * @return Response
     */
    @Operation(
        summary = "Flush all cache for provider",
        description = "Deletes all objects for a provider (will clean all groups and generate a new key)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "All cache for provider flushed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - maintenance portlet access required",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @DELETE
    @Path("/provider/{provider: .*}/flush")
    @Produces({MediaType.APPLICATION_JSON})
    public Response flushAll(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                @Parameter(description = "Cache provider name", required = true)
                                @PathParam("provider") final String provider) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, ()-> "Deletes all objects on  cache provider = " + provider);

        MaintenanceUtil.flushCache();
        return Response.ok(new ResponseEntityView<>("flushed all")).build();
    }
}
