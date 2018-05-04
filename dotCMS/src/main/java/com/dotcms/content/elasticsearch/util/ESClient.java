package com.dotcms.content.elasticsearch.util;

import com.google.common.annotations.VisibleForTesting;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ClusterAPI;
import com.dotcms.cluster.business.ReplicasMode;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.StringPool;

import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ESClient {

	private static Node _nodeInstance;
	final String syncMe = "esSync";
	private final ServerAPI serverAPI;
	static final String ES_TRANSPORT_HOST = "transport.host";
	private final ClusterAPI clusterAPI;

	public ESClient() {
	    this(APILocator.getServerAPI(), APILocator.getClusterAPI());
    }

    public ESClient(final ServerAPI serverAPI, final ClusterAPI clusterAPI) {
        this.serverAPI = serverAPI;
        this.clusterAPI = clusterAPI;
    }

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

    private void initNode (final Builder extSettings) {
    	
        if ( _nodeInstance == null || _nodeInstance.isClosed()) {
        	long start = System.currentTimeMillis();
            synchronized (syncMe) {
                if ( _nodeInstance == null || _nodeInstance.isClosed()) {

                    shutDownNode();

                    final String node_id = ConfigUtils.getServerId();
                    String esPathHome = ESUtils.getESPathHome();

                    Logger.info(this, "***PATH HOME: " + esPathHome);

                    final String yamlPath = ESUtils.getYamlConfiguration();

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
                    }

                    setReplicasSettings();

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
            }
        }
    }

    /**
     * This method will update the replicas settings for the IndicesAdminClient
     */
    public void setReplicasSettings() {
        try {
            //Build the replicas config settings for the indices client
            final Optional<UpdateSettingsRequest> settingsRequest = getReplicasSettings();

            settingsRequest.ifPresent(updateSettingsRequest -> _nodeInstance.client().admin().indices().updateSettings(
                updateSettingsRequest
            ).actionGet());

        } catch (IndexNotFoundException e) {
            /*
            Updating settings without Indices will throw this exception but should be only visible
            on start when the just created node does not have any created indices, for this case
            call the setReplicasSettings method after the indices creation.
             */
            Logger.warn(ESClient.class,
                    "Unable to set up ES replicas: [No indices found]");
        } catch (Exception e) {
            Logger.error(ESClient.class, "Unable to set up ES replicas.", e);
        }
    }

    /**
     * Returns the settings of the replicas configuration for the indices client, this configuration depends on the
     * <strong>ES_INDEX_REPLICAS</strong> and <strong>ES_INDEX_REPLICAS</strong> properties.
     * <br>
     * <br>
     *
     * If <strong>AUTOWIRE_CLUSTER_ES == true and ES_INDEX_REPLICAS == autowire</strong> the number of replicas will
     * be handled by the AUTOWIRE.
     *
     * @return The replicas settings
     * @throws IOException
     */
    private Optional<UpdateSettingsRequest> getReplicasSettings () throws IOException {

        if (!(LicenseUtil.getLevel()> LicenseLevel.STANDARD.level)){
            return Optional.empty();
        }

        UpdateSettingsRequest settingsRequest = new UpdateSettingsRequest();

        final ReplicasMode replicasMode = clusterAPI.getReplicasMode();
        final XContentBuilder builder = jsonBuilder().startObject()
                .startObject("index");

        if(replicasMode.getNumberOfReplicas()>-1) {
            builder.field("number_of_replicas", replicasMode.getNumberOfReplicas());
        }
        builder.field("auto_expand_replicas",replicasMode.getAutoExpandReplicas()).endObject();

        settingsRequest.settings(builder.endObject().string(), XContentType.JSON);

        return Optional.of(settingsRequest);
    }



    /**
     * Builds and returns the settings for starting up the ES node.
     *
     * If ES Autowire is on settings are set by the autowire process unless there are settings
     * specified in the elasticsearch-override.yml file, which will always take precedence and will override autowire values.
     * <p>
     * The properties whose values are determined by autowire are:
     * <ul>
     * <li>es.http.host (if http.enabled comes with true from the override file, and this is not set, set same ip as transport.host)
     * <li>es.http.port
     * <li>transport.host
     * <li>transport.tcp.port
     * </ul>
     * if ClusterUtils.isESAutoWire()==false: from the values in the elasticsearch-override.yml file
     *
     * @return settings object to be used for initializing ES node
     */
	public Builder getESNodeSettings() throws Exception {
        Server currentServer;
        String serverId;

        // Load settings from elasticsearch-override.yml
        final Builder externalSettings = ESUtils.getExtSettingsBuilder();

        // Get current server
        final ServerAPI serverAPI      = APILocator.getServerAPI();
        currentServer = serverAPI.getCurrentServer();

        // Add transport.host and transport.tcp.port to the settings
        setTransportConfToSettings(currentServer, externalSettings);

        // Add http.host and http.port to the settings if http.enabled=true
        setHttpConfToSettings(currentServer, externalSettings);

        // update server with tcp transport address and port
        updateServerTransportConfFromSettings(currentServer, externalSettings);

        // update server with http port
        updateServerHttpConfFromSettings(currentServer, externalSettings);

        // set comma-separated list of address:port to settings
        setUnicastHostsToSettings(externalSettings);

        return externalSettings;
	}

    private void updateServerTransportConfFromSettings(Server currentServer, final Builder externalSettings) {

        final int transportTCPPort = Integer.parseInt(externalSettings
            .get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName()));

        Server currentServerWithTransportSettings = Server.builder(currentServer)
            .withIpAddress(externalSettings.get(ES_TRANSPORT_HOST)).withEsTransportTcpPort(transportTCPPort).build();

        try {
            serverAPI.updateServer(currentServerWithTransportSettings);
        } catch (DotDataException e) {
            Logger.error(this,
                "Error trying to update server. Server Id: " + currentServer.getServerId(), e);
        }
    }

    private void updateServerHttpConfFromSettings(Server currentServer, final Builder externalSettings) {

	    if(!UtilMethods.isSet(externalSettings.get(ServerPort.ES_HTTP_PORT.getPropertyName()))) {
	        return;
        }

        final int httpPort = Integer.parseInt(externalSettings
            .get(ServerPort.ES_HTTP_PORT.getPropertyName()));

        Server currentServerWithPort = Server.builder(currentServer).withEsHttpPort(httpPort).build();

        try {
            serverAPI.updateServer(currentServerWithPort);
        } catch (DotDataException e) {
            Logger.error(this,
                "Error trying to update server. Server Id: " + currentServer.getServerId(), e);
        }

    }

    /**
     *
     * @param currentServer
     * @param externalSettings
     * @return
     */
    private void setHttpConfToSettings(final Server currentServer, final Builder externalSettings) {
        String httpPort;

        final String bindAddr = externalSettings.get("transport.host");
        if (UtilMethods.isSet(externalSettings.get("http.enabled")) && (Boolean
                .parseBoolean(externalSettings.get("http.enabled")))) {
            httpPort =
                    UtilMethods.isSet(currentServer.getEsHttpPort()) ? currentServer.getEsHttpPort()
                            .toString()
                            : ClusterFactory.getNextAvailablePort(currentServer.getServerId(), ServerPort.ES_HTTP_PORT,
                                    externalSettings);

            if (!UtilMethods.isSet(externalSettings.get("http.host"))) {
                externalSettings.put("http.host", bindAddr);
            }

            externalSettings.put(ServerPort.ES_HTTP_PORT.getPropertyName(), httpPort);
        }
    }

    /**
     *
     * @param currentServer
     * @param externalSettings
     */
    @VisibleForTesting
    protected void setTransportConfToSettings(final Server currentServer, final Builder externalSettings) {
        String bindAddressFromProperty;
        String bindAddr;
        String transportTCPPort;
        bindAddressFromProperty = externalSettings.get(ES_TRANSPORT_HOST);

        if (UtilMethods.isSet(bindAddressFromProperty)) {
            bindAddressFromProperty = validateAddress(bindAddressFromProperty);
        }

        bindAddr = bindAddressFromProperty != null ? bindAddressFromProperty : currentServer.getIpAddress();

        externalSettings.put(ES_TRANSPORT_HOST, bindAddr);

        transportTCPPort = UtilMethods.isSet(currentServer.getEsTransportTcpPort())
            ? Integer.toString(currentServer.getEsTransportTcpPort())
            : getNextAvailableESPort(currentServer.getServerId(), bindAddr, externalSettings);

        externalSettings.put(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName(), transportTCPPort);
    }

    @Nullable
    private String validateAddress(String bindAddressFromProperty) {
        try {
            InetAddress addr = InetAddress.getByName(bindAddressFromProperty);
            if (ClusterFactory.isValidIP(bindAddressFromProperty)) {
                bindAddressFromProperty = addr.getHostAddress();
            } else {
                Logger.info(ClusterFactory.class,
                        "Address provided in transport.host property is not "
                                + "valid: " + bindAddressFromProperty);
                bindAddressFromProperty = null;
            }
        } catch (UnknownHostException e) {
            Logger.info(ClusterFactory.class,
                    "Address provided in transport.host property is not "
                            + " valid: " + bindAddressFromProperty);
            bindAddressFromProperty = null;
        }
        return bindAddressFromProperty;
    }

    /**
     *
     * @param externalSettings
     */
    public void restartNode(final Builder externalSettings) {
        shutDownNode();
        initNode(externalSettings);
    }

    @VisibleForTesting
    protected String getServerAddress(final Server server) {
        final String ipAddress = (UtilMethods.isSet(server.getHost()) && !server.getHost().equals("localhost"))
            ? server.getHost()
            : server.getIpAddress();
        final String port = Integer.toString(server.getEsTransportTcpPort());
        return ipAddress + StringPool.COLON + port;
    }

    /**
     *
     * @param externalSettings
     * @throws DotDataException
     */
    private void setUnicastHostsToSettings(final Builder externalSettings) throws DotDataException {

        final String bindAddr = externalSettings.get("transport.host");
        final String transportTCPPort = externalSettings.get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName());

        final List<Server> aliveServers = serverAPI.getAliveServers();

        String initialHosts = aliveServers.stream().map(this::getServerAddress).collect(Collectors.joining(","));

        if(initialHosts.isEmpty()) {
            initialHosts = bindAddr.equals("localhost")
                ? serverAPI.getCurrentServer().getIpAddress() + ":" + transportTCPPort
                : bindAddr + ":" + transportTCPPort;
        }

        if(UtilMethods.isSet(initialHosts)) {
            externalSettings.put("discovery.zen.ping.unicast.hosts", initialHosts);
            Logger.info(this, "discovery.zen.ping.unicast.hosts: "+initialHosts);
        }
    }

    public void removeClusterNode() {
	    shutDownNode();
	}

	private String getNextAvailableESPort(final String serverId, final String bindAddr, final Builder externalSettings) {
        return getNextAvailableESPort(serverId, bindAddr, null, externalSettings);
    }

	/**
	 * Validate if the base port is available in the specified bindAddress.
	 * If not the it will try to get the next port available
	 * @param serverId Server identification
	 * @param bindAddr Address where the port should be running
	 * @param basePort Initial port to check
	 * @param externalSettings settings used to override configuration
     * @return port
	 */
	public String getNextAvailableESPort(final String serverId, final String bindAddr, final String basePort,
            final Builder externalSettings) {

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

        return freePort;
    }
}
