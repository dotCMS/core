/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.cluster;

import static com.dotmarketing.util.StringUtils.isSet;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cluster.ClusterUtils;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.Base64;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.elasticsearch.common.settings.Settings.Builder;

public class ClusterFactory {



    private static boolean CLUSTER_INITED=false;
	private static List<Server> KNOWN_SERVERS=Collections.EMPTY_LIST;

    private static final String VERIFY_REQUIRED_TABLES =
            "SELECT dc.cluster_id, dc.cluster_salt, cs.server_id, cs.cluster_id, cs.name, "
                    + "cs.ip_address, cs.host, cs.cache_port, cs.es_transport_tcp_port, "
                    + "cs.es_network_port, cs.es_http_port, cs.key_, sl.id, sl.serverid, sl.license, "
                    + " sl.lastping FROM dot_cluster dc \n"
                    + " JOIN cluster_server cs\n"
                    + " ON dc.cluster_id = cs.cluster_id\n"
                    + " JOIN sitelic sl ON cs.server_id = sl.serverid";


    @WrapInTransaction
    public static void initialize() {

        ServerAPI serverAPI = APILocator.getServerAPI();
        try {

            serverAPI.getOrCreateMyServer();
            setUpLicense();
            rewireClusterIfNeeded();

            
        }  catch (Exception e) {
            Logger.error(ClusterFactory.class, "Could not save Server to DB", e);
            throw new DotStateException("Could not save Server to DB", e);
        }
        
    }
    
    @CloseDBIfOpened
    @VisibleForTesting
    public static boolean clusterReady() {
        if(CLUSTER_INITED) return true;
        // detect if the table schema is there
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()){
            DotConnect dc=new DotConnect();
            dc.setSQL(VERIFY_REQUIRED_TABLES);
            dc.loadResult(conn);
            CLUSTER_INITED=true;
            return CLUSTER_INITED;
        }
        catch(Exception ex) {
            // the table isn't yet set up
            Logger.warnAndDebug(ClusterFactory.class, "cluster table scheme not setup. will catch up later:" + ex.getMessage(), ex);
        }
        return false;
        
    }
    
    
    
    
    
    
    final private static String DOTCMS_CLUSTER_ID = "DOTCMS_CLUSTER_ID";
    final private static String DOTCMS_CLUSTER_SALT = "DOTCMS_CLUSTER_SALT";

    /**
     * This method returns the clusterId for the group of servers that this node belongs to. If this is
     * the first time startup, the server will try to resolve the clusterId in the
     * dotmarketing-config.properties, as a system property and then finally as an environmental
     * variable before just picking a random id
     * 
     * @returns
     */
    public static String getClusterId() {
        return getClusterData().getClusterId();
    }

    /**
     *
     * @return
     */
    public static String getClusterSalt() {
        return getClusterData().getClusterSalt();
    }

    /**
     * This method returns an Object with the cluster data for the group of servers that this node belongs to.
     * Cluster Data
     * If this is the first time startup, the server will try to resolve the clusterId in the
     * dotmarketing-config.properties, as a system property and then finally as an environmental
     * variable before just picking a random id
     *
     * @returns
     */
    public static ClusterData getClusterData() {
       return clusterData.get();
    }

    private static Lazy<ClusterData> clusterData = Lazy.of(ClusterFactory::writeClusterData);

    /**
     * Tries to resolve the clusterId & clusterSalt using db/environmental vars
     * @return
     */
    @WrapInTransaction
    private static ClusterData writeClusterData() {

            String clusterId = null;
            String clusterSalt = null;

            // does it exist in the db?

            final DotConnect dc = new DotConnect();
            try {
                final Optional<Tuple2<String, String>> clusterOptional = dc
                        .setSQL("select cluster_id, cluster_salt from dot_cluster")
                        .loadObjectResults().stream()
                        .map(map -> Tuple.of((String) map.get("cluster_id"),
                                (String) map.get("cluster_salt")))
                        .findFirst();
                if (clusterOptional.isPresent()) {
                    final Tuple2<String, String> tuple = clusterOptional.get();
                    if (isSet(tuple._1)) {
                        clusterId = tuple._1;
                    }

                    if (isSet(tuple._2)) {
                        clusterSalt = tuple._2;
                    }
                    //Only when both values are set we can initialize clusterData object.
                    if (isSet(clusterId) && isSet(clusterSalt)) {
                        return new ClusterData(clusterId, clusterSalt);
                    }
                }

                // is it in dotmarketing-config or System Properties?
                clusterId = Config.getStringProperty(DOTCMS_CLUSTER_ID,
                        System.getProperty(DOTCMS_CLUSTER_ID));
                clusterSalt = Config.getStringProperty(DOTCMS_CLUSTER_SALT,
                        System.getProperty(DOTCMS_CLUSTER_SALT));

                // is it an environmental variable?
                if (!UtilMethods.isSet(clusterId)) {
                    clusterId = System.getenv(DOTCMS_CLUSTER_ID);
                }

                // well, let's just pick something
                if (!UtilMethods.isSet(clusterId)) {
                    clusterId = UUIDGenerator.shorty();
                }

                if (!UtilMethods.isSet(clusterSalt)) {
                    clusterSalt = System.getenv(DOTCMS_CLUSTER_SALT);
                }

                if (!UtilMethods.isSet(clusterSalt)) {
                    clusterSalt = Base64.objectToString(Encryptor.generateKey());
                }

                dc.setSQL("insert into dot_cluster values (?,?)");
                dc.addParam(clusterId);
                dc.addParam(clusterSalt);
                dc.loadResult();
                return new ClusterData(clusterId, clusterSalt);

            } catch (DotDataException | EncryptorException e) {
                final String errorMessage = " Error computing cluster data.";
                Logger.error(ClusterFactory.class, errorMessage, e);
                throw new DotRuntimeException(errorMessage, e);
            }
    }

    @CloseDBIfOpened
    public static String getNextAvailablePort(String serverId, ServerPort port){
        return getNextAvailablePort(serverId, port, null);
    }

    @CloseDBIfOpened
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

    @WrapInTransaction
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


        Server currentServer = serverAPI.getCurrentServer();
        currentServer = Server.builder().withServer(currentServer).withIpAddress(getIPAdress()).build();
        serverAPI.updateServer( currentServer );

        LicenseManager.getInstance().takeLicenseFromRepoIfNeeded(false);


        if ( Config.getBooleanProperty( "ENABLE_SERVER_HEARTBEAT", true ) ) {
            LicenseUtil.updateLicenseHeartbeat();
        }
    }

    public static synchronized void rewireClusterIfNeeded() {
        if(!clusterReady()) {
            return;
        }
        if(!isEnterprise()) {
            return;
        }
		try{
			List<Server> aliveServers = APILocator.getServerAPI().getAliveServers();

            if (!aliveServers.equals(KNOWN_SERVERS) || !aliveServers
                    .contains(APILocator.getServerAPI().getCurrentServer()) ) {

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

        if(clusterReady()) {
            addMeToCacheIfNeeded();
            KNOWN_SERVERS  = APILocator.getServerAPI().getAliveServers();
        }else {
            Logger.info(ClusterFactory.class, "Cluster not yet active. Not rewiring");
        }
    }

     private static void addMeToCacheIfNeeded() throws DotDataException  {
         if(isEnterprise()) {

             final Server localServer = APILocator.getServerAPI().getOrCreateMyServer();

             try {
                 ((ChainableCacheAdministratorImpl) CacheLocator.getCacheAdministrator().
                     getImplementationObject()).setCluster(localServer);
                 ((ChainableCacheAdministratorImpl) CacheLocator.getCacheAdministrator().
                     getImplementationObject()).testCluster();
             } catch (Exception e) {
                 Logger.error(ClusterFactory.class, e.getMessage(), e);
             }

                 
             
         } else {
             CacheLocator.getCacheAdministrator().getTransport().shutdown(); 
         }
    }
     
     private static boolean isEnterprise() {
         
         return LicenseUtil.getLevel()> LicenseLevel.STANDARD.level;
     }

    @WrapInTransaction
    public static void removeNodeFromCluster() {
        removeNodeFromClusterNow();
    }

    @WrapInTransaction
    private static void removeNodeFromClusterNow() {
		if(ClusterUtils.isTransportAutoWire()) {
	      ChainableCacheAdministratorImpl cacheAdm = (ChainableCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject();
	      cacheAdm.shutdownChannel();
		}
    }
    

    /**
     * Get the current ES_TRANSPORT_TCP_PORT server port
     * @param serverId Server identification
     * @return Number  Port number
     * @throws DotDataException
     */
    @CloseDBIfOpened
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

    /**
     * Cluster Data Holder
     */
    public static class ClusterData {

        private final String clusterId;

        private final String clusterSalt;

        private ClusterData(final String clusterId, final String clusterSalt) {
            this.clusterId = clusterId;
            this.clusterSalt = clusterSalt;
        }

        public String getClusterId() {
            return clusterId;
        }

        public String getClusterSalt() {
            return clusterSalt;
        }
    }

}
