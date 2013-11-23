package com.dotcms.cluster.business;

import java.io.IOException;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.exception.DotDataException;

public interface ServerAPI {

	public void saveServer(Server server) throws DotDataException;

	public Server getServer(String serverId);

	public void createServerUptime(String serverId) throws DotDataException;

	public String readServerId();

	public  void writeServerId(byte[] data) throws IOException;

	public void updateHeartbeat() throws DotDataException;

}
