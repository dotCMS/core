package com.dotcms.cluster.business;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

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
			Logger.debug(getClass(), "Server ID not found");
		} catch (IOException e) {
			Logger.error(getClass(), "Could not read Server ID from File", e);
		} finally {
			try {
				if(br!=null)
					br.close();
			} catch (IOException e) {
				Logger.error(getClass(), "Could not close BufferedReader for Server File", e);
			}
		}


        return serverId;

	}

	public  void writeServerId(byte[] data) throws IOException {
		String realPath = Config.CONTEXT.getRealPath("dotsecure") + java.io.File.separator + "server_id.dat";
		File serverFile = new File(realPath);

		if(serverFile.exists())
			serverFile.delete();

		OutputStream os = new FileOutputStream(serverFile);
		os.write(data);
		os.flush();
		os.close();

	}

	public void createServerUptime(String serverId) throws DotDataException {
		serverFactory.createServerUptime(serverId);
	}

	public void updateHeartbeat() throws DotDataException {
		serverFactory.updateHeartbeat(readServerId());
	}

}
