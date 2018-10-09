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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;

import java.io.InputStream;
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.cluster.bean.ServerPort.ES_TRANSPORT_TCP_PORT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ESClient {

	private static Node _nodeInstance;
	final String syncMe = "esSync";
	private final ServerAPI serverAPI;
	@VisibleForTesting static final String ES_TRANSPORT_HOST = "transport.host";
	@VisibleForTesting static final String ES_NODE_DATA = "node.data";
	@VisibleForTesting static final String ES_NODE_MASTER = "node.master";
	@VisibleForTesting static final String ES_ZEN_UNICAST_HOSTS = "discovery.zen.ping.unicast.hosts";
    private static final String ES_PATH_HOME_DEFAULT_VALUE = "WEB-INF/elasticsearch";
    private static final String ES_CONFIG_DIR = "config";
    private static final String ES_YML_FILE = "elasticsearch.yml";
    private static final String ES_EXT_YML_FILE = "elasticsearch-override.yml";
    private static final String ES_PATH_HOME = "es.path.home";
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
            initNode(null);
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



                    try{
                        _nodeInstance = new Node(loadNodeSettings(extSettings)).start();
                    } catch (IOException | NodeValidationException e){
                        Logger.error(this, "Error validating ES node at start.", e);
                    }

                    if (UtilMethods.isSet(extSettings)){
                        setReplicasSettings();
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

    @VisibleForTesting
    Settings loadNodeSettings(final Builder extSettings) throws IOException {

        final String node_id = ConfigUtils.getServerId();
        final String esPathHome = getESPathHome();

        Logger.info(this, "***PATH HOME: " + esPathHome);

        final Path yamlPath = getDefaultYaml();

        try(InputStream inputStream = Files.newInputStream(yamlPath)) {
            final Builder builder = Settings.builder().
                    loadFromStream(yamlPath.toString(), inputStream, false).
                    put("node.name", node_id).
                    put("path.home", esPathHome);

            setAbsolutePath("path.data", builder);
            setAbsolutePath("path.repo", builder);
            setAbsolutePath("path.logs", builder);

            builder.put(
                    extSettings != null ? extSettings.build() : getExtSettingsBuilder().build());

            //Remove any discovery property when using community license
            if (isCommunityOrStandard()) {
                List<String> keysToRemove = builder.keys().stream()
                        .filter(key -> key.startsWith("discovery.") && !key
                                .equals(ES_ZEN_UNICAST_HOSTS)).collect(Collectors.toList());
                keysToRemove.forEach(elem -> builder.remove(elem));
            }
            return builder.build();
        }
    }

    private void setAbsolutePath(final String key, final Builder builder){
        final String pathData = builder.get(key);
        if(UtilMethods.isSet(pathData) && !new File(pathData).isAbsolute()){
            builder.put(key, ConfigUtils.getDynamicContentPath() + File.separator + pathData);
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
        // Load settings from elasticsearch-override.yml
        final Builder externalSettings = getExtSettingsBuilder();

        // Add transport.host and transport.tcp.port to the settings
        setTransportConfToSettings(externalSettings);

        // Add http.host and http.port to the settings if http.enabled=true
        setHttpConfToSettings(externalSettings);

        // update server with tcp transport address and port
        updateServerTransportConfFromSettings(externalSettings);

        // update server with http port
        updateServerHttpConfFromSettings(externalSettings);

        // set comma-separated list of address:port to settings
        setUnicastHostsToSettings(externalSettings);

        return externalSettings;
	}

    private void updateServerTransportConfFromSettings(final Builder externalSettings) throws DotDataException {

        if (!UtilMethods
                .isSet(externalSettings.get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName()))) {
            return;
        }

        final int transportTCPPort = Integer.parseInt(externalSettings
            .get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName()));

        final Server currentServer = serverAPI.getCurrentServer();

        Server currentServerWithTransportSettings = Server.builder(currentServer)
            .withIpAddress(externalSettings.get(ES_TRANSPORT_HOST)).withEsTransportTcpPort(transportTCPPort).build();

        try {
            serverAPI.updateServer(currentServerWithTransportSettings);
        } catch (DotDataException e) {
            Logger.error(this,
                "Error trying to update server. Server Id: " + currentServer.getServerId(), e);
        }
    }

    private void updateServerHttpConfFromSettings(final Builder externalSettings) throws DotDataException {

	    if(!UtilMethods.isSet(externalSettings.get(ServerPort.ES_HTTP_PORT.getPropertyName()))) {
	        return;
        }

        final int httpPort = Integer.parseInt(externalSettings
            .get(ServerPort.ES_HTTP_PORT.getPropertyName()));

        final Server currentServer = serverAPI.getCurrentServer();

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
     * @param externalSettings
     * @return
     */
    @VisibleForTesting
    void setHttpConfToSettings(final Builder externalSettings) throws DotDataException {
        if(clusterAPI.isESAutoWire()) {
            String httpPort = externalSettings.get(ServerPort.ES_HTTP_PORT.getPropertyName());

            if (UtilMethods.isSet(externalSettings.get("http.enabled")) && (Boolean
                .parseBoolean(externalSettings.get("http.enabled")))) {

                if(!UtilMethods.isSet(httpPort)) {
                    final Server currentServer = serverAPI.getCurrentServer();

                    httpPort =
                            UtilMethods.isSet(currentServer.getEsHttpPort()) ? currentServer.getEsHttpPort()
                                    .toString()
                                    : ClusterFactory.getNextAvailablePort(currentServer.getServerId(), ServerPort.ES_HTTP_PORT,
                                    externalSettings);

                }

                if (!UtilMethods.isSet(externalSettings.get("http.host"))) {
                    externalSettings.put("http.host", externalSettings.get("transport.host"));
                }

                externalSettings.put(ServerPort.ES_HTTP_PORT.getPropertyName(), httpPort);
            }
        }
    }

    /**
     *
     * @param externalSettings
     */
    @VisibleForTesting
    void setTransportConfToSettings(final Builder externalSettings) throws DotDataException {
        if(clusterAPI.isESAutoWire()) {
            String bindAddressFromProperty = externalSettings.get(ES_TRANSPORT_HOST);

            if (UtilMethods.isSet(bindAddressFromProperty)) {
                bindAddressFromProperty = validateAddress(bindAddressFromProperty);
            }

            final Server currentServer = serverAPI.getCurrentServer();

            final String bindAddr = bindAddressFromProperty != null ? bindAddressFromProperty : currentServer.getIpAddress();

            externalSettings.put(ES_TRANSPORT_HOST, bindAddr);

            String transportTCPPort = externalSettings.get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName());

            if(!UtilMethods.isSet(transportTCPPort)) {
                transportTCPPort = UtilMethods.isSet(currentServer.getEsTransportTcpPort())
                        ? Integer.toString(currentServer.getEsTransportTcpPort())
                        : getNextAvailableESPort(currentServer.getServerId(), bindAddr, externalSettings);
            }

            externalSettings.put(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName(), transportTCPPort);
        }
    }

    @Nullable
    @VisibleForTesting
    protected String validateAddress(String bindAddressFromProperty) {
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
        if(clusterAPI.isESAutoWire()) {
            final String bindAddr = externalSettings.get(ES_TRANSPORT_HOST);
            final String transportTCPPort = externalSettings.get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName());

            final List<Server> aliveServers = serverAPI.getAliveServers();

            String initialHosts = aliveServers.stream().map(this::getServerAddress).collect(Collectors.joining(","));

            if (initialHosts.isEmpty()) {
                initialHosts = bindAddr.equals("localhost")
                    ? serverAPI.getCurrentServer().getIpAddress() + ":" + transportTCPPort
                    : bindAddr + ":" + transportTCPPort;
            }

            if (UtilMethods.isSet(initialHosts)) {
                externalSettings.put(ES_ZEN_UNICAST_HOSTS, initialHosts);
                Logger.info(this, ES_ZEN_UNICAST_HOSTS + ": " + initialHosts);
            }
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

    private String getESPathHome() {
        String esPathHome = Config
            .getStringProperty(ES_PATH_HOME, ES_PATH_HOME_DEFAULT_VALUE);

        esPathHome =
            !new File(esPathHome).isAbsolute() ? FileUtil.getRealPath(esPathHome) : esPathHome;

        return esPathHome;
    }

    @VisibleForTesting
    Path getDefaultYaml(){
        final String yamlPath = System.getenv("ES_PATH_CONF");
        if (UtilMethods.isSet(yamlPath)  && FileUtil.exists(yamlPath)){
            return Paths.get(yamlPath);
        }else{
            return Paths.get(getESPathHome() + File.separator + ES_CONFIG_DIR + File.separator + ES_YML_FILE);
        }
    }

    @VisibleForTesting
    Builder getExtSettingsBuilder() throws IOException {
        Builder settings = Settings.builder();
        final Path overrideSettingsPath = getOverrideYamlPath();

        if (Files.exists(overrideSettingsPath)) {
            settings = Settings.builder().loadFromPath(overrideSettingsPath);

            if(isCommunityOrStandard()) {
                // Let's try to get Transport TCP Port from elasticsearch-override.yml file
                String transportTCPPort = settings.get(ES_TRANSPORT_TCP_PORT.getPropertyName());

                if(!UtilMethods.isSet(transportTCPPort)) {
                    transportTCPPort = getDefaultTransportTCPPort();
                }

                setCommunityESValues(settings, transportTCPPort);
            }
        } else if(isCommunityOrStandard()) {
            setCommunityESValues(settings, getDefaultTransportTCPPort());
        }

        return settings;
    }

    @VisibleForTesting
    Path getOverrideYamlPath() {
        String overrideYamlPathStr = System.getenv("ES_PATH_CONF");
        if (!UtilMethods.isSet(overrideYamlPathStr) || !FileUtil.exists(overrideYamlPathStr)) {
            //Get elasticsearch-override.yml from default location
            overrideYamlPathStr = getESPathHome() + File.separator + ES_CONFIG_DIR + File.separator + ES_EXT_YML_FILE;
        } else {
            //Otherwise, get parent directory from the ES_PATH_CONF
            overrideYamlPathStr = new File(overrideYamlPathStr).getParent() + File.separator + ES_EXT_YML_FILE;
        }
        return Paths.get(overrideYamlPathStr);
    }

    private void setCommunityESValues(final Builder overrideSettings, final String transportTCPPort) {
        overrideSettings.put(ES_ZEN_UNICAST_HOSTS, "localhost:"+transportTCPPort);
        overrideSettings.put(ES_TRANSPORT_HOST, "localhost");
        overrideSettings.put(ES_NODE_DATA, "true");
        overrideSettings.put(ES_NODE_MASTER, "true");
    }

    /**
     * Tries to get the Transport TCP Port from the elasticsearch.yml file.
     * If not found returns the getDefaultValue() of {@link ServerPort#ES_TRANSPORT_TCP_PORT}
     */
    @VisibleForTesting
    String getDefaultTransportTCPPort() throws IOException {
        String transportTCPPort = ES_TRANSPORT_TCP_PORT.getDefaultValue();
        final Path defaultYMLPath = getDefaultYaml();

        if (Files.exists(defaultYMLPath)) {
            final Builder defaultSettings = Settings.builder().loadFromPath(defaultYMLPath);
            final String transportTCPPortFromYAML = defaultSettings.get(ES_TRANSPORT_TCP_PORT.getPropertyName());
            transportTCPPort = UtilMethods.isSet(transportTCPPortFromYAML)?transportTCPPortFromYAML:transportTCPPort;
        }

        return transportTCPPort;
    }

    @VisibleForTesting
    boolean isCommunityOrStandard() {
        return LicenseUtil.getLevel()<= LicenseLevel.STANDARD.level;
    }
}
