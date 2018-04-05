package com.dotcms.cluster.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.util.Config;

@Deprecated
public enum ESProperty {
	ES_NETWORK_HOST(Config.getStringProperty("network.host", "localhost"), "network.host"),
	ES_TRANSPORT_TCP_PORT(Config.getStringProperty("transport.tcp.port", null), "transport.tcp.port"),
	ES_HTTP_PORT(Config.getStringProperty("http.port", null), "http.port"),
	ES_DISCOVERY_ZEN_PING_TIMEOUT(Config.getStringProperty("discovery.zen.ping.fd.timeout", "5s"), "discovery.zen.fd.ping.timeout"),
	ES_DISCOVERY_ZEN_PING_UNICAST_HOSTS(Config.getStringProperty("discovery.zen.ping.unicast.hosts", null), "discovery.zen.ping.unicast.hosts");

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
