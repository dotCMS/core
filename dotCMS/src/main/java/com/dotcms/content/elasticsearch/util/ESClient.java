package com.dotcms.content.elasticsearch.util;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import com.dotcms.cluster.ClusterUtils;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;

public class ESClient {

	private static Node _nodeInstance;
	final String syncMe = "esSync";
	public static final String HOME_PATH = "es.path.home";

	public Client getClient() {

        try{
            initNode(ESUtils.getExtSettingsBuilder());
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

    private void initNode (Builder extSettings) {
    	
        if ( _nodeInstance == null || _nodeInstance.isClosed()) {
        	long start = System.currentTimeMillis();
            synchronized (syncMe) {
                if ( _nodeInstance == null || _nodeInstance.isClosed()) {

                    shutDownNode();

                    final String node_id = ConfigUtils.getServerId();
                    String esPathHome = Config.getStringProperty(HOME_PATH, "WEB-INF/elasticsearch");

                    esPathHome = !new File(esPathHome).isAbsolute() ? FileUtil.getRealPath(esPathHome) : esPathHome;

                    Logger.info(this, "***PATH HOME: " + esPathHome);

                    String yamlPath = ESUtils.getYamlConfiguration(esPathHome);

                    try{
                        _nodeInstance = new Node(
                                Settings.builder().
                                loadFromStream(yamlPath, getClass().getResourceAsStream(yamlPath), false).
                                put( "node.name", node_id ).
                                put("path.home", esPathHome).put(extSettings.build()).
                                        build()
                        ).start();
                    } catch (IOException | NodeValidationException e){
                        Logger.error(this, "Error validating ES node at start.", e);
                        //TODO: idk if a throw E is necessary.
                    }

                    try {

                        //Build the replicas config settings for the indices client
                        //TODO: Evaluate if this code can be moved to setReplicasSettings()
                        Optional<UpdateSettingsRequest> settingsRequest = getReplicasSettings();

                        if(settingsRequest.isPresent()) {
                            _nodeInstance.client().admin().indices().updateSettings(
                                settingsRequest.get()
                            ).actionGet();
                        }
                    } catch ( IndexNotFoundException e ) {
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
            try {
                _nodeInstance.close();
            } catch (IOException e){
                Logger.error(this, "Error shutDownNode ES.", e);
                //TODO: idk if a throw E is necessary.
            }
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
     * <strong>AUTOWIRE_CLUSTER_ES</strong> and <strong>AUTOWIRE_MANAGE_ES_REPLICAS</strong> properties.
     * <br>
     * <br>
     *
     * If <strong>AUTOWIRE_CLUSTER_ES == true and AUTOWIRE_MANAGE_ES_REPLICAS == true</strong> the number of replicas will
     * be handled by the AUTOWIRE.
     *
     * @return The replicas settings
     * @throws IOException
     */
    private Optional<UpdateSettingsRequest> getReplicasSettings () throws IOException {

		Optional<UpdateSettingsRequest> updateSettingsRequest = Optional.empty();

        if (ClusterUtils.isESAutoWireReplicas()){
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
                    .endObject().string(), XContentType.JSON);

                return Optional.of(settingsRequest);
            }
        }

        return updateSettingsRequest;
    }

	public void setClusterNode() throws Exception {
        Server currentServer;

	    String bindAddr;
        String bindAddressFromProperty;
        String httpPort;
        String serverId;
        String transportTCPPort;

        Builder externalSettings = ESUtils.getExtSettingsBuilder();
        ServerAPI serverAPI      = APILocator.getServerAPI();

        httpPort = null;
        serverId = ConfigUtils.getServerId();
        //This line is added because when someone add a license the node is already up and working and reset the existing port
        shutDownNode();

        currentServer = serverAPI.getCurrentServer();

        bindAddressFromProperty = externalSettings.get("transport.host");

        if(UtilMethods.isSet(bindAddressFromProperty)) {
            try {
                InetAddress addr = InetAddress.getByName(bindAddressFromProperty);
                if(ClusterFactory.isValidIP(bindAddressFromProperty)){
                    bindAddressFromProperty = addr.getHostAddress();
                }else{
                    Logger.info(ClusterFactory.class, "Address provided in transport.host property is not "
                            + "valid: " + bindAddressFromProperty);
                    bindAddressFromProperty = null;
                }
            } catch(UnknownHostException e) {
                Logger.info(ClusterFactory.class, "Address provided in transport.host property is not "
                        + " valid: " + bindAddressFromProperty);
                bindAddressFromProperty = null;
            }
        }

        bindAddr = bindAddressFromProperty!=null ? bindAddressFromProperty : currentServer.getIpAddress();

        if(UtilMethods.isSet(currentServer.getEsTransportTcpPort())){
            transportTCPPort = getNextAvailableESPort(serverId,bindAddr,currentServer.getEsTransportTcpPort().toString(), externalSettings);
        }else{
            transportTCPPort = getNextAvailableESPort(serverId, bindAddr, null, externalSettings);
        }

        externalSettings.put("transport.host", bindAddr);
        externalSettings.put(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName(), transportTCPPort);

        if(UtilMethods.isSet(externalSettings.get("http.enabled")) && (Boolean.parseBoolean(externalSettings.get("http.enabled")))) {
            httpPort = UtilMethods.isSet(currentServer.getEsHttpPort()) ? currentServer.getEsHttpPort().toString()
                    :ClusterFactory.getNextAvailablePort(serverId, ServerPort.ES_HTTP_PORT, externalSettings);
            currentServer = Server.builder(currentServer).withEsHttpPort(Integer.parseInt(httpPort)).build();
            if (!UtilMethods.isSet(externalSettings.get(ServerPort.ES_HTTP_PORT.getPropertyName()))){
                externalSettings.put(ServerPort.ES_HTTP_PORT.getPropertyName(), bindAddr);
            }
        }

        externalSettings.put("http.port", httpPort);

        //TODO:Check this set and compare with setUnicastHosts
        currentServer = Server.builder(currentServer).withEsTransportTcpPort(Integer.parseInt(transportTCPPort)).build();

        setUnicastHosts(httpPort, transportTCPPort, bindAddr, serverAPI, currentServer,
                externalSettings);

        try {
            serverAPI.updateServer(currentServer);
        } catch (DotDataException e) {
            Logger.error(this, "Error trying to update server. Server Id: " + currentServer.getServerId());
        }

        shutDownNode();
		initNode(externalSettings);
	}

    private void setUnicastHosts(String httpPort, String transportTCPPort, String bindAddr,
            ServerAPI serverAPI, Server currentServer, Builder externalSettings)
            throws DotDataException {

        String initData;

        List<String> myself = new ArrayList<>();
        myself.add(currentServer.getServerId());
        List<Server> aliveServers = serverAPI.getAliveServers(myself);
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

        if(initData!=null) {
            externalSettings.put("discovery.zen.ping.unicast.hosts", httpPort);
            Logger.info(this, "discovery.zen.ping.unicast.hosts: "+initData);
        }
    }

    public void removeClusterNode() {
	    shutDownNode();
	}

	/**
	 * Validate if the base port is available in the specified bindAddress.
	 * If not the it will try to get the next port available
	 * @param serverId Server identification
	 * @param bindAddr Address where the port should be running
	 * @param basePort Initial port to check
	 * @param externalSettings
     * @return port
	 */
	public String getNextAvailableESPort(String serverId, String bindAddr, String basePort,
            Builder externalSettings) {

        String freePort = null;

        if (UtilMethods.isSet(externalSettings)){
            freePort = externalSettings.get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName());
        }

        if (!UtilMethods.isSet(freePort)){
            freePort = ServerPort.ES_TRANSPORT_TCP_PORT.getDefaultValue();
        }

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
