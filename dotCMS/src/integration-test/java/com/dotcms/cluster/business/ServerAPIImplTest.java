package com.dotcms.cluster.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServerAPIImplTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testGetOldestServerReturnsProperServer() throws Exception{
        final ServerAPI serverApi = APILocator.getServerAPI();
        final File servers = new File(
                APILocator.getFileAssetAPI().getRealAssetsRootPath() + java.io.File.separator
                        + "server");

        if (!servers.exists()) {
            servers.mkdirs();
        }
        FileUtil.listFilesRecursively(servers, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && UUIDUtil.isUUID(pathname.getName());
            }
        }).forEach(f -> FileUtil.deltree(f));

        final List<Server> aliveServers = new ArrayList<>();

        for (int i = 0; i < 6; i++) {

            final Server server = Server.builder()
                    .withCachePort(i)
                    .withEsHttpPort(i)
                    .withIpAddress("10.0.0." + i)
                    .withLastHeartBeat(System.currentTimeMillis() + (i * 1000))
                    .withName("serverName" + i)
                    .withClusterId(ClusterFactory.getClusterId())
                    .withServerId(UUIDGenerator.generateUuid())
                    .build();
            serverApi.saveServer(server);

            serverApi.writeHeartBeatToDisk(server.serverId);

            aliveServers.add(server);
            ThreadUtils.sleep(1001);

        }

        final String oldestServer = serverApi.getOldestServer(
                aliveServers.stream().map(s -> s.serverId).collect(Collectors.toList()));
        assertEquals(oldestServer, aliveServers.get(0).serverId);

        FileUtil.listFilesRecursively(servers, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && UUIDUtil.isUUID(pathname.getName());
            }
        }).forEach(f -> FileUtil.deltree(f));
        assertEquals(serverApi.readServerId(), serverApi.getOldestServer());
    }

}
