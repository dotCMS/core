package com.dotcms.rest.api.v1.maintenance;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import com.dotcms.cluster.bean.Server;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.config.DotInitializer;
import com.dotcms.dotpubsub.DotPubSubEvent;
import com.dotcms.dotpubsub.DotPubSubProvider;
import com.dotcms.dotpubsub.DotPubSubProviderLocator;
import com.dotcms.dotpubsub.DotPubSubTopic;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;

/**
 * This topic is intended for cluster aware actions such as restarting the servers or killing
 * session across all nodes the cluster. Only the shutdown action has been implemented
 * 
 * @author jsanca
 */
public class ClusterManagementTopic implements DotPubSubTopic,DotInitializer {


    private static final long serialVersionUID = 1L;


    public enum CLUSTER_ACTIONS {
        CLUSTER_RESTART, KILL_SESSIONS,KILL_SESSION, NONE;
    }
    
    static final String ROLLING_DELAY = "delay";
    static final String SERVER_ORDER = "order";
    static final String CLUSTER_ACTION = "action";
    static final String TOPIC = "Cluster_Actions";
    
    private static class SingletonHelper {
        private static final ClusterManagementTopic INSTANCE = new ClusterManagementTopic();
    }

    public static ClusterManagementTopic getInstance() {
        return SingletonHelper.INSTANCE;
    }

    @VisibleForTesting
    private final String serverId;

    private final transient DotPubSubProvider provider;

    private ClusterManagementTopic() {
        this(APILocator.getServerAPI().readServerId());

    }

    @VisibleForTesting
    public ClusterManagementTopic(final String serverId) {
        this(serverId, DotPubSubProviderLocator.provider.get());
    }

    @VisibleForTesting
    public ClusterManagementTopic(final String serverId, final DotPubSubProvider provider) {
        this.serverId = serverId;
        this.provider = provider;
        provider.subscribe(this);
    }


    public boolean restartCluster(int rollingDelay) {

        DotPubSubEvent event = new DotPubSubEvent.Builder()
                        .addPayload(SERVER_ORDER, myServerOrder())
                        .addPayload(ROLLING_DELAY, rollingDelay)
                        .addPayload(CLUSTER_ACTION, CLUSTER_ACTIONS.CLUSTER_RESTART.name())
                        .withTopic(TOPIC)
                        .build();

        this.provider.publish(event);

        return true;
    }

    @Override
    public String getInstanceId() {
        return serverId;
    }

    final class Restarter implements Runnable {
        final int delaySeconds;

        Restarter(int delaySeconds) {
            this.delaySeconds = delaySeconds;
        }

        @Override
        public void run() {

            Logger.info(ClusterManagementTopic.class, "Running restart in: " + delaySeconds + " seconds");
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (Exception e) { // NOSONAR
                // do nothing
            }finally {
                System.exit(Config.getIntProperty("SYSTEM_EXIT_CODE", 0));
            }
        }
    }

    private static CLUSTER_ACTIONS resolveAction(String name) {
        for(CLUSTER_ACTIONS action : CLUSTER_ACTIONS.values()) {
            if(action.name().equalsIgnoreCase(name)) {
                return action;
            }
        }
        return CLUSTER_ACTIONS.NONE;
    }
    
    

    @Override
    public void notify(final DotPubSubEvent event) {
        Logger.info(this.getClass(), () -> "Got DOTCMS_CLUSTER_RESTART from server:" + event.getOrigin() + ".");


        CLUSTER_ACTIONS action = resolveAction((String)event.getPayload().get(CLUSTER_ACTION));
        
        switch (action) {
            case CLUSTER_RESTART:
                fireRestart(event);
                break;
            case KILL_SESSIONS:
                Logger.info(getClass(), "Killing Sessions not impl");
                break;
            default:
                break;
        }
        
        

    }


    private void fireRestart(final DotPubSubEvent event) {
        HashMap<String, Integer> serverOrderMap =
                        (HashMap<String, Integer>) event.getPayload().getOrDefault(SERVER_ORDER, new HashMap<>());

        int serverOrder = serverOrderMap.getOrDefault(serverId, 0);

        int rollingDelay = (Integer) event.getPayload()
                        .getOrDefault(ROLLING_DELAY,  Config.getIntProperty("ROLLING_RESTART_DELAY_SECONDS", 60));

        Instant shutdown = Instant.now().plus(serverOrder *   rollingDelay, ChronoUnit.SECONDS);
        
        
        Logger.info(this.getClass(), "Restarting  " + shutdown + "s, serverOrder:" + serverOrder
                        + ", rollingDelay:" + rollingDelay);
        Restarter restarter = new Restarter(serverOrder * rollingDelay);



        System.setProperty("DOTCMS_CLUSTER_RESTART", shutdown.toString());
        
        DotConcurrentFactory.getInstance().getSingleSubmitter(TOPIC).submit(restarter);

        
        
    }
    
    
    /**
     * This is a hashmap of the servers in the cluster and their current order
     * it is sent to the cluster as a single view so each server can work out 
     * when to restart itself
     * @return
     */
    private HashMap<String, Serializable> myServerOrder() {
        int serverOrder = 0;
        HashMap<String, Serializable> serverMap = new HashMap<>();
        serverMap.put(serverId, serverOrder++);
 
        List<Server> servers = Try.of(()-> APILocator.getServerAPI().getAllServers()).getOrElseThrow(DotRuntimeException::new);
        for (Server server : servers) {
            serverMap.putIfAbsent(server.getServerId(), serverOrder++);
        }

        return serverMap;

    }


    @Override
    public Comparable getKey() {

        return TOPIC;
    }

    @Override
    public void init() {
       Logger.info(getClass(), "Intializing " + TOPIC);
        
    }


}
