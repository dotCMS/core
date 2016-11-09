package com.dotcms.cluster.bean;

import java.util.Date;

public class ServerUptime {
	private String id;
	private String serverId;
	private Date startup;
	private Date heartbeat;
	private Date shutdown;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public Date getStartup() {
		return startup;
	}
	public void setStartup(Date startup) {
		this.startup = startup;
	}
	public Date getHeartbeat() {
		return heartbeat;
	}
	public void setHeartbeat(Date heartbeat) {
		this.heartbeat = heartbeat;
	}
	public Date getShutdown() {
		return shutdown;
	}
	public void setShutdown(Date shutdown) {
		this.shutdown = shutdown;
	}

}
