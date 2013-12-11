package com.dotcms.cluster.business;

import java.util.List;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.exception.DotDataException;

public abstract class ServerFactory {

	public abstract void saveServer(Server server) throws DotDataException;

	public abstract Server getServer(String serverId);

	public abstract void createServerUptime(String serverId) throws DotDataException;

	public abstract void updateHeartbeat(String serverId) throws DotDataException;

	public abstract List<Server> getAliveServers() throws DotDataException;

	public abstract List<Server> getAliveServers(List<String> toExclude) throws DotDataException;

	public abstract void updateServer(Server server) throws DotDataException;

	public abstract String[] getAliveServersIds() throws DotDataException;

	public abstract List<Server> getAllServers() throws DotDataException;

}
