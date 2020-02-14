package com.dotcms.cache.lettuce;

import static org.junit.Assert.assertTrue;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.DotCacheAdministrator;

public class LettuceTransportTest {

    static LettuceTransport transport1 = new LettuceTransport(LettuceClient.getInstance(), "server-1", "cluster1");
    static LettuceTransport transport2 = new LettuceTransport(LettuceClient.getInstance(), "server-2", "cluster1");
    static LettuceTransport transport3 = new LettuceTransport(LettuceClient.getInstance(), "server-3", "cluster1");


    @BeforeClass
    public static void startup() throws Exception {
        IntegrationTestInitService.getInstance().init();
        transport1.init();
        transport2.init();
        transport3.init();

        
    }

    
    
    /**
     * The the servers getting info messages?
     * @throws Exception
     */
    @Test
    public void test_servers_can_hear_each_other() throws Exception {


        final String test1 = "test from one!!!";
        transport1.send(MessageType.INFO, test1);

        Thread.sleep(2000);

        assert (transport2.messagesIn != null);
        assert (transport2.messagesIn.isEmpty() == false);
        assert (transport2.messagesIn.stream().anyMatch(m -> m.type == MessageType.INFO && m.message.equals(test1)));
        assert (transport3.messagesIn != null);
        assert (transport3.messagesIn.isEmpty() == false);
        assert (transport3.messagesIn.stream().anyMatch(m -> m.type == MessageType.INFO && m.message.equals(test1)));

        final String test2 = "test from two!!!";
        transport2.send(MessageType.INFO, test2);
        Thread.sleep(2000);
        assert (transport1.messagesIn != null);
        assert (transport1.messagesIn.isEmpty() == false);
        assert (transport1.messagesIn.stream().anyMatch(m -> m.type == MessageType.INFO && m.message.equals(test2)));
        assert (transport3.messagesIn != null);
        assert (transport3.messagesIn.isEmpty() == false);
        assert (transport3.messagesIn.stream().anyMatch(m -> m.type == MessageType.INFO && m.message.equals(test2)));



    }

    /**
     * Are all servers responding to a ping?
     * 
     * @throws Exception
     */
    @Test
    public void test_cluster_ping() throws Exception {


        transport1.testCluster();
        Thread.sleep(2000);

        assert (transport2.messagesIn != null);
        assert (transport2.messagesIn.isEmpty() == false);
        assert (transport2.messagesIn.stream().anyMatch(m -> m.type == MessageType.PING && m.message.equals("server-1")));
        assert (transport3.messagesIn != null);
        assert (transport3.messagesIn.isEmpty() == false);
        assert (transport3.messagesIn.stream().anyMatch(m -> m.type == MessageType.PING && m.message.equals("server-1")));

        // check that we got pong back

        assert (transport1.messagesIn.stream().anyMatch(m -> m.type == MessageType.PONG && m.message.equals("server-2")));
        assert (transport1.messagesIn.stream().anyMatch(m -> m.type == MessageType.PONG && m.message.equals("server-3")));

    }

    /**
     * Are all servers responding to the cluster?
     * 
     * @throws Exception
     */
    @Test
    public void test_validate_cache_in_cluster() throws Exception {

        long start = System.currentTimeMillis();

        // we expect 3 servers
        Map<String, Boolean> map = transport1.validateCacheInCluster("0", 3, 5);

        long took = System.currentTimeMillis() - start;
        assert (took < 2000);

        assert (map.containsKey("server-1"));
        assert (map.containsKey("server-2"));
        assert (map.containsKey("server-3"));



    }

    /**
     * this tests sending cache invalidations
     * including sending multiple invalidations in a single message
     * @throws Exception
     */
    
    @Test
    public void test_cache_invalidations() throws Exception {

        
        DotCacheAdministrator cache = new ChainableCacheAdministratorImpl(transport1);
        cache.initProviders();
        cache.flushAll();
        
        boolean works = false;
        int i=0;
        
        while(!works && ++i<20) {
            works = transport2.messagesIn.stream().anyMatch(m -> m.type == MessageType.INVALIDATE && m.message.equals("0:" + DotCacheAdministrator.ROOT_GOUP));
            Thread.sleep(100);
        }
        assertTrue("transport 2 got the invalidations", works);
        
        works = false; i=0;
        while(!works && ++i<20) {
            works =  (transport3.messagesIn.stream().anyMatch(m -> m.type == MessageType.INVALIDATE && m.message.equals("0:" + DotCacheAdministrator.ROOT_GOUP)));
            Thread.sleep(100);
        }
        assertTrue("transport 2 got the invalidations", works);
        

        
        cache.flushGroup(CacheLocator.getIdentifierCache().getPrimaryGroup());
        cache.flushGroup(CacheLocator.getIdentifierCache().get404Group());
        cache.remove("testestKey","testsetGoup"); 
        

        
        works = false; i=0;
        while(!works && ++i<20) {
            works =  (transport2.messagesIn.stream().anyMatch(m -> m.type == MessageType.INVALIDATE && m.message.equalsIgnoreCase("0:" + CacheLocator.getIdentifierCache().getPrimaryGroup())));
            Thread.sleep(100);
        }
        assertTrue("transport 2 got the invalidations", works);
        
        
        works = false; i=0;
        while(!works && ++i<20) {
            works =  (transport3.messagesIn.stream().anyMatch(m -> m.type == MessageType.INVALIDATE && m.message.equalsIgnoreCase("0:" + CacheLocator.getIdentifierCache().getPrimaryGroup())));
            Thread.sleep(100);
        }
        assertTrue("transport 3 got the invalidations", works);
     
        works = false; i=0;
        while(!works && ++i<20) {
            works =  (transport2.messagesIn.stream().anyMatch(m -> m.type == MessageType.INVALIDATE && m.message.equalsIgnoreCase("0:" + CacheLocator.getIdentifierCache().get404Group())));
            Thread.sleep(100);
        }
        assertTrue("transport2 got the invalidations", works);
        
        
        works = false; i=0;
        while(!works && ++i<20) {
            works =  (transport3.messagesIn.stream().anyMatch(m -> m.type == MessageType.INVALIDATE && m.message.equalsIgnoreCase("0:" + CacheLocator.getIdentifierCache().get404Group())));
            Thread.sleep(100);
        }
        
        assertTrue("transport3 got the invalidations", works);
       



    }

    /**
     * Are all servers responding to the cluster?
     * 
     * @throws Exception
     */
    @Test
    public void test_flush_all() throws Exception {

        long start = System.currentTimeMillis();

        // we expect 3 servers
        Map<String, Boolean> map = transport1.validateCacheInCluster("0", 3, 5);

        long took = System.currentTimeMillis() - start;
        assert (took < 2000);

        assert (map.containsKey("server-1"));
        assert (map.containsKey("server-2"));
        assert (map.containsKey("server-3"));



    }

    
    
    
    
    
    
    
    @AfterClass
    public static void shutdown() throws Exception {
        transport1.shutdown();
        transport2.shutdown();
        transport3.shutdown();
        Thread.sleep(2000);
    }



}
