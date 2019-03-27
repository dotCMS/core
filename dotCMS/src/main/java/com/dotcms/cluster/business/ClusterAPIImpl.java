package com.dotcms.cluster.business;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import static com.dotcms.cluster.business.ReplicasMode.AUTOWIRE;
import static com.dotcms.cluster.business.ReplicasMode.STATIC;

public class ClusterAPIImpl implements ClusterAPI {

    private final ServerAPI serverAPI;

    public ClusterAPIImpl() {
        this(APILocator.getServerAPI());
    }

    public ClusterAPIImpl(final ServerAPI serverAPI) {
        this.serverAPI = serverAPI;
    }

    @Override
    public boolean isTransportAutoWire() {
        return Config.getBooleanProperty("AUTOWIRE_CLUSTER_TRANSPORT", true)
            && LicenseUtil.getLevel()> LicenseLevel.STANDARD.level;
    }

    @Override
    public boolean isESAutoWire() {
        return Config.getBooleanProperty("AUTOWIRE_CLUSTER_ES", true)
            && LicenseUtil.getLevel()> LicenseLevel.STANDARD.level;
    }

    @Override
    public boolean isReplicasSetInConfig() {
        return UtilMethods.isSet(Config.getStringProperty("ES_INDEX_REPLICAS", null))
            && LicenseUtil.getLevel()> LicenseLevel.STANDARD.level;
    }

    @Override
    public boolean isAutoScaleConfigured() {
        return isTransportAutoWire() || isESAutoWire();
    }

    @Override
    public ReplicasMode getReplicasMode(){

        final ReplicaModeFactory replicaModeFactory = new ReplicaModeFactory();
        ReplicasMode replicasMode;

        if (isReplicasSetInConfig()) {
            replicasMode = replicaModeFactory.fromConfig();
        } else {
            if (isESAutoWire()){
                replicasMode = AUTOWIRE;
            }else{
                replicasMode = STATIC;
            }
        }

        int numberOfReplicas = replicasMode.getNumberOfReplicas();

        if (LicenseUtil.getLevel() <= LicenseLevel.STANDARD.level){
            numberOfReplicas = 0;
        }

        if (replicasMode.equals(AUTOWIRE)){
            if (isESAutoWire()){
                numberOfReplicas = getReplicasCountByNodes();
            } else{
                Logger.error(ESIndexAPI.class,
                    "Setting ES_INDEX_REPLICAS=\"autowire\" and AUTOWIRE_CLUSTER_ES=false is not allowed; number_of_replicas=0 will be used by default");
            }
        }

        replicasMode.setNumberOfReplicas(numberOfReplicas);

        return replicasMode;

    }

    private int getReplicasCountByNodes(){
        int serverCount;

        try {
            serverCount = serverAPI.getAliveServersIds().length;
        } catch (DotDataException e) {
            Logger.error(ESIndexAPI.class, "Error getting live server list for server count, using 1 as default.");
            serverCount = 1;
        }
        // formula is (live server count (including the ones that are down but not yet timed out) - 1)

        if(serverCount>0) {
            serverCount = serverCount - 1;
        }

        return serverCount;

    }
}
