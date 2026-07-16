package com.dotcms.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/cluster")
@Tag(name = "Cluster Management")
public class ClusterResource {

    private final WebResource webResource = new WebResource();

    /**
     * Returns a Map of the Cache Cluster Nodes Status
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws JSONException
     * @throws DotCacheException 
     * @throws DotSecurityException 
     */
    @GET
    @Path ("/getNodesStatus/{params:.*}")
    @Produces ("application/json")
    public Response getNodesInfo (@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params )
			throws DotDataException, JSONException, DotStateException, DotSecurityException, DotCacheException {

		new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.params(params)
				.rejectWhenNoUser(true)
				.requiredPortlet(PortletID.CONFIGURATION.toString())
				.init();


        final List<Server> pendingServers = new ArrayList<>(APILocator.getServerAPI().getAliveServers());
        final String myServerId = APILocator.getServerAPI().readServerId();
        
        final int maxWaitTime =  Config.getIntProperty("CLUSTER_SERVER_THREAD_SLEEP", Math.max(2000, pendingServers.size()*1000)) ;
        final Map<String, Serializable> info = new HashMap<>();
        info.put("myServerId", myServerId);
        final Map<String, Serializable> members = CacheLocator.getCacheAdministrator().getTransport().validateCacheInCluster(maxWaitTime);
        final ArrayList<Serializable> arrayOfServerResponses = new ArrayList<>();
        
        // if we got a response, remove from pending servers
        pendingServers.removeIf(s->members.containsKey(s.getServerId()));
        

        members.values().forEach(map->{
            arrayOfServerResponses.add(map);
        });
        
        pendingServers.forEach(server->arrayOfServerResponses.add(ClusterUtilProxy.createFailedJson(server)));
        
        info.put("clusterHealth", pendingServers.isEmpty() ? "green":"red");
        info.put("serverInfo", arrayOfServerResponses);

		
		
		

        return Response.ok(info).build();
    }



    /**
     * Returns a Map of the ES Cluster Nodes Status
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws JSONException
     */
    @GET
    @Path ("/getESConfigProperties/{params:.*}")
    @Produces ("application/json")
    public Response getESConfigProperties ( @Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params )
			throws DotDataException, JSONException {

        InitDataObject initData = webResource.init( params, request, response, false, PortletID.CONFIGURATION.toString() );
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

        JSONObject jsonNode = new JSONObject();
        ServerAPI serverAPI = APILocator.getServerAPI();

        String serverId = serverAPI.readServerId();
        Server server = serverAPI.getServer(serverId);
        String cachePort = ClusterFactory.getNextAvailablePort(serverId, ServerPort.CACHE_PORT);
        String esPort = ClusterFactory.getNextAvailablePort(serverId, ServerPort.ES_TRANSPORT_TCP_PORT);

        jsonNode.put("BIND_ADDRESS", server!=null&&UtilMethods.isSet(server.getIpAddress())?server.getIpAddress():"");
        jsonNode.put("CACHE_BINDPORT", server!=null&&UtilMethods.isSet(server.getCachePort())?server.getCachePort():cachePort);
        jsonNode.put("ES_TRANSPORT_TCP_PORT", server!=null&&UtilMethods.isSet(server.getEsTransportTcpPort())?server.getEsTransportTcpPort():esPort);

        return responseResource.response( jsonNode.toString() );

    }

    @GET
    @Path("/licenseRepoStatus")
    @Produces("application/json")
    public Response getLicenseRepoStatus(@Context HttpServletRequest request,
										 @Context final HttpServletResponse response,
										 @PathParam ("params") String params) throws DotDataException, JSONException {
        webResource.init(params, request, response,true, null);

        JSONObject json=new JSONObject();
        json.put("total", LicenseUtil.getLicenseRepoTotal());
        json.put("available", LicenseUtil.getLicenseRepoAvailableCount());

        return Response.ok(json.toString()).build();
    }
    /**
     * Remove server from cluster
     * @param request
     * @param params
     * @return
     */
    @POST
    @Path("/remove/{params:.*}")
    public Response removeFromCluster(@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("params") String params) {
        InitDataObject initData = webResource.init(params, request, response, true, PortletID.CONFIGURATION.toString());
        String serverId = initData.getParamsMap().get("serverid");
        try {
        	HibernateUtil.startTransaction();
            APILocator.getServerAPI().removeServerFromClusterTable(serverId);
            HibernateUtil.closeAndCommitTransaction();
        }
        catch(Exception ex) {
            Logger.error(this, "can't remove from cluster ",ex);
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e) {
                Logger.warn(this, "can't rollback", e);
            }
            return Response.serverError().build();
        } finally {
        	HibernateUtil.closeSessionSilently();
		}

        return Response.ok().build();
    }
    
    
    
    /**
     * sends a cluster ping which is recorded in the logs
     * 
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/test")
    public Response testCluster(@Context HttpServletRequest request, @Context final HttpServletResponse response) {


        new WebResource.InitBuilder(request, response).requiredPortlet(PortletID.CONFIGURATION.toString())
                        .requiredBackendUser(true).init();


        CacheLocator.getCacheAdministrator().getTransport().testCluster();



        return Response.ok().build();
    }
    
    
}
