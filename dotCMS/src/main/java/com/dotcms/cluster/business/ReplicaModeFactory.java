package com.dotcms.cluster.business;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import static com.dotcms.cluster.business.ReplicasMode.AUTOWIRE;
import static com.dotcms.cluster.business.ReplicasMode.BOUNDED;
import static com.dotcms.cluster.business.ReplicasMode.STATIC;

public class ReplicaModeFactory {

    public ReplicasMode fromConfig() {
        ReplicasMode replicasMode = null;

        final String replicasValueFromConfig = Config.getStringProperty("ES_INDEX_REPLICAS", null);

        if(replicasValueFromConfig.equalsIgnoreCase(AUTOWIRE.name())) {
            replicasMode = AUTOWIRE;
        } else if(replicasValueFromConfig.contains("-")) {
            replicasMode = BOUNDED;
            replicasMode.setAutoExpandReplicas(replicasValueFromConfig);
        } else {
            try {
                replicasMode = STATIC;
                replicasMode.setNumberOfReplicas(Integer.parseInt(replicasValueFromConfig));
            } catch(NumberFormatException e) {
                Logger.error(this, "Unable to determine the Replica mode from config value ES_INDEX_REPLICAS");
            }
        }

        return replicasMode;

    }
}
