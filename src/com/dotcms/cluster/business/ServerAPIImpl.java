package com.dotcms.cluster.business;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class ServerAPIImpl implements ServerAPI {

	private ServerFactory serverFactory;

	public ServerAPIImpl() {
		serverFactory = FactoryLocator.getServerFactory();
	}

	public void saveServer(Server server) throws DotDataException {
		serverFactory.saveServer(server);
	}

	public Server getServer(String serverId) {
		return serverFactory.getServer(serverId);
	}

	public String readServerId() {
		String realPath = Config.CONTEXT.getRealPath("dotsecure") + java.io.File.separator + "server_id.dat";
		BufferedReader br = null;
		String serverId = null;
		try {
			br = new BufferedReader(new FileReader(new File(realPath)));
			serverId = br.readLine();
		} catch (FileNotFoundException e) {
			Logger.debug(ServerAPIImpl.class, "Server ID not found");
		} catch (IOException e) {
			Logger.error(ServerAPIImpl.class, "Could not read Server ID from File", e);
		} finally {
			try {
				if(br!=null)
					br.close();
			} catch (IOException e) {
				Logger.error(ServerAPIImpl.class, "Could not close BufferedReader for Server File", e);
			}
		}


        return serverId;

	}

	public  void writeServerIdToDisk(String serverId) throws IOException {
		String realPath = Config.CONTEXT.getRealPath("dotsecure") + java.io.File.separator + "server_id.dat";
		File serverFile = new File(realPath);

		if(serverFile.exists())
			serverFile.delete();

		OutputStream os = new FileOutputStream(serverFile);
		os.write(serverId.getBytes());
		os.flush();
		os.close();

	}

	public  void writeHeartBeatToDisk(String serverId) throws IOException {
		String realPath = Config.getStringProperty("ASSET_REAL_PATH", Config.CONTEXT.getRealPath(Config.getStringProperty("ASSET_PATH")))
    			+ java.io.File.separator + "server" + java.io.File.separator + serverId;
		File serverDir = new File(realPath);

		if(!serverDir.exists()) {
			serverDir.mkdirs();
		}

		File heartBeat = new File(realPath + java.io.File.separator + "heartbeat.dat");

		OutputStream os = new FileOutputStream(heartBeat);
		os.write(UtilMethods.dateToHTMLDate(new Date(), "yyyy-MM-dd H:mm:ss").getBytes());
		os.flush();
		os.close();

	}

	public List<Server> getAliveServers() throws DotDataException {
		return serverFactory.getAliveServers();
	}

	public void createServerUptime(String serverId) throws DotDataException {
		serverFactory.createServerUptime(serverId);
	}

	public void updateHeartbeat() throws DotDataException {
		serverFactory.updateHeartbeat(readServerId());
	}

	public void updateServer(Server server) throws DotDataException{
		serverFactory.updateServer(server);
	}

	public String[] getAliveServersIds() throws DotDataException {
		return serverFactory.getAliveServersIds();
	}

	public List<Server> getAllServers() throws DotDataException {
		return serverFactory.getAllServers();
	}

}
