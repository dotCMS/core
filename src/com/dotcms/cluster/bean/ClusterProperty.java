package com.dotcms.cluster.bean;

import java.util.ArrayList;
import java.util.List;

public enum ClusterProperty {
	CACHE_PROTOCOL("tcp"),
	CACHE_BINDADDRESS("localhost"),
	CACHE_BINDPORT("7800"),
	CACHE_TCP_INITIAL_HOSTS("localhost[7800]"),
	CACHE_MULTICAST_PORT("45589"),
	CACHE_MULTICAST_ADDRESS("228.10.10.10"),
	CACHE_FORCE_IPV4("true"),
	ES_NETWORK_HOST("localhost"),
	ES_TRANSPORT_TCP_PORT("9301"),
	ES_NETWORK_PORT("9302"),
	ES_HTTP_PORT("9200"),
	ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED("false"),
	ES_DISCOVERY_ZEN_PING_TIMEOUT("5s"),
	ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS("localhost:9301,localhost:9302");

	private final String defaultValue;

	private ClusterProperty(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
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
}
