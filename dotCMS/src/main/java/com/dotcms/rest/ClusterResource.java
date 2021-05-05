package com.dotcms.rest;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.ClusterUtil;
import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.cluster.action.NodeStatusServerAction;
import com.dotcms.enterprise.cluster.action.ServerAction;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.util.CacheUtil;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import java.io.File;
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


@Path("/cluster")
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

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.requestAndResponse(request, response)
				.params(params)
				.rejectWhenNoUser(true)
				.requiredPortlet(PortletID.CONFIGURATION.toString())
				.init();


        
        ServerAPI serverAPI = APILocator.getServerAPI();
        final  List<Server> failedServers = new ArrayList<>(serverAPI.getAliveServers());
        final String myServerId = serverAPI.readServerId();
        
        int maxWaitTime =  Config.getIntProperty("CLUSTER_SERVER_THREAD_SLEEP", 2000) ;
        final Map<String, Serializable> info = new HashMap<>();
        info.put("myServerId", myServerId);
        final Map<String, Serializable> members = CacheLocator.getCacheAdministrator().getTransport().validateCacheInCluster( maxWaitTime);
        final ArrayList<Serializable> arrayOfServers = new ArrayList<>();
        
        failedServers.removeIf(s->members.containsKey(s.getServerId()));
        

        members.values().forEach(map->{
            arrayOfServers.add(map);
        });
        
        failedServers.forEach(server->arrayOfServers.add(ClusterUtil.createFailedJson(server)));
        
        info.put("clusterHealth", failedServers.isEmpty() ? "green":"red");
        info.put("serverInfo", arrayOfServers);

		
		
		

        return Response.ok(info).build();
    }

    /**
     * Returns a Map with the info of the Node with the given Id
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws JSONException
     */
    @GET
    @Path ("/getNodeStatus/{params:.*}")
    @Produces ("application/json")
    public Response getNodeInfo ( @Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params )
			throws DotDataException, JSONException {

        InitDataObject initData = webResource.init( params, request, response, false, PortletID.CONFIGURATION.toString() );

        Map<String, String> paramsMap = initData.getParamsMap();
		String remoteServerID = paramsMap.get("id");
		String localServerId = APILocator.getServerAPI().readServerId();

		if(UtilMethods.isSet(remoteServerID) && !remoteServerID.equals("undefined")) {
			
			ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
			
			NodeStatusServerAction nodeStatusServerAction = new NodeStatusServerAction();
			Long timeoutSeconds = new Long(1);
			
			ServerActionBean nodeStatusServerActionBean = 
					nodeStatusServerAction.getNewServerAction(localServerId, remoteServerID, timeoutSeconds);
			
			nodeStatusServerActionBean = 
					APILocator.getServerActionAPI().saveServerActionBean(nodeStatusServerActionBean);
			
			//Waits for 3 seconds in order server respond.
			int maxWaitTime = 
					timeoutSeconds.intValue() * 1000 + Config.getIntProperty("CLUSTER_SERVER_THREAD_SLEEP", 2000) ;
			int passedWaitTime = 0;
			
			//Trying to NOT wait whole time for returning the info.
			while (passedWaitTime <= maxWaitTime){
				try {
				    Thread.sleep(10);
				    passedWaitTime += 10;
				    
				    nodeStatusServerActionBean = 
				    		APILocator.getServerActionAPI().findServerActionBean(nodeStatusServerActionBean.getId());
				    
				    //No need to wait if we have all Action results. 
				    if(nodeStatusServerActionBean != null && nodeStatusServerActionBean.isCompleted()){
				    	passedWaitTime = maxWaitTime + 1;
				    }
				    
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				    passedWaitTime = maxWaitTime + 1;
				}
			}
			
			//If the we don't have the info after the timeout
			if(!nodeStatusServerActionBean.isCompleted()){
				nodeStatusServerActionBean.setCompleted(true);
				nodeStatusServerActionBean.setFailed(true);
				nodeStatusServerActionBean.setResponse(new JSONObject().put(ServerAction.ERROR_STATE, "Server did NOT respond on time"));
				APILocator.getServerActionAPI().saveServerActionBean(nodeStatusServerActionBean);
			}
			
			JSONObject jsonNodeStatusObject = null;
			
			//If the we have a failed job.
			if(nodeStatusServerActionBean.isFailed()){
				jsonNodeStatusObject = 
						ClusterUtilProxy.createFailedJson(APILocator.getServerAPI().getServer(nodeStatusServerActionBean.getServerId()));
		    	
			//If everything is OK.
			} else {
				jsonNodeStatusObject = 
		        		nodeStatusServerActionBean.getResponse().getJSONObject(NodeStatusServerAction.JSON_NODE_STATUS);
				
				//Check Test File Asset
				if(jsonNodeStatusObject.has("assetsStatus")
						&& jsonNodeStatusObject.getString("assetsStatus").equals("green")
						&& jsonNodeStatusObject.has("assetsTestPath")){
					
					//Get the file Name from the response.
					File testFile = new File(jsonNodeStatusObject.getString("assetsTestPath"));
					//If exist we need to check if we can delete it.
					if (testFile.exists()) {
						//If we can't delete it, it is a problem.
						if(!testFile.delete()){
							jsonNodeStatusObject.put("assetsStatus", "red");
							jsonNodeStatusObject.put("status", "red");
						}
					} else {
						jsonNodeStatusObject.put("assetsStatus", "red");
						jsonNodeStatusObject.put("status", "red");
					}
				}
			}
	        
			if(jsonNodeStatusObject != null){
				return responseResource.response( jsonNodeStatusObject.toString() );
			} else {
				return null;
			}
	        
		} else {
			return null;
		}

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
