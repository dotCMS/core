package com.dotcms.cluster.bean;


public class Server {
	private String serverId;
	private String clusterId;
	private String ipAddress;
	private String host;
	private Integer esTransportTcpPort;
	private Integer esNetworkPort;
	private Integer esHttpPort;
	private Integer cachePort;

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

	public Integer getEsTransportTcpPort() {
		return esTransportTcpPort;
	}
	public void setEsTransportTcpPort(Integer esTransportTcpPort) {
		this.esTransportTcpPort = esTransportTcpPort;
	}
	public Integer getEsNetworkPort() {
		return esNetworkPort;
	}
	public void setEsNetworkPort(Integer esNetworkPort) {
		this.esNetworkPort = esNetworkPort;
	}
	public Integer getEsHttpPort() {
		return esHttpPort;
	}
	public void setEsHttpPort(Integer esHttpPort) {
		this.esHttpPort = esHttpPort;
	}
	public Integer getCachePort() {
		return cachePort;
	}
	public void setCachePort(Integer cachePort) {
		this.cachePort = cachePort;
	}

}
