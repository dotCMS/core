package com.dotcms.cluster.business;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerAPIImpl implements ServerAPI {

    private static String SERVER_ID=null;
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
	@Override
    public Server getOrCreateServer(final String serverId) throws DotDataException {
	    Server tryServer = getServer(serverId);
        if(tryServer==null || tryServer.getServerId() ==null)  {
            LocalTransaction.wrap(() ->{
				getServerTransaction(serverId);
       
            });

        }
        

        return serverFactory.getServer(serverId);
    }

	private void getServerTransaction(String serverId) throws DotDataException {
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
            writeHeartBeatToDisk(newServer.getServerId());
        } catch (IOException e) {
            Logger.error(ClusterFactory.class, "Could not write Server ID to file system" , e);
        }
	}

	private File serverIdFile(){
	    
        String dynamicContentPath = ConfigUtils.getDynamicContentPath();

        
        String realPath;
        if ( dynamicContentPath.endsWith( File.separator ) ) {
            realPath = ConfigUtils.getDynamicContentPath() + "server_id.dat";
        } else {
            realPath = ConfigUtils.getDynamicContentPath() + File.separator + "server_id.dat";
        }
        Logger.debug(ServerAPIImpl.class, "Server Id " + realPath);

        return new File(realPath);

	}
	
	
	
	
	@Override
	public String readServerId()  {
	    // once set this should never change
		BufferedReader br = null;
		FileReader reader = null;
	    if(SERVER_ID==null){
    	    try{

        	    File serverFile = serverIdFile();
        
        	    if(!serverFile.exists()){
        	        writeServerIdToDisk(UUIDUtil.uuid());
        	    }

        	    reader = new FileReader(serverFile);
        		br =  new BufferedReader(reader);
        		SERVER_ID = br.readLine();
        		Logger.debug(ServerAPIImpl.class, "ServerID: " + SERVER_ID);
        
    
    	    } catch(IOException ioe){
    	        throw new DotStateException("Unable to read server id at " + serverIdFile() +
						" please make sure that the directory exists and is readable and writeable. If problems" +
						" persist, try deleting the file.  The system will recreate a new one on startup", ioe);
    	    } finally{
				try {
					if (reader != null){
						reader.close();
					}
					if (br != null){
						br.close();
					}

				} catch (IOException e) {
					Logger.error(this.getClass(), e.getMessage());
				}
			}
	    }
	    return SERVER_ID;
	}

	private  void writeServerIdToDisk(String serverId) throws IOException {


        File serverFile = serverIdFile();

		if(serverFile.exists()){
			serverFile.delete();
		}
		OutputStream os = new FileOutputStream(serverFile);
		os.write(serverId.getBytes());
		os.flush();
		os.close();

	}

	public  void writeHeartBeatToDisk(String serverId) throws IOException {

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

			OutputStream os = new FileOutputStream(heartBeat);
			os.write(UtilMethods.dateToHTMLDate(new Date(), "yyyy-MM-dd H:mm:ss").getBytes());
			os.flush();
			os.close();
		} else {
			Logger.warn(ServerAPIImpl.class, "ENABLE_SERVER_HEARTBEAT is set to false to we do not need to write to Disk " );
		}

	}

	public List<Server> getAliveServers() throws DotDataException {
		return serverFactory.getAliveServers();
	}

	public List<Server> getAliveServers(List<String> toExclude) throws DotDataException {
		return serverFactory.getAliveServers(toExclude);
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

	public void updateServerName(String serverId, String name) throws DotDataException {
		serverFactory.updateServerName(serverId, name);
	}

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
