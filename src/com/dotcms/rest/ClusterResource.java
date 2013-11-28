package com.dotcms.rest;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;

import com.dotcms.cluster.bean.ESProperty;
import com.dotcms.cluster.business.ClusterFactory;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotGuavaCacheAdministratorImpl;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DotConfig;
import com.dotmarketing.util.Logger;
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
//        View view = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getView();
//        JChannel channel = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getChannel();
        JSONObject jsonClusterStatusObject = new JSONObject();

//        if(view!=null) {
//        	List<Address> members = view.getMembers();
//        	jsonClusterStatusObject.put( "clusterName", channel.getClusterName());
//        	jsonClusterStatusObject.put( "open", channel.isOpen());
//        	jsonClusterStatusObject.put( "numerOfNodes", members.size());
//        	jsonClusterStatusObject.put( "address", channel.getAddressAsString());
//        	jsonClusterStatusObject.put( "receivedBytes", channel.getReceivedBytes());
//        	jsonClusterStatusObject.put( "receivedMessages", channel.getReceivedMessages());
//        	jsonClusterStatusObject.put( "sentBytes", channel.getSentBytes());
//        	jsonClusterStatusObject.put( "sentMessages", channel.getSentMessages());
//        }


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
    @Path ("/getCacheNodesStatus/{params:.*}")
    @Produces ("application/json")
    public Response getNodesInfo ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = init( params, true, request, false );
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
//        View view = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getView();
//        Channel channel = ((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).getChannel();
        JSONArray jsonNodes = new JSONArray();

//        if(view!=null) {
//
//        	List<Address> members = view.getMembers();
//
//        	for ( Address member : members ) {
//
//        		PhysicalAddress physicalAddr = (PhysicalAddress)channel.downcall(new Event(Event.GET_PHYSICAL_ADDRESS, member));
//        		IpAddress ipAddr = (IpAddress)physicalAddr;
//
//        		JSONObject jsonNode = new JSONObject();
//        		jsonNode.put( "id", member.toString());
//        		jsonNode.put( "ip", ipAddr.toString());
//        		//Added to the response list
//        		jsonNodes.add( jsonNode );
//        	}
//        }

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
		String serverName = request.getServerName();
		URL urlE = null;
		String content = "";

		try {
			urlE = new URL("http://"+serverName+":9200/_cluster/health?pretty=true");
			content = IOUtils.toString(urlE.openStream());

		} catch (Exception e) {
			Logger.error(getClass(), e.getMessage(), e);
		}

        return responseResource.response( content.toString() );

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
    @Path ("/getESNodesStatus/{params:.*}")
    @Produces ("application/json")
    public Response getESClusterNodesStatus ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = init( params, true, request, false );
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
		String serverName = request.getServerName();
		URL urlE = null;
		String content = "";

		try {
			urlE = new URL("http://"+serverName+":9200/_cluster/nodes");
			content = IOUtils.toString(urlE.openStream());

		} catch (Exception e) {
			Logger.error(getClass(), e.getMessage(), e);
		}

        return responseResource.response( content.toString() );

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

        JSONObject clusterProps = new JSONObject();
        Iterator<String> keys = DotConfig.getKeys();

        while ( keys.hasNext() ) {
        	String key = keys.next();
        	clusterProps.put( key, DotConfig.getStringProperty(key));
		}

        return responseResource.response( clusterProps.toString() );

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
    @POST

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path ("/updateESConfigProperties/{params:.*}")
    @Produces ("application/json")
    public String updateESConfigProperties ( @Context HttpServletRequest request, @FormParam("accept") String accept ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

//        InitDataObject initData = init( params, true, request, false );
//        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );


        JSONObject clusterProps = new JSONObject();
        Iterator<String> keys = DotConfig.getKeys();

        while ( keys.hasNext() ) {
        	String key = keys.next();
        	clusterProps.put( key, DotConfig.getStringProperty(key));
		}

//        return responseResource.response( clusterProps.toString() );
        return "true";

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

        if(request.getContentType().startsWith(MediaType.APPLICATION_JSON)) {
            HashMap<String,String> map=new HashMap<String,String>();

            try {
	            JSONObject obj = new JSONObject(IOUtils.toString(request.getInputStream()));

	            Iterator<String> keys = obj.keys();
	            while(keys.hasNext()) {
	                String key=keys.next();
	                Object value=obj.get(key);
	                List<String> validProperties = ESProperty.getPropertiesList();

	                if(validProperties.contains(key)) {
	                	map.put(key, value.toString());
	                }

	                ClusterFactory.addNodeToCluster(map, "SERVER_ID");
	            }
            } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }


        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
        return responseResource.response( );


    }



}
