package com.dotcms.cluster.bean;


public class Server {
	private String serverId;
	private String clusterId;
	private String ipAddress;
	private String host;
	private Short esTransportTcpPort;
	private Short esNetworkPort;
	private Short esHttpPort;
	private Short cachePort;

	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public String getClusterId() {
		return clusterId;
	}
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}

	public Short getEsTransportTcpPort() {
		return esTransportTcpPort;
	}
	public void setEsTransportTcpPort(Short esTransportTcpPort) {
		this.esTransportTcpPort = esTransportTcpPort;
	}
	public Short getEsNetworkPort() {
		return esNetworkPort;
	}
	public void setEsNetworkPort(Short esNetworkPort) {
		this.esNetworkPort = esNetworkPort;
	}
	public Short getEsHttpPort() {
		return esHttpPort;
	}
	public void setEsHttpPort(Short esHttpPort) {
		this.esHttpPort = esHttpPort;
	}
	public Short getCachePort() {
		return cachePort;
	}
	public void setCachePort(Short cachePort) {
		this.cachePort = cachePort;
	}

}
