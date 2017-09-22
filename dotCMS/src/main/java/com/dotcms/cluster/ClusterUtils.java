package com.dotcms.cluster;

import com.dotmarketing.util.Config;

public class ClusterUtils {

	public static boolean isTransportAutoWire(){
		return Config.getBooleanProperty("AUTOWIRE_CLUSTER_TRANSPORT", true);
	}

	public static boolean isESAutoWire(){
		return Config.getBooleanProperty("AUTOWIRE_CLUSTER_ES", true);
	}

	public static boolean isESAutoWireReplicas(){
		return isESAutoWire()
			&& Config.getBooleanProperty("AUTOWIRE_MANAGE_ES_REPLICAS", true);
	}
	
	
    public static boolean isAutoScaleConfigured() {
        return isTransportAutoWire() || isESAutoWire();
    }
}
