package com.dotcms.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ClusterFactory;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotGuavaCacheAdministratorImpl;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;


@Path("/cluster")
public class ClusterResource extends WebResource {

	 /**
     * Returns a Map of the Cache Cluster Status
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
    @Path ("/getCacheClusterStatus/{params:.*}")
    @Produces ("application/json")
    public Response getCacheClusterStatus ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = init( params, true, request, false );
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        View view = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getView();
        JChannel channel = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getChannel();
        JSONObject jsonClusterStatusObject = new JSONObject();

        if(view!=null) {
        	List<Address> members = view.getMembers();
        	jsonClusterStatusObject.put( "clusterName", channel.getClusterName());
        	jsonClusterStatusObject.put( "open", channel.isOpen());
        	jsonClusterStatusObject.put( "numberOfNodes", members.size());
        	jsonClusterStatusObject.put( "address", channel.getAddressAsString());
        	jsonClusterStatusObject.put( "receivedBytes", channel.getReceivedBytes());
        	jsonClusterStatusObject.put( "receivedMessages", channel.getReceivedMessages());
        	jsonClusterStatusObject.put( "sentBytes", channel.getSentBytes());
        	jsonClusterStatusObject.put( "sentMessages", channel.getSentMessages());
        }


        return responseResource.response( jsonClusterStatusObject.toString() );

    }

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

        InitDataObject initData = init( params, true, request, false );
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        ServerAPI serverAPI = APILocator.getServerAPI();
        List<Server> servers = serverAPI.getAllServers();
        List<Address> members = new ArrayList<Address>();

        // JGroups Cache
        View view = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getView();
        JChannel channel = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getChannel();

        if(view!=null) {
        	members = view.getMembers();
        }

        // ES Clustering
        AdminClient esClient = null;
        try {
        	esClient = new ESClient().getClient().admin();
        }  catch (Exception e) {
        	Logger.error(ClusterResource.class, "Error getting ES Client", e);
        }

        JSONArray jsonNodes = new JSONArray();
        String myServerId = serverAPI.readServerId();

        for (Server server : servers) {
        	JSONObject jsonNode = new JSONObject();
    		jsonNode.put( "serverId", server.getServerId());
    		jsonNode.put( "ipAddress", server.getIpAddress());

    		Boolean cacheStatus = false;
    		Boolean esStatus = false;
    		String nodeCacheWholeAddr = server.getIpAddress() + ":" + server.getCachePort();
    		String nodeESWholeAddr = server.getIpAddress() + ":" + server.getEsTransportTcpPort();

    		for ( Address member : members ) {
    			PhysicalAddress physicalAddr = (PhysicalAddress)channel.downcall(new Event(Event.GET_PHYSICAL_ADDRESS, member));
    			IpAddress ipAddr = (IpAddress)physicalAddr;

    			if(nodeCacheWholeAddr.equals(ipAddr.toString()) || (nodeCacheWholeAddr.replace("localhost", "127.0.0.1").equals(ipAddr.toString()))
    					|| (nodeCacheWholeAddr.replace("127.0.0.1", "localhost").equals(ipAddr.toString()))) {
    				cacheStatus = true;
    				break;
    			}
    		}

    		if(esClient!=null) {
	    		NodesInfoRequest nodesReq = new NodesInfoRequest();
	    		ActionFuture<NodesInfoResponse> afNodesRes = esClient.cluster().nodesInfo(nodesReq);
	    		NodesInfoResponse nodesRes = afNodesRes.actionGet();
	    		NodeInfo[] esNodes = nodesRes.getNodes();

	    		for (NodeInfo nodeInfo : esNodes) {
					DiscoveryNode node = nodeInfo.getNode();
					String address = node.getAddress().toString();

					if(address.contains(nodeESWholeAddr) || address.contains(nodeESWholeAddr.replace("localhost", "127.0.0.1"))) {
						esStatus = true;
						break;
					}
				}
    		}

    		if(UtilMethods.isSet(server.getLastHeartBeat())) {
    			Date now = new Date();
    			long difference = now.getTime() - server.getLastHeartBeat().getTime();
    			difference /= 1000;
    			jsonNode.put("contacted", difference);
    		}

    		if(view==null && !myServerId.equals(server.getServerId())) {
    			jsonNode.put("cacheStatus", "N/A");
        		jsonNode.put("esStatus", "N/A");
    		} else {
    			jsonNode.put("cacheStatus", cacheStatus.toString());
        		jsonNode.put("esStatus", esStatus.toString());
    		}


    		jsonNode.put("myself", myServerId.equals(server.getServerId()));
    		jsonNode.put("cachePort", server.getCachePort());
    		jsonNode.put("esPort", server.getEsTransportTcpPort());


    		//Added to the response list
    		jsonNodes.add( jsonNode );
		}

        return responseResource.response( jsonNodes.toString() );

    }

    /**
     * Returns a Map of the ES Cluster Status
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
    @Path ("/getESClusterStatus/{params:.*}")
    @Produces ("application/json")
    public Response getESClusterStatus ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = init( params, true, request, false );
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

        AdminClient client=null;

        JSONObject jsonNode = new JSONObject();

        try {
        	client = new ESClient().getClient().admin();
        } catch (Exception e) {
        	Logger.error(ClusterResource.class, "Error getting ES Client", e);
        	jsonNode.put("error", e.getMessage());
        	return responseResource.response( jsonNode.toString() );
        }

		ClusterHealthRequest clusterReq = new ClusterHealthRequest();
		ActionFuture<ClusterHealthResponse> afClusterRes = client.cluster().health(clusterReq);
		ClusterHealthResponse clusterRes = afClusterRes.actionGet();


		jsonNode.put("clusterName", clusterRes.getClusterName());
		jsonNode.put("numberOfNodes", clusterRes.getNumberOfNodes());
		jsonNode.put("activeShards", clusterRes.getActiveShards());
		jsonNode.put("activePrimaryShards", clusterRes.getActivePrimaryShards());
		jsonNode.put("unasignedPrimaryShards", clusterRes.getUnassignedShards());
		ClusterHealthStatus clusterStatus = clusterRes.getStatus();
		jsonNode.put("status", clusterStatus);

        return responseResource.response( jsonNode.toString() );

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

        InitDataObject initData = init( params, true, request, false );
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

//        Iterator<String> keys = DotConfig.getKeys();
        JSONObject jsonNode = new JSONObject();
        ServerAPI serverAPI = APILocator.getServerAPI();

        String serverId = serverAPI.readServerId();
        Server server = serverAPI.getServer(serverId);
        String cachePort = ClusterFactory.getNextAvailablePort(serverId, ServerPort.CACHE_PORT);
        String esPort = ClusterFactory.getNextAvailablePort(serverId, ServerPort.ES_TRANSPORT_TCP_PORT);

        jsonNode.put("CACHE_PROTOCOL", "tcp");
        jsonNode.put("CACHE_BINDPORT", server!=null&&UtilMethods.isSet(server.getCachePort())?server.getCachePort():cachePort);
        jsonNode.put("CACHE_MULTICAST_ADDRESS", "228.10.10.10");
        jsonNode.put("CACHE_MULTICAST_PORT", "45589");
        jsonNode.put("ES_TRANSPORT_TCP_PORT", server!=null&&UtilMethods.isSet(server.getEsTransportTcpPort())?server.getEsTransportTcpPort():esPort);

        return responseResource.response( jsonNode.toString() );

    }

    /**
     * Wires a new node to the Cache and ES Cluster
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws JSONException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path ("/wirenode/{params:.*}")
    @Produces ("application/json")
    public Response wireNode ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {
        InitDataObject initData = init( params, true, request, false ); // TODO rejectWhenNoUser has to be true
        JSONObject jsonNode = new JSONObject();

        if(request.getContentType().startsWith(MediaType.APPLICATION_JSON)) {
            HashMap<String,String> map=new HashMap<String,String>();

            try {
            	String payload = IOUtils.toString(request.getInputStream());
	            JSONObject obj = new JSONObject(payload);

	            Iterator<String> keys = obj.keys();
	            while(keys.hasNext()) {
	                String key=keys.next();
	                Object value=obj.get(key);
	                map.put(key, value.toString());
	            }

	            ClusterFactory.addNodeToCluster(map, APILocator.getServerAPI().readServerId());

	            jsonNode.put("result", "OK");

            } catch (Exception e) {
				Logger.error(ClusterResource.class, "Error wiring a new node to the Cluster", e);
				jsonNode.put("result", "ERROR:" + e.getMessage());
				jsonNode.put("detail", e.getCause());
			}
        }

        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        return responseResource.response(jsonNode.toString());


    }



}
