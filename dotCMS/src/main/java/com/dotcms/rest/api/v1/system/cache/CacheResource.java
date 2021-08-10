package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.enterprise.cache.provider.CacheProviderAPIImpl;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.PortletID;
import com.google.common.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

/**
 * Cache Resource
 * @author jsanca
 */
@Path("/v1/caches")
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

                method = providerAPI.getClass().getMethod("getProvidersForRegion", new Class[] {String.class});
                method.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new DotStateException(e);
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

        return Response.ok(new ResponseEntityView(this.getProviders(group))).build();
    }

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

        return Response.ok(new ResponseEntityView(this.getProvider(provider, group))).build();
    }

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

        return Response.ok(new ResponseEntityView(
                this.getProvider(provider, group).getKeys(group))).build();
    }

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

        final Object obj = this.getProvider(provider, group).get(group, id);
        return Response.ok(new ResponseEntityView(obj == null? "NOPE" : obj)).build();
    }

    @NoCache
    @GET
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

        this.getProvider(provider, group).remove(group);
        return Response.ok(new ResponseEntityView("flushed")).build();
    }

    @NoCache
    @GET
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

        this.getProvider(provider, group).remove(group, id);
        return Response.ok(new ResponseEntityView("flushed")).build();
    }

    @NoCache
    @GET
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

        MaintenanceUtil.flushCache();
        return Response.ok(new ResponseEntityView("flushed all")).build();
    }
}
