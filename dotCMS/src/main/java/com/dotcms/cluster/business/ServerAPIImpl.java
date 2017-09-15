package com.dotcms.cluster.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerAPIImpl implements ServerAPI {

    private static String SERVER_ID=null;
	private final ServerFactory serverFactory;

	public ServerAPIImpl() {
		serverFactory = FactoryLocator.getServerFactory();
	}

	@WrapInTransaction
	public void saveServer(Server server) throws DotDataException {
		serverFactory.saveServer(server);
	}

	@CloseDBIfOpened
	public Server getServer(String serverId) throws DotDataException{
		return serverFactory.getServer(serverId);
	}

	@WrapInTransaction
	@Override
    public Server getOrCreateServer(final String serverId) throws DotDataException {
	    final Server tryServer = getServer(serverId);

        if(tryServer == null || tryServer.getServerId() == null)  {
			createNewServerTransaction(serverId);
        }

        return serverFactory.getServer(serverId);
    }

	private void createNewServerTransaction(String serverId) throws DotDataException {
		Server newServer = new Server();
		newServer.setServerId(serverId);
		newServer.setIpAddress(ClusterFactory.getIPAdress());

		String hostName = "localhost";
		try {
            hostName = InetAddress.getLocalHost().getHostName();

        } catch (UnknownHostException e) {
            Logger.error(ClusterFactory.class, "Error trying to get the host name. ", e);
        }

		newServer.setName(hostName);

		saveServer(newServer);

		// set up ports

		String port=ClusterFactory.getNextAvailablePort(newServer.getServerId(), ServerPort.CACHE_PORT);
		Config.setProperty(ServerPort.CACHE_PORT.getPropertyName(), port);
		newServer.setCachePort(Integer.parseInt(port));

		port=new ESClient().getNextAvailableESPort(newServer.getServerId(), newServer.getIpAddress(),
                String.valueOf(newServer.getEsTransportTcpPort()));
		Config.setProperty(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName(), port);
		newServer.setEsTransportTcpPort(Integer.parseInt(port));

		port=ClusterFactory.getNextAvailablePort(newServer.getServerId(), ServerPort.ES_HTTP_PORT);
		Config.setProperty(ServerPort.ES_HTTP_PORT.getPropertyName(), port);
		newServer.setEsHttpPort(Integer.parseInt(port));

		updateServer(newServer);

		try {
            writeHeartBeatToDisk();
        } catch (IOException e) {
            Logger.error(ClusterFactory.class, "Could not write Server ID to file system" , e);
        }
	}

	private File serverIdFile(){
	    
	    String realPath = ConfigUtils.getDynamicContentPath() 
	                    + File.separator 
	                    + "license"  
	                     + File.separator 
	                    + "server_id.dat";
        
        Logger.debug(ServerAPIImpl.class, "Server Id " + realPath);

        return new File(realPath);
	}
	

	@Override
	public String readServerId()  {
	    // once set this should never change

	    if(SERVER_ID==null){
    	    try{
        	    File serverFile = serverIdFile();
        	    if(!serverFile.exists()){
        	        writeServerIdToDisk(UUIDUtil.uuid());
        	    }
        	    
        	    try (BufferedReader br = Files.newBufferedReader(serverFile.toPath())) {
            		SERVER_ID = br.readLine();
            		Logger.debug(ServerAPIImpl.class, "ServerID: " + SERVER_ID);
        	    }
    
    	    } catch(IOException ioe){
    	        throw new DotStateException("Unable to read server id at " + serverIdFile() +
						" please make sure that the directory exists and is readable and writeable. If problems" +
						" persist, try deleting the file.  The system will recreate a new one on startup", ioe);
    	    } 
	    }
	    return SERVER_ID;
	}

	private  void writeServerIdToDisk(String serverId) throws IOException {

        File serverFile = serverIdFile();
        serverFile.mkdirs();
        serverFile.delete();

        try(OutputStream os = Files.newOutputStream(serverFile.toPath())){
            os.write(serverId.getBytes());
        }
	}

	public  void writeHeartBeatToDisk() throws IOException {
		String serverId = readServerId();
		//First We need to check if the heartbeat job is enable.
		if ( Config.getBooleanProperty("ENABLE_SERVER_HEARTBEAT", true) ) {
			String realPath = APILocator.getFileAssetAPI().getRealAssetsRootPath()
					+ java.io.File.separator
					+ "server"
					+ java.io.File.separator
					+ serverId;

			File serverDir = new File(realPath);
			if(!serverDir.exists()) {
				serverDir.mkdirs();
			}

			File heartBeat = new File(realPath + java.io.File.separator + "heartbeat.dat");

			try(OutputStream os = Files.newOutputStream(heartBeat.toPath())){
			    os.write(UtilMethods.dateToHTMLDate(new Date(), "yyyy-MM-dd H:mm:ss").getBytes());
			}
		} else {
			Logger.warn(ServerAPIImpl.class, "ENABLE_SERVER_HEARTBEAT is set to false to we do not need to write to Disk " );
		}

	}

	@CloseDBIfOpened
	@Override
	public List<Server> getAliveServers() throws DotDataException {
		return serverFactory.getAliveServers();
	}

	@CloseDBIfOpened
	@Override
	public List<Server> getAliveServers(List<String> toExclude) throws DotDataException {
		return serverFactory.getAliveServers(toExclude);
	}

	@WrapInTransaction
	@Override
	public void createServerUptime() throws DotDataException {
		serverFactory.createServerUptime();
	}

	@WrapInTransaction
	@Override
	public void updateHeartbeat() throws DotDataException {

	    serverFactory.updateHeartbeat(readServerId());
	}

	@WrapInTransaction
	@Override
	public void updateServer(Server server) throws DotDataException{
		serverFactory.updateServer(server);
	}

	@CloseDBIfOpened
	@Override
	public String[] getAliveServersIds() throws DotDataException {
		return serverFactory.getAliveServersIds();
	}

	@CloseDBIfOpened
	@Override
	public List<Server> getAllServers() throws DotDataException {
		return serverFactory.getAllServers();
	}

	@WrapInTransaction
	@Override
	public void updateServerName(String serverId, String name) throws DotDataException {
		serverFactory.updateServerName(serverId, name);
	}

	@WrapInTransaction
	@Override
	public void removeServerFromClusterTable(String serverId) throws DotDataException, IOException{
		serverFactory.removeServerFromClusterTable(serverId);
	}

	@Override
	public List<Server> getInactiveServers() throws DotDataException{
		List<Server> inactiveServers = new CopyOnWriteArrayList<>(getAllServers());
		List<Server> aliveServers = getAliveServers();

		for(Server aliveServer: aliveServers){
			for(Server inactiveServer : inactiveServers){
				if(inactiveServer.getServerId().equals(aliveServer.getServerId())){
					inactiveServers.remove(inactiveServer);
				}
			}
		}
		
		return inactiveServers;
	}


	@Override
	public Server getCurrentServer() throws DotDataException {

	    return getOrCreateServer(readServerId());
	}
}
