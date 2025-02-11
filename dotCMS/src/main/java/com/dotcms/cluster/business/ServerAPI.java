package com.dotcms.cluster.business;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.exception.DotDataException;

import com.dotmarketing.util.UUIDUtil;
import io.vavr.Lazy;
import java.io.IOException;
import java.util.List;

public interface ServerAPI {

	/**
	 * this is the UNIQUE identifier for the server instance
	 */
	final static Lazy<String> SERVER_ID = Lazy.of(UUIDUtil::uuid);
	public void saveServer(Server server) throws DotDataException;

	public Server getServer(String serverId) throws DotDataException;

	public String readServerId();

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

	Server getOrCreateMyServer() throws DotDataException;

    void writeHeartBeatToDisk(String serverId) throws IOException;

    String getOldestServer() throws DotDataException, IOException;
    
    /**
     * Returns a list of serverIds participating in a reindex
     * @return
     * @throws DotDataException
     */
  List<String> getReindexingServers() throws DotDataException;

	/**
	 * Gets the servers start time
	 *
	 * @return milliseconds representing the date-time when the server started.
	 */
	long getServerStartTime();

}
