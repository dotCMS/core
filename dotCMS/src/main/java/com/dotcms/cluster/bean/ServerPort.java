package com.dotcms.cluster.bean;

public enum ServerPort {
	CACHE_PORT("cache_port", "CACHE_BINDPORT", "5701"),
	ES_TRANSPORT_TCP_PORT("es_transport_tcp_port", "transport.tcp.port", "9300"),
	ES_HTTP_PORT("es_http_port", "http.port", "9200");

	private String tableName;
	private String propertyName;
	private String defaultValue;

	ServerPort(String tableName, String propertyName, String defaultValue) {
		this.tableName = tableName;
		this.propertyName = propertyName;
		this.defaultValue = defaultValue;
	}

	public String getTableName() {
		return this.tableName;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

}
