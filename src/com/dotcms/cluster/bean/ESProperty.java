package com.dotcms.cluster.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.util.Config;

public enum ESProperty {
	ES_NETWORK_HOST(Config.getStringProperty("es.network.host", "localhost"), "es.network.host"),
	ES_TRANSPORT_TCP_PORT(Config.getStringProperty("es.transport.tcp.port", null), "es.transport.tcp.port"),
	ES_HTTP_PORT(Config.getStringProperty("es.http.port", null), "es.http.port"),
	ES_DISCOVERY_ZEN_PING_MULTICAST_ENABLED(Config.getStringProperty("es.discovery.zen.ping.multicast.enabled", "true"), "es.discovery.zen.ping.multicast.enabled"),
	ES_DISCOVERY_ZEN_PING_TIMEOUT(Config.getStringProperty("es.discovery.zen.ping.timeout", null), "es.discovery.zen.ping.timeout"),
	ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS(Config.getStringProperty("es.discovery.zen.ping.unicast.hosts", null), "es.discovery.zen.ping.unicast.hosts");

	private String defaultValue;
	private String keyName;

	private ESProperty() {}

	private ESProperty(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	private ESProperty(String defaultValue, String keyName) {
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
		Class<ESProperty> c = ESProperty.class;
		ESProperty[] contants = (ESProperty[]) c.getEnumConstants();

		for (ESProperty clusterProperty : contants) {
			propertiesList.add(clusterProperty.toString());
		}
		return propertiesList;

	}

	public static Map<ESProperty, String> getCacheDefaultMap() {
		Map<ESProperty, String> defaultMap = new HashMap<ESProperty, String>();
		Class<ESProperty> c = ESProperty.class;
		ESProperty[] contants = (ESProperty[]) c.getEnumConstants();

		for (ESProperty clusterProperty : contants) {
			if(clusterProperty.getKeyName().startsWith("CACHE")) {
				defaultMap.put(clusterProperty, clusterProperty.getDefaultValue());
			}
		}

		return defaultMap;

	}

	public static Map<ESProperty, String> getESDefaultMap() {
		Map<ESProperty, String> defaultMap = new HashMap<ESProperty, String>();
		Class<ESProperty> c = ESProperty.class;
		ESProperty[] contants = (ESProperty[]) c.getEnumConstants();

		for (ESProperty clusterProperty : contants) {
			if(clusterProperty.getKeyName().startsWith("es.")) {
				defaultMap.put(clusterProperty, clusterProperty.getDefaultValue());
			}
		}

		return defaultMap;

	}
}
