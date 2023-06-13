package com.dotcms.enterprise.cluster;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.cluster.ClusterUtils;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;

import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import org.elasticsearch.common.settings.Settings.Builder;

public class ClusterFactory {

    private static final DotConcurrentFactory dotConcurrentFactory =
            DotConcurrentFactory.getInstance();

    private static String CLUSTER_ID=null;
    private static boolean CLUSTER_INITED=false;
	private static List<Server> KNOWN_SERVERS=Collections.EMPTY_LIST;

    private static final String VERIFY_REQUIRED_TABLES = "SELECT count(*) FROM dot_cluster dc "
            + "JOIN cluster_server cs ON dc.cluster_id = cs.cluster_id "
            + "JOIN sitelic sl ON cs.server_id = sl.serverid";


    public static void initialize() {

        // detect if the table schema is there
        try {
            DotConnect dc=new DotConnect();
            dc.setSQL(VERIFY_REQUIRED_TABLES);
            dc.loadResult();
        }
        catch(Exception ex) {
            // the table isn't yet set up
            Logger.warn(ClusterFactory.class, "table schema not ready for cluster setup. will catch up later");
            return;
        }finally{
        	DbConnectionFactory.closeSilently();
        }

        ServerAPI serverAPI = APILocator.getServerAPI();
        try {
            CLUSTER_INITED=true;
            serverAPI.getOrCreateMyServer();
            setUpLicense();
            rewireClusterIfNeeded();

            
        }  catch (Exception e) {
            Logger.error(ClusterFactory.class, "Could not save Server to DB", e);
            throw new DotStateException("Could not save Server to DB", e);
        }
        

    }




    public static String getClusterId() {
        if (CLUSTER_ID == null) {
            synchronized (ClusterFactory.class) {
                if (CLUSTER_ID == null) {
                    DotConnect dc = new DotConnect();
                    dc.setSQL("select cluster_id from dot_cluster");


                    try {
                        List<Map<String, Object>> results = dc.loadObjectResults();
                        if (!results.isEmpty()) {
                            CLUSTER_ID = (String) results.get(0).get("cluster_id");
                        } else {
                            final String cluster = UUID.randomUUID().toString();
                            dc.setSQL("insert into dot_cluster values (?)");
                            dc.addParam(cluster);
                            dc.loadResult();

                            CLUSTER_ID = cluster;
                        }

                    } catch (Exception e) {
                        Logger.error(ClusterFactory.class, "Could not get Cluster ID from db");
                        CLUSTER_ID = null;
                    }
                }

            }
        }

        return CLUSTER_ID;
    }

    public static String getNextAvailablePort(String serverId, ServerPort port){
        return getNextAvailablePort(serverId, port, null);
    }

    public static String getNextAvailablePort(String serverId, ServerPort port, Builder externalSettings) {
        DotConnect dc = new DotConnect();
        dc.setSQL("select max(" + port.getTableName()+ ") as port from cluster_server where ip_address = (select s.ip_address from cluster_server s where s.server_id = ?) "
                + "or ('localhost' = (select s.ip_address from cluster_server s where s.server_id = ?) and ip_address = '127.0.0.1') "
                + "or('127.0.0.1' = (select s.ip_address from cluster_server s where s.server_id = ?) and ip_address = 'localhost') ");

        dc.addParam(serverId);
        dc.addParam(serverId);
        dc.addParam(serverId);

        Number maxPort  = null;
        String freePort = null;

        if (UtilMethods.isSet(externalSettings)){
            freePort = externalSettings.get(port.getPropertyName());

            if (!UtilMethods.isSet(freePort)){
                freePort = port.getDefaultValue();
            }
        }else{
            freePort = Config.getStringProperty(port.getPropertyName(), port.getDefaultValue());
        }


        try {
            List<Map<String,Object>> results = dc.loadObjectResults();
            if(!results.isEmpty()) {
                maxPort = (Number) results.get(0).get("port");
                freePort = UtilMethods.isSet(maxPort)?Integer.toString(maxPort.intValue()+1):freePort;
                int pp=Integer.parseInt(freePort);
                while(!UtilMethods.isPortFree(pp)) {
               		pp = pp + 1;
                }
                freePort=Integer.toString(pp);
            }

        } catch (DotDataException e) {
            Logger.error(ClusterFactory.class, "Could not get Available server port", e);
        }

        return freePort.toString();
    }

    public static void setUpLicense() throws Exception {
    	
    	
    	
    	
        ServerAPI serverAPI = APILocator.getServerAPI();
        final String serverId = APILocator.getServerAPI().readServerId();
        List<String> myself = new ArrayList<>();
        myself.add(serverId);

        List<Server> aliveServers = serverAPI.getAliveServers(myself);
        if(aliveServers!=null && !aliveServers.isEmpty()) {
            Server randomAliveServer = aliveServers.get(0);
            String randomServerId = randomAliveServer.getServerId();

            if (randomServerId != null) {
                String
                serverFilePath =
                Config.getStringProperty("ASSET_REAL_PATH",
                        FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH", "/assets")))
                        + java.io.File.separator + "server" + java.io.File.separator + randomServerId
                        + java.io.File.separator + "heartbeat.dat";
                File file = new File(serverFilePath);
                if (!file.exists()) {
                    Logger.warn(ClusterFactory.class, "");
                    Logger.warn(ClusterFactory.class,
                            "INVALID SERVER IN CLUSTER OR SHARED ASSETS CANNOT BE READ/WRITTEN");
                    Logger.warn(ClusterFactory.class, "Looking for file :" + file);
                    Logger.warn(ClusterFactory.class, "Could not find server data for server:" + randomServerId);
                    Logger.warn(ClusterFactory.class, "Is this server part of the cluster?");
                    Logger.warn(ClusterFactory.class, "");

                    AdminLogger.log(ClusterFactory.class, "addNodeToCluster", "");
                    AdminLogger.log(ClusterFactory.class, "addNodeToCluster",
                            "INVALID SERVER IN CLUSTER OR SHARED ASSETS CANNOT BE READ/WRITTEN");
                    AdminLogger.log(ClusterFactory.class, "addNodeToCluster", "Looking for file :" + file);
                    AdminLogger.log(ClusterFactory.class, "addNodeToCluster",
                            "Could not find server data for server:" + randomServerId);
                    AdminLogger.log(ClusterFactory.class, "addNodeToCluster", "Is this server part of the cluster?");
                    AdminLogger.log(ClusterFactory.class, "addNodeToCluster", "");
                }
            }
        }


        Server currentServer;
        try {
            HibernateUtil.startTransaction();

            currentServer = serverAPI.getCurrentServer();
            currentServer = Server.builder().withServer(currentServer).withIpAddress(getIPAdress()).build();

            serverAPI.updateServer( currentServer );

            HibernateUtil.commitTransaction();
        } catch ( Exception ex ) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch ( DotHibernateException e ) {
                Logger.error( ClusterFactory.class, "Can't rollback on editing a server", e );
            }
        } finally {
            try {
                HibernateUtil.closeSession();
            } catch ( DotHibernateException e ) {
                Logger.error( ClusterFactory.class, "Can't close session after editing a server", e );
            }
        }

        LicenseManager.getInstance().takeLicenseFromRepoIfNeeded(false);


        if ( Config.getBooleanProperty( "ENABLE_SERVER_HEARTBEAT", true ) ) {
            LicenseUtil.updateLicenseHeartbeat();
        }
    }

    public static synchronized void rewireClusterIfNeeded() {

		try{
			List<Server> aliveServers = APILocator.getServerAPI().getAliveServers();
		
			if(!aliveServers.equals(KNOWN_SERVERS)){

				Logger.info(ClusterFactory.class, "Cluster topography has changed. This might force a rewire");
				Logger.info(ClusterFactory.class, "Expecting Servers:" + KNOWN_SERVERS);
				Logger.info(ClusterFactory.class, "Found Servers:" + aliveServers);
				
				rewireCluster();
			}
		}
		catch(Exception e){
			Logger.error(ClusterFactory.class, "Unable to rewire cluster:" + e.getMessage());
			Logger.error(ClusterFactory.class, "servers:" + KNOWN_SERVERS);
			
			
			throw new DotRuntimeException(e);
		}
    }
    
    public static void rewireCluster() throws Exception {
        if(CLUSTER_INITED) {
            Logger.info(ClusterFactory.class, "Cluster Active. Rewiring");
            addMeToCacheIfNeeded();
            addMeToESIfNeeded();
            KNOWN_SERVERS  = APILocator.getServerAPI().getAliveServers();
        }else {
            Logger.info(ClusterFactory.class, "Cluster not yet active. Not rewiring");
        }
    }

     private static void addMeToCacheIfNeeded() throws DotDataException  {
         if(LicenseUtil.getLevel()> LicenseLevel.STANDARD.level) {

             final Server localServer = APILocator.getServerAPI().getOrCreateMyServer();
             final DotSubmitter submitter =
                 dotConcurrentFactory.getSubmitter();
             submitter.submit(() -> {
                     try {
                         ((ChainableCacheAdministratorImpl) CacheLocator.getCacheAdministrator().
                             getImplementationObject()).setCluster(localServer);
                         ((ChainableCacheAdministratorImpl) CacheLocator.getCacheAdministrator().
                             getImplementationObject()).testCluster();
                     } catch (Exception e) {
                         Logger.error(ClusterFactory.class, e.getMessage(), e);
                     }

                 }
             );
         }
    }

     
    private static void addMeToESIfNeeded() throws Exception  {
        if(LicenseUtil.getLevel()> LicenseLevel.STANDARD.level) {
            ESClient esClient = new ESClient();
            esClient.restartNode(esClient.getESNodeSettings());
        }
    }

    
    public static void removeNodeFromCluster() {
        removeNodeFromClusterNow();
    }

    private static void removeNodeFromClusterNow() {
		if(ClusterUtils.isTransportAutoWire()) {
	      ChainableCacheAdministratorImpl cacheAdm = (ChainableCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject();
	      cacheAdm.shutdownChannel();
		}
		if(ClusterUtils.isESAutoWire()) {
	      ESClient esClient = new ESClient();
	      esClient.removeClusterNode();
		}
    }
    

    /**
     * Get the current ES_TRANSPORT_TCP_PORT server port
     * @param serverId Server identification
     * @return Number  Port number
     * @throws DotDataException
     */
    public static Number getESPort(String serverId) throws DotDataException{
    	Number port = null;
        DotConnect dc = new DotConnect();
    	dc.setSQL("select " + ServerPort.ES_TRANSPORT_TCP_PORT.getTableName()+ " as port from cluster_server where server_id = ? ");
    	dc.addParam(serverId);
    	List<Map<String,Object>> results = dc.loadObjectResults();
    	if(!results.isEmpty()) {
    		port = (Number) results.get(0).get("port");
    	}
        return port;
    }

    /**
     * Validates the IP against the interfaces
     *
     */
    public static boolean isValidIP(String ipAddress){
    	try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()){
                NetworkInterface current = interfaces.nextElement();
                if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
                Enumeration<InetAddress> addresses = current.getInetAddresses();
                while (addresses.hasMoreElements()){
                    InetAddress current_addr = addresses.nextElement();
                    // if this is a loopback or multicast address skip to the next one
                    if(current_addr.isLoopbackAddress() || current_addr.isMulticastAddress()) continue;
                    // if this is a link address or is not local address skip to the next one
                    if(current_addr.isLinkLocalAddress() || !current_addr.isSiteLocalAddress()) continue;
                    else {
                        if(current_addr instanceof Inet4Address) {
                            String address = current_addr.getHostAddress();
                            if(address.equals(ipAddress))
                            	return true;
                        }
                    }
                }
            }
        }catch (SocketException e) {
            Logger.error(ClusterFactory.class, "Error trying to get Server Ip Address.", e);
        }
    	return false;
    }

    /**
     * Util method to find IP Adress searching through properties, InetAddress and Network Interface. This mehod will
     * also validate the IP returned.
     *
     * @return IPAdress as a String of the local node/server.
     */
    public static String getIPAdress(){

        // Get IP Address
        String address = null;

        try {
            // localhost from system
            InetAddress addr = InetAddress.getLocalHost();
            byte[] ipAddr = addr.getAddress();
            addr = InetAddress.getByAddress(ipAddr);
            if(isValidIP(addr.getHostAddress())){
                address = addr.getHostAddress();
            }else{
                Logger.info(ClusterFactory.class, "Could not resolved local ip address for hostname of the local machine");
            }
        } catch(UnknownHostException e) {
            Logger.info(ClusterFactory.class, "Could not resolved local ip address for hostname of the local machine");
        }

        // if InetAddress.getLocalHost() is not valid, look for the first valid IP on the
        // interfaces
        if(address == null){
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                my_loop:
                while (interfaces.hasMoreElements()){
                    NetworkInterface current = interfaces.nextElement();
                    if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
                    Enumeration<InetAddress> addresses = current.getInetAddresses();
                    while (addresses.hasMoreElements()){
                        InetAddress current_addr = addresses.nextElement();
                        // if this is a loopback or multicast address skip to the next one
                        if(current_addr.isLoopbackAddress() || current_addr.isMulticastAddress()) continue;
                        // if this is a link address or is not local address skip to the next one
                        if(current_addr.isLinkLocalAddress() || !current_addr.isSiteLocalAddress()) continue;
                        else {
                            if(current_addr instanceof Inet4Address) {
                                address = current_addr.getHostAddress();
                                break my_loop;
                            }
                        }

                    }
                }
            }catch (SocketException e) {
                Logger.error(ClusterFactory.class, "Error trying to get Server Ip Address.", e);
            }
        }

        // if all else fails use "localhost"
        if(address == null){
        	Logger.info(ClusterFactory.class, "Could not resolved local ip address from interfaces, using 'localhost'.");
            address = "localhost";
        }

        return address;
    }
}