package com.dotcms.cluster.bean;

import java.util.Date;


public class Server {
	private String serverId;
	private String clusterId;
	private String ipAddress;
	private String host;
	private String name;
	private Integer esTransportTcpPort;
	private Integer esNetworkPort;
	private Integer esHttpPort;
	private Integer cachePort;
	private Date lastHeartBeat;
	private String key;

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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	public Date getLastHeartBeat() {
		return lastHeartBeat;
	}
	public void setLastHeartBeat(Date lastHeartBeat) {
		this.lastHeartBeat = lastHeartBeat;
	}
	public String getKey() {
	    return this.key;
	}
	public void setKey(String key) {
	    this.key = key;
	}
}
