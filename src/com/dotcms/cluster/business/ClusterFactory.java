package com.dotcms.cluster.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.cluster.bean.ESProperty;
import com.dotcms.cluster.bean.Server;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotGuavaCacheAdministratorImpl;
import com.dotmarketing.cache.H2CacheLoader;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.servlet.MainServlet;

import static com.dotcms.cluster.bean.ESProperty.*;

public class ClusterFactory {

	public static void generateClusterId() throws DotDataException {

		String clusterId = getClusterId();

		if(!UtilMethods.isSet(clusterId)) {
			DotConnect dc = new DotConnect();
			dc.setSQL("insert into cluster values (?)");
			clusterId = UUID.randomUUID().toString();
			dc.addParam(clusterId);
			dc.loadResult();
		}
	}

	public static String getClusterId() {
		DotConnect dc = new DotConnect();
		dc.setSQL("select cluster_id from cluster");
		String clusterId = null;

		try {
			List<Map<String,Object>> results = dc.loadObjectResults();
			if(!results.isEmpty()) {
				clusterId = (String) results.get(0).get("cluster_id");
			}

		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Could not get Cluster ID", e);
		}

		return clusterId;
	}

	public static String getNextAvailableCachePort(String serverId) {
		DotConnect dc = new DotConnect();
		dc.setSQL("select max(cache_port) as port from server where ip_address = (select s.ip_address from server s where s.server_id = ?)");
		dc.addParam(serverId);
		Long maxPort = null;
		String freePort = Config.getStringProperty("CACHE_BINDPORT", "7800");

		try {
			List<Map<String,Object>> results = dc.loadObjectResults();
			if(!results.isEmpty()) {
				maxPort = (Long) results.get(0).get("port");
				freePort = UtilMethods.isSet(maxPort)?Long.toString(maxPort+1):freePort;
			}

		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Could not get Cluster ID", e);
		}

		return freePort.toString();
	}

	public static String getNextAvailableESPort(String serverId) {
		DotConnect dc = new DotConnect();
		dc.setSQL("select max(cache_port) as port from server where ip_address = (select s.ip_address from server s where s.server_id = ?)");
		dc.addParam(serverId);
		Long maxPort = null;
		String freePort = Config.getStringProperty("CACHE_BINDPORT", "7800");

		try {
			List<Map<String,Object>> results = dc.loadObjectResults();
			if(!results.isEmpty()) {
				maxPort = (Long) results.get(0).get("port");
				freePort = UtilMethods.isSet(maxPort)?Long.toString(maxPort+1):freePort;
			}

		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Could not get Cluster ID", e);
		}

		return freePort.toString();
	}

	public static void addNodeToCluster(String serverId) {
		addNodeToCluster(null, serverId);
	}

	public static void addNodeToCluster(Map<String,String> properties, String serverId) {

		if(properties==null) {
			properties = new HashMap<String, String>();
		}

		addNodeToCacheCluster(properties, serverId);

		Map<ESProperty, String> esProperties = new HashMap<ESProperty, String>();

		esProperties.put(ES_NETWORK_HOST,
				UtilMethods.isSet(properties.get(ES_NETWORK_HOST.toString())) ? properties.get(ES_NETWORK_HOST.toString()) : ES_NETWORK_HOST.getDefaultValue() );
		esProperties.put(ES_TRANSPORT_TCP_PORT,
				UtilMethods.isSet(properties.get(ES_TRANSPORT_TCP_PORT.toString())) ? properties.get(ES_TRANSPORT_TCP_PORT.toString()) : ES_TRANSPORT_TCP_PORT.getDefaultValue() );
		esProperties.put(ES_HTTP_PORT,
				UtilMethods.isSet(properties.get(ES_HTTP_PORT.toString())) ? properties.get(ES_HTTP_PORT.toString()) : ES_HTTP_PORT.getDefaultValue() );
		esProperties.put(ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED,
				UtilMethods.isSet(properties.get(ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED.toString()))
				? properties.get(ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED.toString()) : ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED.getDefaultValue() );
		esProperties.put(ES_DISCOVERY_ZEN_PING_TIMEOUT,
				UtilMethods.isSet(properties.get(ES_DISCOVERY_ZEN_PING_TIMEOUT.toString()))
				? properties.get(ES_DISCOVERY_ZEN_PING_TIMEOUT.toString()) : ES_DISCOVERY_ZEN_PING_TIMEOUT.getDefaultValue() );
//
		ServerAPI serverAPI = APILocator.getServerAPI();
		List<Server> aliveServers = new ArrayList<Server>();

		try {
			aliveServers = serverAPI.getAliveServers();
		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Error getting alive Servers", e);
		}

//		Server currentServer = serverAPI.getServer(serverId);
//		aliveServers.add(currentServer);
//
//		String initalHosts = "";
//
//		int i=0;
//		for (Server server : aliveServers) {
//			if(i>0) {
//				initalHosts += ", ";
//			}
//			initalHosts += server.getHost() + "[" + server.getCachePort() + "]";
//			i++;
//		}
//
//		esProperties.put(ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS,
//				UtilMethods.isSet(properties.get(ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS.toString()))
//				? properties.get(ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS.toString()) : null);
//
//		addNodeToESCluster(esProperties);

	}

	private static void addNodeToCacheCluster(Map<String, String> cacheProperties, String serverId) {
		((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).setCluster(cacheProperties, serverId);
		((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).testCluster();
		Config.setProperty("DIST_INDEXATION_ENABLED", true);

    	try {
			H2CacheLoader.getInstance().moveh2dbDir();
		} catch (Exception e) {
			Logger.error(MainServlet.class, "Error sending H2DB Dir to trash", e);
		}

	}

	private static void addNodeToESCluster(Map<ESProperty, String> esProperties) {
		ESClient esClient = new ESClient();
		esClient.setClusterNode(esProperties);

	}

}
