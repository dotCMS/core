package com.dotcms.cluster.business;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.exception.DotDataException;

import java.io.IOException;
import java.util.List;

public interface ServerAPI {

	public void saveServer(Server server) throws DotDataException;

	public Server getServer(String serverId) throws DotDataException;

	public void createServerUptime() throws DotDataException;

	public String readServerId();

	public void updateHeartbeat() throws DotDataException;

	public List<Server> getAliveServers() throws DotDataException;

	public List<Server> getAliveServers(List<String> toExclude) throws DotDataException;

	public  void writeHeartBeatToDisk() throws IOException;

	public void updateServer(Server server) throws DotDataException;

	public String[] getAliveServersIds() throws DotDataException;

	public List<Server> getAllServers() throws DotDataException;

	public void updateServerName(String serverId, String name) throws DotDataException;

	/**
	 * Remove the specified server from the cluster_server_uptime and cluster_server tables
	 * @param serverId Server identifier
	 * @throws DotDataException
	 */
	void removeServerFromClusterTable(String serverId) throws DotDataException;

	/**
	 * Get the list of inactive servers
	 * @return List<Server>
	 * @throws DotDataException
	 */
	List<Server> getInactiveServers() throws DotDataException;

	/**
	 *
	 * @return the server where the API call is made.
	 * @throws DotDataException 
     */
    Server getCurrentServer() throws DotDataException;

    Server getOrCreateServer(String serverId) throws DotDataException;

}
