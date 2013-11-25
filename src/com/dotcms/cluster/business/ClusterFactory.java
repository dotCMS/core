package com.dotcms.cluster.business;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.cluster.bean.ClusterProperty;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotGuavaCacheAdministratorImpl;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import static com.dotcms.cluster.bean.ClusterProperty.*;

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

	public static String getNextAvailableCachePort() {  // TODO: REMOVE THIS METHOD
		DotConnect dc = new DotConnect();
		dc.setSQL("select max(cache_port) as port from server");
		String maxPort = null;
		String freePort = CACHE_BINDPORT.getDefaultValue();

		try {
			List<Map<String,Object>> results = dc.loadObjectResults();
			if(!results.isEmpty()) {
				maxPort = (String) results.get(0).get("port");
				freePort = UtilMethods.isSet(maxPort)?Integer.toString((Integer.parseInt(maxPort)+1)):freePort;
			}

		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Could not get Cluster ID", e);
		}

		return freePort.toString();
	}

	public static String getCacheTCPInitialHosts() { // TODO: REMOVE THIS METHOD
		DotConnect dc = new DotConnect();
		dc.setSQL("select host, cache_port from server");
		String tcpInitialHosts = "";

		try {
			List<Map<String,Object>> results = dc.loadObjectResults();
			int count = 0;

			for (Map<String, Object> map : results) {
				String host = (String) map.get("host");
				String port = (String) map.get("cache_port");

				if(UtilMethods.isSet(host)) {
					if(count>0) {
						tcpInitialHosts += ", " + host + "[" + port + "]";
					} else {
						tcpInitialHosts += host + "[" + port + "]";
					}
				}
				count++;
			}

			if(count==0) {
				tcpInitialHosts = CACHE_TCP_INITIAL_HOSTS.getDefaultValue();
			}

		} catch (DotDataException e) {
			Logger.error(ClusterFactory.class, "Could not get Cluster ID", e);
		}

		return tcpInitialHosts;
	}

	public static void addNode() {
		addNode(new HashMap<String, String>());
	}

	public static void addNode(Map<String,String> properties) {
		Map<ClusterProperty, String> cacheProperties = new HashMap<ClusterProperty, String>();

		if(properties==null) {
			properties = new HashMap<String, String>();
		}

		cacheProperties.put(CACHE_PROTOCOL,
				UtilMethods.isSet(properties.get(CACHE_PROTOCOL.toString())) ? properties.get(CACHE_PROTOCOL.toString()) : CACHE_PROTOCOL.getDefaultValue() );
		cacheProperties.put(CACHE_BINDADDRESS,
				UtilMethods.isSet(properties.get(CACHE_BINDADDRESS.toString())) ? properties.get(CACHE_BINDADDRESS.toString()) : CACHE_BINDADDRESS.getDefaultValue() );
		cacheProperties.put(CACHE_BINDPORT,
				UtilMethods.isSet(properties.get(CACHE_BINDPORT.toString())) ? properties.get(CACHE_BINDPORT.toString()) : getNextAvailableCachePort() );
		cacheProperties.put(CACHE_TCP_INITIAL_HOSTS,
				UtilMethods.isSet(properties.get(CACHE_TCP_INITIAL_HOSTS.toString())) ? properties.get(CACHE_TCP_INITIAL_HOSTS.toString()) : getCacheTCPInitialHosts() );
		cacheProperties.put(CACHE_TCP_INITIAL_HOSTS,
				UtilMethods.isSet(properties.get(CACHE_MULTICAST_PORT.toString())) ? properties.get(CACHE_MULTICAST_PORT.toString()) : CACHE_MULTICAST_PORT.getDefaultValue() );
		cacheProperties.put(CACHE_MULTICAST_ADDRESS,
				UtilMethods.isSet(properties.get(CACHE_MULTICAST_ADDRESS.toString())) ? properties.get(CACHE_MULTICAST_ADDRESS.toString()) : CACHE_MULTICAST_ADDRESS.getDefaultValue() );
		cacheProperties.put(CACHE_FORCE_IPV4,
				UtilMethods.isSet(properties.get(CACHE_FORCE_IPV4.toString())) ? properties.get(CACHE_FORCE_IPV4.toString()) : CACHE_FORCE_IPV4.getDefaultValue() );

		((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).setCluster(cacheProperties);

		Map<ClusterProperty, String> esProperties = new HashMap<ClusterProperty, String>();

		esProperties.put(ES_NETWORK_HOST,
				UtilMethods.isSet(properties.get(ES_NETWORK_HOST.toString())) ? properties.get(ES_NETWORK_HOST.toString()) : ES_NETWORK_HOST.getDefaultValue() );
		esProperties.put(ES_TRANSPORT_TCP_PORT,
				UtilMethods.isSet(properties.get(ES_TRANSPORT_TCP_PORT.toString())) ? properties.get(ES_TRANSPORT_TCP_PORT.toString()) : null );
		esProperties.put(ES_HTTP_PORT,
				UtilMethods.isSet(properties.get(ES_HTTP_PORT.toString())) ? properties.get(ES_HTTP_PORT.toString()) : null );
		esProperties.put(ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED,
				UtilMethods.isSet(properties.get(ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED.toString()))
				? properties.get(ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED.toString()) : ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED.getDefaultValue() );
		esProperties.put(ES_DISCOVERY_ZEN_PING_TIMEOUT,
				UtilMethods.isSet(properties.get(ES_DISCOVERY_ZEN_PING_TIMEOUT.toString()))
				? properties.get(ES_DISCOVERY_ZEN_PING_TIMEOUT.toString()) : ES_DISCOVERY_ZEN_PING_TIMEOUT.getDefaultValue() );
		esProperties.put(ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS,
				UtilMethods.isSet(properties.get(ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS.toString()))
				? properties.get(ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS.toString()) : null);

		ESClient esClient = new ESClient();
		esClient.setClusterNode(esProperties);

	}

}
