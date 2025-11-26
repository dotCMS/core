package com.dotcms.cluster.business;

import io.vavr.Lazy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.FileUtil;

public class ServerAPIImpl implements ServerAPI {


    private final ServerFactory serverFactory;

    public ServerAPIImpl() {
        serverFactory = FactoryLocator.getServerFactory();
    }

    @CloseDBIfOpened
    public void saveServer(Server server) throws DotDataException {
        serverFactory.saveServer(server);
    }

    @CloseDBIfOpened
    public Server getServer(String serverId) throws DotDataException {
        return serverFactory.getServer(serverId);
    }

    @CloseDBIfOpened
    @Override
    public Server getOrCreateMyServer() throws DotDataException {
        final Server tryServer = getServer(readServerId());

        if (tryServer == null || tryServer.getServerId() == null) {

            createMyServer();
        }

        return getServer(readServerId());
    }

    @WrapInTransaction
    private void createMyServer() throws DotDataException {

        Server.Builder serverBuilder = Server.builder().withServerId(readServerId());

        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getHostName();

        } catch (UnknownHostException e) {
            Logger.error(ClusterFactory.class, "Error trying to get the host name. ", e);
        }

        final String ipAddress = ClusterFactory.getIPAdress();
        serverBuilder.withIpAddress(ipAddress);

        serverBuilder.withName(hostName);
        serverBuilder.withClusterId(ClusterFactory.getClusterId());

        // set up ports

        String port = ClusterFactory.getNextAvailablePort(readServerId(), ServerPort.CACHE_PORT);
        Config.setProperty(ServerPort.CACHE_PORT.getPropertyName(), port);
        serverBuilder.withCachePort(Integer.parseInt(port));

        port = ClusterFactory.getNextAvailablePort(readServerId(), ServerPort.ES_HTTP_PORT);
        serverBuilder.withEsHttpPort(Integer.parseInt(port));
        saveServer(serverBuilder.build());

        try {
            writeHeartBeatToDisk();
        } catch (IOException e) {
            Logger.error(ClusterFactory.class, "Could not write Server ID to file system", e);
        }
    }





    @Override
    public String readServerId() {

        return SERVER_ID.get();
    }



    @Override
    public String getOldestServer() throws DotDataException, IOException {
        
        
        
        return getReindexingServers().stream().findFirst().get();
        
    }
    



    @VisibleForTesting
    @Override
    public void writeHeartBeatToDisk(final String serverId) throws IOException {

        // First We need to check if the heartbeat job is enable.
        if (Config.getBooleanProperty("ENABLE_SERVER_HEARTBEAT", true)) {
            String realPath = APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                    + File.separator + serverId;

            File serverDir = new File(realPath);
            if (!serverDir.exists()) {
                serverDir.mkdirs();
            }

            File heartBeat = new File(realPath + File.separator + "heartbeat.dat");

            try (OutputStream os = Files.newOutputStream(heartBeat.toPath())) {
                os.write(String.valueOf(System.currentTimeMillis()).getBytes());
            }
        } else {
            Logger.warn(ServerAPIImpl.class, "ENABLE_SERVER_HEARTBEAT is set to false to we do not need to write to Disk ");
        }

    }

    public void writeHeartBeatToDisk() throws IOException {
        writeHeartBeatToDisk(readServerId());

    }

    @CloseDBIfOpened
    @Override
    public List<Server> getAliveServers() throws DotDataException {
        return serverFactory.getAliveServers();
    }
    
    @CloseDBIfOpened
    @Override
    public List<String> getReindexingServers() throws DotDataException {
        return serverFactory.getReindexingServers();
    }
    
    @CloseDBIfOpened
    @Override
    public List<Server> getAliveServers(List<String> toExclude) throws DotDataException {
        return serverFactory.getAliveServers(toExclude);
    }

    @WrapInTransaction
    @Override
    public void updateServer(Server server) throws DotDataException {
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
    public void removeServerFromClusterTable(String serverId) throws DotDataException {
        serverFactory.removeServerFromClusterTable(serverId);
    }

    @CloseDBIfOpened
    @Override
    public List<Server> getInactiveServers() throws DotDataException {

        Logger.debug(this, ()->"Getting the inactiver servers");
        return serverFactory.getInactiveServers();
    }

    @Override
    public Server getCurrentServer() throws DotDataException {

        return getOrCreateMyServer();
    }

    /**
     * Gets the servers start time
     *
     * @return milliseconds representing the date-time when the server started.
     */
    @Override
    public long getServerStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }
}
