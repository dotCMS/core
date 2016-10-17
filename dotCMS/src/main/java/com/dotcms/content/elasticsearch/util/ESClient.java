package com.dotcms.content.elasticsearch.util;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.dotmarketing.util.WebKeys;

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
	private String DATA_PATH = "es.path.data";
	private String WORK_PATH = "es.path.work";
	private String REPO_PATH = "es.path.repo";

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
        	long start = System.currentTimeMillis();
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
                        Optional<UpdateSettingsRequest> settingsRequest = getReplicasSettings();

                        if(settingsRequest.isPresent()) {
                            _nodeInstance.client().admin().indices().updateSettings(
                                settingsRequest.get()
                            ).actionGet();
                        }
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
            System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_ES, String.valueOf(System.currentTimeMillis() - start));
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
                Optional<UpdateSettingsRequest> settingsRequest = getReplicasSettings();

                if(settingsRequest.isPresent()) {
                    _nodeInstance.client().admin().indices().updateSettings(
                        settingsRequest.get()
                    ).actionGet();
                }
            } catch ( Exception e ) {
                Logger.error( ESClient.class, "Unable to set ES property auto_expand_replicas.", e );
            }
        }
    }

    /**
     * Returns the settings of the replicas configuration for the indices client, this configuration depends on the
     * <strong>CLUSTER_AUTOWIRE</strong> and <strong>AUTOWIRE_MANAGE_ES_REPLICAS</strong> properties.
     * <br>
     * <br>
     *
     * If <strong>CLUSTER_AUTOWIRE == true and AUTOWIRE_MANAGE_ES_REPLICAS == true</strong> the number of replicas will
     * be handled by the AUTOWIRE.
     *
     * @return The replicas settings
     * @throws IOException
     */
    private Optional<UpdateSettingsRequest> getReplicasSettings () throws IOException {

		Optional<UpdateSettingsRequest> updateSettingsRequest = Optional.empty();

        if (Config.getBooleanProperty("CLUSTER_AUTOWIRE", true)
            && Config.getBooleanProperty("AUTOWIRE_MANAGE_ES_REPLICAS",true)){
            int serverCount;

            try {
                serverCount = APILocator.getServerAPI().getAliveServersIds().length;
            } catch (DotDataException e) {
                Logger.error(this.getClass(), "Error getting live server list for server count, using 1 as default.");
                serverCount = 1;
            }
            // formula is (live server count (including the ones that are down but not yet timed out) - 1)

            if(serverCount>0) {
                UpdateSettingsRequest settingsRequest = new UpdateSettingsRequest();
                settingsRequest.settings(jsonBuilder().startObject()
                    .startObject("index")
                    .field("auto_expand_replicas", false)
                    .field("number_of_replicas", serverCount - 1)
                    .endObject()
                    .endObject().string()
                );

                return Optional.of(settingsRequest);
            }
        }

        return updateSettingsRequest;
    }

	private  void loadConfig(){
		Iterator<String> it = Config.getKeys();

		while(it.hasNext()){
			String key = it.next();

			if(key ==null) continue;

			if(key.startsWith("es.")){
				// if we already have a key, use it
				if(System.getProperty(key) == null){
					if(key.equalsIgnoreCase(DATA_PATH) || key.equalsIgnoreCase(WORK_PATH) || key.equalsIgnoreCase(REPO_PATH)){
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

	public void setClusterNode() throws Exception {
	    String httpPort=null, transportTCPPort, bindAddr, initData;
	    ServerAPI serverAPI = APILocator.getServerAPI();
	    Server currentServer=null;

        if(Config.getBooleanProperty("CLUSTER_AUTOWIRE",true)) {

			String serverId = ConfigUtils.getServerId();
			//This line is added because when someone add a license the node is already up and working and reset the existing port
			shutDownNode();
			currentServer = serverAPI.getServer(serverId);

            String bindAddressFromProperty = Config.getStringProperty("es.network.host", null, false);

            if(UtilMethods.isSet(bindAddressFromProperty)) {
                try {
                    InetAddress addr = InetAddress.getByName(bindAddressFromProperty);
                    if(ClusterFactory.isValidIP(bindAddressFromProperty)){
                        bindAddressFromProperty = addr.getHostAddress();
                    }else{
                        Logger.info(ClusterFactory.class, "Address provided in es.network.host property is not "
                            + "valid: " + bindAddressFromProperty);
                        bindAddressFromProperty = null;
                    }
                } catch(UnknownHostException e) {
                    Logger.info(ClusterFactory.class, "Address provided in es.network.host property is not "
                        + " valid: " + bindAddressFromProperty);
                    bindAddressFromProperty = null;
                }
            }

            bindAddr = bindAddressFromProperty!=null ? bindAddressFromProperty : currentServer.getIpAddress();

			if(UtilMethods.isSet(currentServer.getEsTransportTcpPort())){
				transportTCPPort = getNextAvailableESPort(serverId,bindAddr,currentServer.getEsTransportTcpPort().toString());
			}else{
				transportTCPPort = getNextAvailableESPort(serverId, bindAddr, null);
			}

			if(Config.getBooleanProperty("es.http.enabled", false)) {
				httpPort = UtilMethods.isSet(currentServer.getEsHttpPort()) ? currentServer.getEsHttpPort().toString()
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
