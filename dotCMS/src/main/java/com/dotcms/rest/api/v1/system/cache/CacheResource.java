package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.rest.ResponseEntityListStringView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
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
import org.glassfish.jersey.server.JSONP;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cache Resource for diff provides
 * @author jsanca
 */
@Path("/v1/caches")
@Tag(name = "Cache Management", description = "Cache provider management and operations")
public class CacheResource {

    private final WebResource webResource;
    private final CacheProviderAPIImpl providerAPI;
    private final CacheMaintenanceHelper cacheMaintenanceHelper;
    private Method method = null;

    public CacheResource() {

        this(new WebResource(),
             (CacheProviderAPIImpl) APILocator.getCacheProviderAPI(),
             new CacheMaintenanceHelper());
    }

    @VisibleForTesting
    public CacheResource(final WebResource webResource,
                         final CacheProviderAPIImpl providerAPI,
                         final CacheMaintenanceHelper cacheMaintenanceHelper) {

        this.webResource = webResource;
        this.providerAPI = providerAPI;
        this.cacheMaintenanceHelper = cacheMaintenanceHelper;
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
     * Flushes all caches across all providers, resets permission references,
     * and reloads PushPublishing filters. The provider path parameter is ignored —
     * all caches are flushed regardless.
     *
     *  @param request   {@link HttpServletRequest}
     *  @param response  {@link HttpServletResponse}
     *  @param provider {@link String} ignored — all providers are flushed
     * @return Response
     */
    @Operation(
            summary = "Flush all caches",
            description = "Flushes all caches across all providers, resets permission references, "
                    + "and reloads PushPublishing filters. The provider path parameter is ignored."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All caches flushed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires maintenance portlet access")
    })
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

        cacheMaintenanceHelper.flushAllCaches();
        return Response.ok(new ResponseEntityView("flushed all")).build();
    }


    /**
     * Returns an alphabetically sorted list of all cache region names.
     * Used to populate the flush-cache dropdown in the Cache tab.
     *
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return sorted list of region names
     */
    @Operation(
            summary = "List cache region names",
            description = "Returns an alphabetically sorted list of all cache region names. "
                    + "Used to populate the flush-cache dropdown in the Maintenance portlet Cache tab."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache regions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityListStringView.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires maintenance portlet access")
    })
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityListStringView listRegions(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, () -> "Listing cache regions");

        final Object[] caches = CacheLocator.getCacheIndexes();
        final List<String> regions = Stream.of(caches)
                .map(Object::toString)
                .sorted()
                .collect(Collectors.toList());

        return new ResponseEntityListStringView(regions);
    }

    /**
     * Returns JVM memory information and per-provider per-region cache statistics.
     * Used to render the Cache Stats section in the Maintenance portlet.
     *
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return cache statistics with memory and provider details
     */
    @Operation(
            summary = "Get cache statistics",
            description = "Returns JVM memory information and per-provider per-region cache statistics "
                    + "including hit counts, miss counts, and memory usage."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityCacheStatsView.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires maintenance portlet access")
    })
    @GET
    @Path("/stats")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityCacheStatsView getCacheStats(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        Logger.debug(this, () -> "Retrieving cache statistics");

        return new ResponseEntityCacheStatsView(buildCacheStatsView());
    }

    /**
     * Flushes a specific cache region across all providers, resets permission references,
     * and conditionally reloads PushPublishing filters. Pass {@code "all"} to flush all caches.
     *
     * @param request    {@link HttpServletRequest}
     * @param response   {@link HttpServletResponse}
     * @param regionName the cache region name or {@code "all"}
     * @return confirmation message
     */
    @Operation(
            summary = "Flush a cache region",
            description = "Flushes a specific cache region across all providers, resets permission "
                    + "references, and conditionally reloads PushPublishing filters. "
                    + "Pass 'all' to flush all caches."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache region flushed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - unknown cache region name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires maintenance portlet access")
    })
    @DELETE
    @Path("/region/{regionName: .*}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityStringView flushRegion(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @Parameter(description = "Cache region name (case-insensitive, as returned by "
                    + "GET /api/v1/caches) or 'all' to flush all caches",
                    required = true, example = "Permission")
            @PathParam("regionName") final String regionName) {

        new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.MAINTENANCE.toString().toLowerCase())
                .rejectWhenNoUser(true).init();

        if ("all".equalsIgnoreCase(regionName)) {
            Logger.info(this, "Flushing all caches");
            cacheMaintenanceHelper.flushAllCaches();
            return new ResponseEntityStringView("Flushed all caches");
        }

        final String canonical = cacheMaintenanceHelper.flushRegion(regionName);
        Logger.info(this, "Flushed cache region: " + canonical);
        return new ResponseEntityStringView("Flushed " + canonical);
    }

    /**
     * Builds the full cache statistics view from JVM runtime memory and cache provider stats.
     */
    private CacheStatsView buildCacheStatsView() {

        final long maxMemory = Runtime.getRuntime().maxMemory();
        final long allocatedMemory = Runtime.getRuntime().totalMemory();
        final long usedMemory = allocatedMemory - Runtime.getRuntime().freeMemory();
        final long freeMemory = maxMemory - usedMemory;

        final JvmMemoryView memory = JvmMemoryView.builder()
                .maxMemory(maxMemory)
                .allocatedMemory(allocatedMemory)
                .usedMemory(usedMemory)
                .freeMemory(freeMemory)
                .build();

        final List<CacheProviderStats> providerStatsList =
                CacheLocator.getCacheAdministrator().getCacheStatsList();

        final List<CacheProviderStatsView> providers = new ArrayList<>();
        for (final CacheProviderStats ps : providerStatsList) {

            final List<String> columns = new ArrayList<>(ps.getStatColumns());
            final List<Map<String, String>> stats = new ArrayList<>();

            for (final CacheStats cs : ps.getStats()) {
                final Map<String, String> row = new LinkedHashMap<>();
                for (final String col : ps.getStatColumns()) {
                    row.put(col, cs.getStatValue(col));
                }
                stats.add(row);
            }

            providers.add(CacheProviderStatsView.builder()
                    .providerName(ps.getProviderName())
                    .columns(columns)
                    .stats(stats)
                    .build());
        }

        String hostName = "localhost";
        try {
            hostName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            Logger.debug(this, "Unable to resolve hostname", e);
        }

        return CacheStatsView.builder()
                .clusterId(ClusterFactory.getClusterId())
                .serverId(APILocator.getServerAPI().readServerId())
                .serverName(hostName)
                .memory(memory)
                .providers(providers)
                .build();
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
