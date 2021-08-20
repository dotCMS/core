package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.business.CloseDBIfOpened;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.jersey.server.JSONP;


@Path("/v1/system-status")
public class MonitorResource {

    private static final int    INSUFFICIENT_STORAGE        = 507;
    private static final int    SERVICE_UNAVAILABLE         = 503;
    private static final int    FORBIDDEN                   = 403;

    @NoCache
    @GET
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @CloseDBIfOpened
    public Response statusCheck(final @Context HttpServletRequest request) throws Throwable {
        // Cannot require authentication as we cannot assume db or other subsystems are functioning

        final MonitorHelper helper = new MonitorHelper(request);

        ResponseBuilder builder = null;
        if (helper.accessGranted) {
            final MonitorStats stats = helper.getMonitorStats();

            if (helper.useExtendedFormat) {
                final JSONObject jo = new JSONObject();
                jo.put("serverID", stats.serverId);
                jo.put("clusterID", stats.clusterId);
                jo.put("dotCMSHealthy", stats.isDotCMSHealthy());
                jo.put("frontendHealthy", stats.isFrontendHealthy());
                jo.put("backendHealthy", stats.isBackendHealthy());

                final JSONObject subsystems = new JSONObject();
                subsystems.put("dbSelectHealthy", stats.subSystemStats.isDBHealthy);
                subsystems.put("indexLiveHealthy", stats.subSystemStats.isLiveIndexHealthy);
                subsystems.put("indexWorkingHealthy", stats.subSystemStats.isWorkingIndexHealthy);
                subsystems.put("cacheHealthy", stats.subSystemStats.isCacheHealthy);
                subsystems.put("localFSHealthy", stats.subSystemStats.isLocalFileSystemHealthy);
                subsystems.put("assetFSHealthy", stats.subSystemStats.isAssetFileSystemHealthy);
                jo.put("subsystems", subsystems);

                builder = Response.ok(jo.toString(2), MediaType.APPLICATION_JSON);
            } else {
                if (stats.isDotCMSHealthy()) {
                    builder = Response.ok(StringPool.BLANK, MediaType.APPLICATION_JSON);
                } else if (!stats.isBackendHealthy() && stats.isFrontendHealthy()) {
                    builder = Response.status(INSUFFICIENT_STORAGE).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON);
                } else {
                    builder = Response.status(SERVICE_UNAVAILABLE).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON);
                }
            }
        }
        else {
            // Access is forbidden because IP is not in any range in ACL list
            builder = Response.status(FORBIDDEN).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON);
        }

        return builder.build();
    }
    
    /**
     * This resource tests a very simple case of querying data that should be in cache and returns
     * either success or failure result code.  This is a valid liveness check as the request already runs
     * through the CMSFilter (url resolution, rules firing) before reaching here.
     * 
     * @param request
     * @return
     * @throws Throwable
     */
    @GET
    @Path("/alive")
    @CloseDBIfOpened
    public Response aliveCheck(final @Context HttpServletRequest request) throws Throwable {
        // Cannot require authentication as we cannot assume db or other subsystems are functioning

        final MonitorHelper helper = new MonitorHelper(request);
        if(!helper.accessGranted) {
            return Response.status(FORBIDDEN).build();
        }
        
        
        return Response.status(200).build();
        
    }
    
    
    
}