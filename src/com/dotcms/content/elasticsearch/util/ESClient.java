package com.dotcms.content.elasticsearch.util;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import org.elasticsearch.indices.IndexMissingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import com.dotcms.elasticsearch.script.RelationshipSortOrderScriptFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.liferay.util.FileUtil;

public class ESClient {

	private static Node _nodeInstance;
	final String syncMe = "esSync";

	public Client getClient() {

        try{
            initNode();
        }catch (Exception e) {
            Logger.error(ESClient.class, "Could not initialize ES Node", e);
        }

		return _nodeInstance.client();
	}
	
	public Client getClientInCluster(){
		if ( _nodeInstance == null || _nodeInstance.isClosed()) {
			return null;
		} else {
			return _nodeInstance.client();
		}
	}

    private void initNode () {

        if ( _nodeInstance == null || _nodeInstance.isClosed()) {
            synchronized (syncMe) {
                if ( _nodeInstance == null || _nodeInstance.isClosed()) {

                    loadConfig();

                    shutDownNode();

                    String node_id = ConfigUtils.getServerId();
                    _nodeInstance = nodeBuilder().
                            settings(
                                    ImmutableSettings.settingsBuilder().
                                            put( "name", node_id ).
                                            put( "script.native.related.type", RelationshipSortOrderScriptFactory.class.getCanonicalName() ).build()
                            ).build().start();

                    try {

                        //Build the replicas config settings for the indices client
                        UpdateSettingsRequest settingsRequest = getReplicasSettings();

                        UpdateSettingsResponse resp = _nodeInstance.client().admin().indices().updateSettings(
                                settingsRequest
                        ).actionGet();
                    } catch ( IndexMissingException e ) {
                        /*
                        Updating settings without Indices will throw this exception but should be only visible on start when the
                        just created node does not have any created indices, for this case call the setReplicasSettings method after the indices creation.
                         */
                        Logger.warn( ESClient.class, "Unable to set ES property auto_expand_replicas: [No indices found]" );
                    } catch ( Exception e ) {
                        Logger.error( ESClient.class, "Unable to set ES property auto_expand_replicas.", e );
                    }

                    try {
                        // wait a bit while the node gets available for requests
                        Thread.sleep( 5000L );
                    } catch ( InterruptedException e ) {
                        Logger.error( ESClient.class, "Error waiting for node to be available", e );
                    }
                }
            }
        }
    }

    public void shutDownNode () {
        if ( _nodeInstance != null ) {
            _nodeInstance.close();
        }
    }

    /**
     * This method will update the replicas settings for the IndicesAdminClient
     */
    public void setReplicasSettings () {

        if ( _nodeInstance != null && !_nodeInstance.isClosed() ) {

            try {

                //Build the replicas config settings for the indices client
                UpdateSettingsRequest settingsRequest = getReplicasSettings();

                UpdateSettingsResponse resp = _nodeInstance.client().admin().indices().updateSettings(
                        settingsRequest
                ).actionGet();
            } catch ( Exception e ) {
                Logger.error( ESClient.class, "Unable to set ES property auto_expand_replicas.", e );
            }
        }
    }

    /**
     * Returns the settings of the replicas configuration for the indices client, this configuration depends on the
     * <strong>CLUSTER_AUTOWIRE</strong> and <strong>es.index.auto_expand_replicas</strong> properties.
     * <br>
     * <br>
     *
     * If <strong>CLUSTER_AUTOWIRE == false and es.index.auto_expand_replicas == false</strong> the <strong>auto_expand_replicas</strong> will be disabled
     * and the number of replicas will be set (<strong>es.index.number_of_replicas</strong>).
     *
     * @return The replicas settings
     * @throws IOException
     */
    private UpdateSettingsRequest getReplicasSettings () throws IOException {

        UpdateSettingsRequest settingsRequest = new UpdateSettingsRequest();

        /*
         If CLUSTER_AUTOWIRE AND auto_expand_replicas are false we will specify the number of replicas to use
         */
        if ( !Config.getBooleanProperty("CLUSTER_AUTOWIRE", true) &&
                !Config.getBooleanProperty("es.index.auto_expand_replicas", false) ) {

            //Getting the number of replicas
            int replicas = Config.getIntProperty("es.index.number_of_replicas", 0);

            settingsRequest = settingsRequest.settings(
                    jsonBuilder().startObject()
                            .startObject("index")
                            .field("auto_expand_replicas", "false")
                            .field("number_of_replicas", replicas)
                            .endObject()
                            .endObject().string()
            );
        } else {
            settingsRequest = settingsRequest.settings(
                    jsonBuilder().startObject()
                            .startObject("index")
                            .field("auto_expand_replicas", "0-all")
                            .endObject()
                            .endObject().string()
            );
        }

        return settingsRequest;
    }

	private  void loadConfig(){
		Iterator<String> it = Config.getKeys();

		while(it.hasNext()){
			String key = it.next();

			if(key ==null) continue;

			if(key.startsWith("es.")){
				// if we already have a key, use it
				if(System.getProperty(key) == null){
					if(key.equalsIgnoreCase("es.path.data") || key.equalsIgnoreCase("es.path.work")){
                        String esPath = Config.getStringProperty(key);
                      if( new File(esPath).isAbsolute()){
                    	  System.setProperty(key,esPath);
                      }else
                        System.setProperty(key, FileUtil.getRealPath(esPath));
                    }
					else{
						System.setProperty(key, Config.getStringProperty(key));
					}

					//logger.debug( "Copying esdata folder..." );
				}
			}
		}
	}

	public void setClusterNode(Map<String, String> properties) throws Exception {
	    String httpPort=null, transportTCPPort, bindAddr, initData;
	    ServerAPI serverAPI = APILocator.getServerAPI();
	    Server currentServer=null;
	    
        if(Config.getBooleanProperty("CLUSTER_AUTOWIRE",true)) {

			String serverId = ConfigUtils.getServerId();
			//This line is added because when someone add a license the node is already up and working and reset the existing port
			shutDownNode();
			currentServer = serverAPI.getServer(serverId);

			String storedBindAddr = (UtilMethods.isSet(currentServer.getHost()) && !currentServer.getHost().equals("localhost"))
					?currentServer.getHost():currentServer.getIpAddress();

			bindAddr = properties!=null && UtilMethods.isSet(properties.get("BIND_ADDRESS")) ? properties.get("BIND_ADDRESS")
					:Config.getStringProperty("es.network.host", storedBindAddr);

			currentServer.setHost(Config.getStringProperty("es.network.host", null));

			if(properties!=null && UtilMethods.isSet(properties.get("ES_TRANSPORT_TCP_PORT"))){
				transportTCPPort = getNextAvailableESPort(serverId,bindAddr,properties.get("ES_TRANSPORT_TCP_PORT"));
			} else if(UtilMethods.isSet(currentServer.getEsTransportTcpPort())){
				transportTCPPort = getNextAvailableESPort(serverId,bindAddr,currentServer.getEsTransportTcpPort().toString()); 
			}else{ 
				transportTCPPort = getNextAvailableESPort(serverId, bindAddr, null);
			}

			if(Config.getBooleanProperty("es.http.enabled", false)) {
				httpPort = properties!=null &&   UtilMethods.isSet(properties.get("ES_HTTP_PORT")) ? properties.get("ES_HTTP_PORT")
						:UtilMethods.isSet(currentServer.getEsHttpPort()) ? currentServer.getEsHttpPort().toString()
						:ClusterFactory.getNextAvailablePort(serverId, ServerPort.ES_HTTP_PORT);

				currentServer.setEsHttpPort(Integer.parseInt(httpPort));
			}			

			List<String> myself = new ArrayList<String>();
			myself.add(currentServer.getServerId());

			List<Server> aliveServers = serverAPI.getAliveServers(myself);

			currentServer.setEsTransportTcpPort(Integer.parseInt(transportTCPPort));

			aliveServers.add(currentServer);

			StringBuilder initialHosts = new StringBuilder();

			int i=0;
			for (Server server : aliveServers) {
				if(i>0) {
					initialHosts.append(",");
				}

				if(UtilMethods.isSet(server.getHost()) && !server.getHost().equals("localhost")) {
					initialHosts.append(server.getHost()).append(":").append(server.getEsTransportTcpPort());
				} else {
					initialHosts.append(server.getIpAddress()).append(":").append(server.getEsTransportTcpPort());
				}

				i++;
			}

			if(initialHosts.length()==0) {
				if(bindAddr.equals("localhost")) {
					initialHosts.append(currentServer.getIpAddress()).append(":").append(transportTCPPort);
				} else {
					initialHosts.append(bindAddr).append(":").append(transportTCPPort);
				}
			}

			initData=initialHosts.toString();
			
			try {
                serverAPI.updateServer(currentServer);
            } catch (DotDataException e) {
                Logger.error(this, "Error trying to update server. Server Id: " + currentServer.getServerId());
            }
        }
        else {
            httpPort = Config.getStringProperty("es.http.port", "9200");
            transportTCPPort = Config.getStringProperty("es.transport.tcp.port", null);
            bindAddr = Config.getStringProperty("es.network.host", null);
            initData = Config.getStringProperty("es.discovery.zen.ping.unicast.hosts", null);
        }
        
        if(transportTCPPort!=null)
            System.setProperty("es.transport.tcp.port",  transportTCPPort);
        
        if(bindAddr!=null)
            System.setProperty("es.network.host", bindAddr );
        
        if(Config.getBooleanProperty("es.http.enabled", false)) {
            System.setProperty("es.http.port",  httpPort);
            System.setProperty("es.http.enabled", "true");
        }
        
        System.setProperty("es.discovery.zen.ping.multicast.enabled",
                Config.getStringProperty("es.discovery.zen.ping.multicast.enabled", "false") );

        System.setProperty("es.discovery.zen.ping.timeout",
                Config.getStringProperty("es.discovery.zen.ping.timeout", "5s") );
        
        if(initData!=null) {
    		System.setProperty("es.discovery.zen.ping.unicast.hosts",initData);
    		Logger.info(this, "discovery.zen.ping.unicast.hosts: "+initData);
        }

        shutDownNode();
		initNode();
	}

	public void removeClusterNode() {
	    if(UtilMethods.isSet(System.getProperty("es.discovery.zen.ping.unicast.hosts"))) {
    	    System.setProperty("es.discovery.zen.ping.unicast.hosts","");
    	    shutDownNode();
	    }
	}
	
	/**
	 * Validate if the base port is available in the specified bindAddress. 
	 * If not the it will try to get the next port available
	 * @param serverId Server identification
	 * @param bindAddr Address where the port should be running
	 * @param basePort Initial port to check
	 * @return port
	 */
	public String getNextAvailableESPort(String serverId, String bindAddr, String basePort) {
        
        String freePort = Config.getStringProperty(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName(), ServerPort.ES_TRANSPORT_TCP_PORT.getDefaultValue());
        try {
        	if(UtilMethods.isSet(basePort)){
        		freePort=basePort;
        	}else{
        		Number port = ClusterFactory.getESPort(serverId);
                freePort = UtilMethods.isSet(port)?Integer.toString(port.intValue()+1):freePort;
        	}
            int pp=Integer.parseInt(freePort);
            //This will check the next 10 ports to see if one its available
            int count=1;
            while(!UtilMethods.isESPortFree(bindAddr,pp) && count <= 10) {
            	pp = pp + 1;
               	count++;
            }   
            freePort=Integer.toString(pp);
        } catch (DotDataException e) {
            Logger.error(ESClient.class, "Could not get an Available server port", e);
        }

        return freePort.toString();
    }
}
