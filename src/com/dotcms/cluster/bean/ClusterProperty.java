package com.dotcms.cluster.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.util.Config;

public enum ClusterProperty {
	CACHE_PROTOCOL(Config.getStringProperty("CACHE_PROTOCOL", "tcp")),
	CACHE_BINDADDRESS(Config.getStringProperty("CACHE_BINDADDRESS", "localhost")),
	CACHE_BINDPORT(Config.getStringProperty("CACHE_BINDPORT", null)),
	CACHE_TCP_INITIAL_HOSTS(Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", "localhost[7800]")),
	CACHE_MULTICAST_PORT(Config.getStringProperty("CACHE_MULTICAST_PORT", "45588")), // jgroups default 45566
	CACHE_MULTICAST_ADDRESS(Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10")), // jgroups default 228.8.8.8
	CACHE_FORCE_IPV4(Config.getStringProperty("CACHE_FORCE_IPV4", "true")),
	ES_NETWORK_HOST(Config.getStringProperty("es.network.host", "localhost"), "es.network.host"),
	ES_TRANSPORT_TCP_PORT(Config.getStringProperty("es.transport.tcp.port", null), "es.transport.tcp.port"),
	ES_HTTP_PORT(Config.getStringProperty("es.http.port", null), "es.http.port"),
	ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED(Config.getStringProperty("es.discovery.zen.ping.multicast.enabled", null), "es.discovery.zen.ping.multicast.enabled"),
	ES_DISCOVERY_ZEN_PING_TIMEOUT(Config.getStringProperty("es.discovery.zen.ping.timeout", null), "es.discovery.zen.ping.timeout"),
	ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS(Config.getStringProperty("es.discovery.zen.ping.unicast.hosts", null), "es.discovery.zen.ping.unicast.hosts");

	private String defaultValue;
	private String keyName;

	private ClusterProperty() {}

	private ClusterProperty(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	private ClusterProperty(String defaultValue, String keyName) {
		this.defaultValue = defaultValue;
		this.keyName = keyName;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public static List<String> getPropertiesList() {
		List<String> propertiesList = new ArrayList<String>();
		Class<ClusterProperty> c = ClusterProperty.class;
		ClusterProperty[] contants = (ClusterProperty[]) c.getEnumConstants();

		for (ClusterProperty clusterProperty : contants) {
			propertiesList.add(clusterProperty.toString());
		}
		return propertiesList;

	}

	public static Map<ClusterProperty, String> getDefaultMap() {
		Map<ClusterProperty, String> defaultMap = new HashMap<ClusterProperty, String>();
		Class<ClusterProperty> c = ClusterProperty.class;
		ClusterProperty[] contants = (ClusterProperty[]) c.getEnumConstants();

		for (ClusterProperty clusterProperty : contants) {
			defaultMap.put(clusterProperty, clusterProperty.getDefaultValue());
		}

		return defaultMap;

	}
}
