package com.dotcms.rest.api.v1.system.monitor;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.StringPool;

import java.util.List;
import java.util.Map;


@Path("/v1/{a:system-status|probes}")
public class MonitorResource {

    private static final int    INSUFFICIENT_STORAGE        = 507;
    private static final int    SERVICE_UNAVAILABLE         = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
    private static final int    FORBIDDEN                   = HttpServletResponse.SC_FORBIDDEN;

    @NoCache
    @GET
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @CloseDBIfOpened
    public Response statusCheck(final @Context HttpServletRequest request) throws Throwable {

        final MonitorHelper helper = new MonitorHelper(request);

        ResponseBuilder builder = null;
        if (!helper.accessGranted) {
            return Response.status(FORBIDDEN).build();
        }
        final MonitorStats stats = helper.getMonitorStats();

        if (helper.useExtendedFormat) {
            final Map<String, Object> map = new HashMap<>();
            map.put("serverID", stats.serverId);
            map.put("clusterID", stats.clusterId);
            map.put("dotCMSHealthy", stats.isDotCMSHealthy());
            map.put("frontendHealthy", stats.isFrontendHealthy());
            map.put("backendHealthy", stats.isBackendHealthy());

            final Map<String, Object> subsystems = new HashMap<>();
            subsystems.put("dbSelectHealthy", stats.subSystemStats.isDBHealthy);
            subsystems.put("indexLiveHealthy", stats.subSystemStats.isLiveIndexHealthy);
            subsystems.put("indexWorkingHealthy", stats.subSystemStats.isWorkingIndexHealthy);
            subsystems.put("cacheHealthy", stats.subSystemStats.isCacheHealthy);
            subsystems.put("localFSHealthy", stats.subSystemStats.isLocalFileSystemHealthy);
            subsystems.put("assetFSHealthy", stats.subSystemStats.isAssetFileSystemHealthy);
            map.put("subsystems", subsystems);
            return Response.ok(map, MediaType.APPLICATION_JSON).build();
        }

        if (stats.isDotCMSHealthy()) {
            return Response.ok(StringPool.BLANK, MediaType.APPLICATION_JSON).build();
        }

        if (!stats.isBackendHealthy() && stats.isFrontendHealthy()) {
            return Response.status(INSUFFICIENT_STORAGE).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.status(SERVICE_UNAVAILABLE).entity(StringPool.BLANK).type(MediaType.APPLICATION_JSON).build();


    }


}
    
    
    /**
     * This probe is lightweight - it checks if the server is up (by the time a request gets here it has
     * already run through the CMSFilter) and does a quick cache check to insure all is well.
     * 
     * @param request
     * @return
     * @throws Throwable
     */

    @GET
    @Path("/alive")
    @CloseDBIfOpened
    @Produces(MediaType.APPLICATION_JSON)
    public Response aliveCheck(final @Context HttpServletRequest request) throws Throwable {


        final MonitorHelper helper = new MonitorHelper(request);
        if(!helper.accessGranted) {
            return Response.status(FORBIDDEN).build();
        }
        //try this twice as it is an imperfect test
        if(helper.isCacheHealthy()) {
            return Response.ok().build();
        }
        if(helper.isCacheHealthy()) {
            return Response.ok().build();
        }
        
        return Response.status(SERVICE_UNAVAILABLE).build();
        
    }
    
    
    /**
     * This probe tests all the dotCMS subsystems and will return either a success or failure based on
     * the result. This is a valid readiness check as the request already runs through the CMSFilter
     * (url resolution, rules firing) before reaching here.
     * 
     * @param request
     * @return
     * @throws Throwable
     */
    @GET
    @Path("/ready")
    @CloseDBIfOpened
    @Produces(MediaType.APPLICATION_JSON)
    public Response readyCheck(final @Context HttpServletRequest request) throws Throwable {

        return startup(request);
    }
    
    /**
     * This resource tests that dotCMS has started and queries all subsystems before returning an ok.
     * 
     * @param request
     * @return
     * @throws Throwable
     */
    @GET
    @Path("/startup")
    @CloseDBIfOpened
    @Produces(MediaType.APPLICATION_JSON)
    public Response startup(final @Context HttpServletRequest request) throws Throwable {

        final MonitorHelper helper = new MonitorHelper(request);
        if(!helper.accessGranted) {
            return Response.status(FORBIDDEN).entity(Map.of()).build();
        }
        
        // this is set at the end of the InitServlet
        if(System.getProperty(WebKeys.DOTCMS_STARTED_UP)==null) {
            return Response.status(SERVICE_UNAVAILABLE).build();
        }
        
        if(!helper.getMonitorStats().isDotCMSHealthy()) {
            return Response.status(SERVICE_UNAVAILABLE).build();
        }
        
        return Response.ok().build();
        
    }
    
    
    
    
}
