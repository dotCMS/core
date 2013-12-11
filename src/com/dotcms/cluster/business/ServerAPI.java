package com.dotcms.cluster.business;

import java.io.IOException;
import java.util.List;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.exception.DotDataException;

public interface ServerAPI {

	public void saveServer(Server server) throws DotDataException;

	public Server getServer(String serverId);

	public void createServerUptime(String serverId) throws DotDataException;

	public String readServerId();

	public  void writeServerIdToDisk(String serverId) throws IOException;

	public void updateHeartbeat() throws DotDataException;

	public List<Server> getAliveServers() throws DotDataException;

	public List<Server> getAliveServers(List<String> toExclude) throws DotDataException;

	public  void writeHeartBeatToDisk(String serverId) throws IOException;

	public void updateServer(Server server) throws DotDataException;

	public String[] getAliveServersIds() throws DotDataException;

	public List<Server> getAllServers() throws DotDataException;

}
