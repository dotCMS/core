package com.dotcms.rest.api.v1.system.monitor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.*;
import com.liferay.util.StringPool;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.json.JSONObject;


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
        builder.header("Access-Control-Expose-Headers", "Authorization");
        builder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
        return builder.build();
    }
}