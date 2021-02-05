package com.dotcms.cluster.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.cluster.bean.Server;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.UUIDGenerator;
import io.vavr.control.Try;

public class ServerAPIImplTest {

    final static List<Server> fakeServers= new ArrayList<>();
    static ServerAPI serverApi;

    
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        serverApi = APILocator.getServerAPI();

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
            fakeServers.add(server);
            serverApi.saveServer(server);
        }
        
        
        // create fake licenses for each server with random startup times
        DotConnect dc = new DotConnect();
        fakeServers.forEach(server->{            
            dc.setSQL("INSERT INTO sitelic(id,serverid,license,lastping,startup_time) VALUES(?,?,?,?,?)")
                .addParam(UUIDGenerator.generateUuid())
                .addParam(server.getServerId())
                .addParam("FAKE_LICENSE:" + System.currentTimeMillis())
                .addParam(new Date())
                .addParam(new Random().nextInt(30000000))
                .getResult();
        });
        
        
        
        
    }
    
    /**
     * delete the fake licenses and servers
     * @throws Exception
     */
    @AfterClass
    public static void cleanupAfter() throws Exception {
        DotConnect dc = new DotConnect();
        dc.setSQL("delete from sitelic where license like 'FAKE_LICENSE:%'").loadResult();
        fakeServers.forEach(server->{      
           Try.run(()-> serverApi.removeServerFromClusterTable(server.getServerId()));
        });
        
    }
    
    /**
     * Create and register fake server licenses, then check to make sure that we get the oldest one when
     * requested
     * 
     * @throws Exception
     */
    @Test
    public void testGetOldestServerReturnsProperServer() throws Exception{
        // we get the alive servers ordered by startup_time asc
        final List<Server> aliveServers = serverApi.getAliveServers();
        final String oldestServerId = serverApi.getOldestServer();

        assertTrue("We have 6 servers", aliveServers.size()==6);
        for(Server server : aliveServers) {
            Logger.error("SERVERS LOG:  ", "SERVERS LOG:  " + server.getServerId() + " " + server.getLicenseSerial()+ " " + server.getStartupTime());
        }

        // the oldest server is the first alive server ordered by startup_time asc
        assertEquals(oldestServerId, aliveServers.get(0).serverId);
        
        final Server oldestServer = serverApi.getServer(oldestServerId);
        assertEquals(oldestServer, aliveServers.get(0));
    }

    @Test
    public void testGetServerStartTime() {
        assertNotNull(serverApi.getServerStartTime());
    }

}
