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
import org.apache.ignite.transactions.TransactionConcurrency;

public class IgniteClient {
    private final Ignite ignite;
    private final static String IGNITE_CONFIG_FILE_NAME = "/ignite-dotcms.xml";
    private final static String INSTANCE_PREFIX = "dotCMS_";
    private final static String FILE_PREFIX = "dotCMS_FS_";
    private static IgniteClient instance;
    private final String myCluster;


    private IgniteClient() {
        myCluster = ClusterFactory.getClusterId().substring(0, 8) + "_" + System.currentTimeMillis();
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

        String igniteInstanceName = INSTANCE_PREFIX + myCluster;
        /***
         * 
         * THERE IS A BUG IN IGNITE 2.3 https://issues.apache.org/jira/browse/IGNITE-6944 to run
         * ignite 2.3.0, you need to use the jdk marshaller JdkMarshaller marshaller = new
         * JdkMarshaller(); cfg.setMarshaller(marshaller);
         */

        IgniteConfiguration cfg = new IgniteConfiguration();
        
        TransactionConfiguration txCfg= new TransactionConfiguration();
        txCfg.setDefaultTxConcurrency(TransactionConcurrency.OPTIMISTIC);

        cfg.setTransactionConfiguration(txCfg);
        cfg.setIgniteInstanceName(igniteInstanceName);
        
        cfg.setClientMode(Config.getBooleanProperty("cache.ignite.clientMode", true));


        FileSystemConfiguration fileSystemCfg = new FileSystemConfiguration();
        fileSystemCfg.setName(FILE_PREFIX + myCluster);
        fileSystemCfg.setDefaultMode(IgfsMode.DUAL_ASYNC);

        Map<String, IgfsMode> pathModes = new HashMap<>();
        pathModes.put("/tmp/.*", IgfsMode.PRIMARY);
        fileSystemCfg.setPathModes(pathModes);

        cfg.setFileSystemConfiguration(fileSystemCfg);
        fireUp = Ignition.getOrStart(cfg);
        return fireUp;
    }



    public static IgniteClient getInstance() {
        if (null == instance) {
            synchronized (IgniteClient.class) {
                if (null == instance) {
                    instance = new IgniteClient();
                }
            }
        }
        return instance;
    }

    /**
     * restarts ignite
     */
    public void reIgnite() {
        String name = instance.myCluster;
        instance = null;
        Ignition.stop(name, false);
    }

    public Ignite ignite() {
        return ignite;
    }

}
