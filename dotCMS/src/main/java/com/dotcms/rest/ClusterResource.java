package com.dotcms.rest;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.cluster.action.NodeStatusServerAction;
import com.dotcms.enterprise.cluster.action.ServerAction;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.AdminClient;
import com.dotcms.repackage.org.jgroups.Address;
import com.dotcms.repackage.org.jgroups.JChannel;
import com.dotcms.repackage.org.jgroups.View;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.jgroups.JGroupsCacheTransport;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;


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
     * @throws DotSecurityException
     * @throws JSONException
     */
    @GET
    @Path ("/getNodesStatus/{params:.*}")
    @Produces ("application/json")
    public Response getNodesInfo ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init( params, true, request, false, "9");
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        
        ServerAPI serverAPI = APILocator.getServerAPI();
        List<Server> servers = serverAPI.getAliveServers();
        String myServerId = serverAPI.readServerId();

        List<ServerActionBean> actionBeans = new ArrayList<ServerActionBean>();
        List<ServerActionBean> resultActionBeans = new ArrayList<ServerActionBean>();
        JSONArray jsonNodes = new JSONArray();
        
        NodeStatusServerAction nodeStatusServerAction = new NodeStatusServerAction();
        Long timeoutSeconds = new Long(1);
		
		for (Server server : servers) {
			
			ServerActionBean nodeStatusServerActionBean = 
					nodeStatusServerAction.getNewServerAction(myServerId, server.getServerId(), timeoutSeconds);
			
			nodeStatusServerActionBean = 
					APILocator.getServerActionAPI().saveServerActionBean(nodeStatusServerActionBean);
			
			actionBeans.add(nodeStatusServerActionBean);
		}
		
		//Waits for 3 seconds in order server respond.
		int maxWaitTime = 
				timeoutSeconds.intValue() * 1000 + Config.getIntProperty("CLUSTER_SERVER_THREAD_SLEEP", 2000) ;
		int passedWaitTime = 0;
		
		//Trying to NOT wait whole time for returning the info.
		while (passedWaitTime <= maxWaitTime){
			try {
			    Thread.sleep(10);
			    passedWaitTime += 10;
			    
			    resultActionBeans = new ArrayList<ServerActionBean>();
			    
			    //Iterates over the Actions in order to see if we have it all.
			    for (ServerActionBean actionBean : actionBeans) {
			    	ServerActionBean resultActionBean = 
				    		APILocator.getServerActionAPI().findServerActionBean(actionBean.getId());
			    	
			    	//Add the ActionBean to the list of results.
			    	if(resultActionBean.isCompleted()){
			    		resultActionBeans.add(resultActionBean);
			    	}
				}
			    
			    //No need to wait if we have all Action results. 
			    if(resultActionBeans.size() == servers.size()){
			    	passedWaitTime = maxWaitTime + 1;
			    }
			    
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			    passedWaitTime = maxWaitTime + 1;
			}
		}
		
		//If some of the server didn't pick up the action, means they are down.
		if(resultActionBeans.size() != actionBeans.size()){
			//Need to find out which is missing?
			for(ServerActionBean actionBean : actionBeans){
				boolean isMissing = true;
				for(ServerActionBean resultActionBean : resultActionBeans){
					if(resultActionBean.getId().equals(actionBean.getId())){
						isMissing = false;
					}
				}
				//If the actionBean wasn't pick up.
				if(isMissing){
					//We need to save it as failed.
					actionBean.setCompleted(true);
					actionBean.setFailed(true);
					actionBean.setResponse(new JSONObject().put(ServerAction.ERROR_STATE, "Server did NOT respond on time"));
					APILocator.getServerActionAPI().saveServerActionBean(actionBean);
					
					//Add it to the results.
					resultActionBeans.add(actionBean);
				}
			}
		}
		
		//Iterate over all the results gathered.
		for (ServerActionBean resultActionBean : resultActionBeans) {
			JSONObject jsonNodeStatusObject = null;
			
			//If the result is failed we need to gather the info available.
			if(resultActionBean.isFailed()){
				Logger.error(ClusterResource.class, 
						"Error trying to get Node Status for server " + resultActionBean.getServerId());
				
				jsonNodeStatusObject = 
						ClusterUtilProxy.createFailedJson(APILocator.getServerAPI().getServer(resultActionBean.getServerId()));
			
		    //If the result is OK we need to get the response object.
			} else {
				jsonNodeStatusObject = resultActionBean.getResponse().getJSONObject(NodeStatusServerAction.JSON_NODE_STATUS);
				jsonNodeStatusObject.put("myself", myServerId.equals(resultActionBean.getServerId()));
				
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
			
			//Add the status of the node to the list of other nodes.
			if(jsonNodeStatusObject != null){
				jsonNodes.add( jsonNodeStatusObject );
			}
		}	

        return responseResource.response( jsonNodes.toString() );
    }

    /**
     * Returns a Map with the info of the Node with the given Id
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws JSONException
     */
    @GET
    @Path ("/getNodeStatus/{params:.*}")
    @Produces ("application/json")
    public Response getNodeInfo ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init( params, true, request, false, "9" );

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
     * @throws DotSecurityException
     * @throws JSONException
     */
    @GET
    @Path ("/getESConfigProperties/{params:.*}")
    @Produces ("application/json")
    public Response getESConfigProperties ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init( params, true, request, false, "9" );
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
    public Response getLicenseRepoStatus(@Context HttpServletRequest request, @PathParam ("params") String params) throws DotDataException, JSONException {
        webResource.init(params, true, request, true, null);

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
    public Response removeFromCluster(@Context HttpServletRequest request, @PathParam("params") String params) {
        InitDataObject initData = webResource.init(params, true, request, true, "9");
        String serverId = initData.getParamsMap().get("serverid");
        try {
        	HibernateUtil.startTransaction();
            APILocator.getServerAPI().removeServerFromClusterTable(serverId);
            HibernateUtil.commitTransaction();
        }
        catch(Exception ex) {
            Logger.error(this, "can't remove from cluster ",ex);
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e) {
                Logger.warn(this, "can't rollback", e);
            }
            return Response.serverError().build();
        }
        
        return Response.ok().build();
    }
}
