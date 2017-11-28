package com.dotmarketing.business.cache.provider.ignite;

import com.dotcms.enterprise.cluster.ClusterFactory;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.FileSystemConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.TransactionConfiguration;
import org.apache.ignite.igfs.IgfsMode;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.transactions.TransactionConcurrency;

public class IgniteClient {
    private final Ignite ignite;
    private final static String IGNITE_CONFIG_FILE_NAME = "/ignite-dotcms.xml";
    private final static String INSTANCE_PREFIX = "dotCMS_";
    private final static String FILE_PREFIX = "dotCMS_FS_";
    private final String myCluster;
    private final long started = System.currentTimeMillis();
    public final String instanceName;

    private IgniteClient() {
        this(ClusterFactory.getClusterId().substring(0, 8));
    }

    protected IgniteClient(String clusterId) {
        myCluster = clusterId;
        instanceName = INSTANCE_PREFIX + myCluster + "_" + started;
        ignite = initIgnite();

    }

    private Ignite initIgnite() {
        Ignite fireUp;
        try (InputStream in = IgniteCacheProvider.class.getResourceAsStream(IGNITE_CONFIG_FILE_NAME)) {
            if (in != null) {
                fireUp = Ignition.start(IGNITE_CONFIG_FILE_NAME);
                return fireUp;
            }
        } catch (IOException e) {
            Logger.info(IgniteCacheProvider.class, "No " + IGNITE_CONFIG_FILE_NAME + " found, starting ignite with defaults");
        }


        /***
         * 
         * THERE IS A BUG IN IGNITE 2.3 https://issues.apache.org/jira/browse/IGNITE-6944 to run
         * ignite 2.3.0, you need to use the jdk marshaller JdkMarshaller marshaller = new
         * JdkMarshaller(); cfg.setMarshaller(marshaller);
         */

        IgniteConfiguration cfg = new IgniteConfiguration().setIgniteInstanceName(instanceName)
                .setClientMode(Config.getBooleanProperty("cache.ignite.clientMode", true));

        TransactionConfiguration txCfg =
                new TransactionConfiguration().setDefaultTxConcurrency(TransactionConcurrency.OPTIMISTIC);

        cfg.setTransactionConfiguration(txCfg);

        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setSlowClientQueueLimit(1000);
        
        cfg.setCommunicationSpi(commSpi);
        
        
        
        FileSystemConfiguration fileSystemCfg = new FileSystemConfiguration();
        fileSystemCfg.setName(FILE_PREFIX + myCluster);
        fileSystemCfg.setDefaultMode(IgfsMode.DUAL_ASYNC);

        Map<String, IgfsMode> pathModes = new HashMap<>();
        pathModes.put("/tmp/.*", IgfsMode.PRIMARY);
        fileSystemCfg.setPathModes(pathModes);

        cfg.setFileSystemConfiguration(fileSystemCfg);
        
        Logger.info(this.getClass(), "Starting Ignite:" + cfg);
        
        
        fireUp = Ignition.start(cfg);
        return fireUp;
    }



    public synchronized void stop() {
        Ignition.stop(instanceName, true);
    }


    public Ignite ignite() {
        return ignite;
    }

}
