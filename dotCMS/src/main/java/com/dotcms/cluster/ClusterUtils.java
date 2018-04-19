package com.dotcms.cluster;

import com.dotcms.cluster.business.ReplicasMode;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;

/**
 * Deprecated as of 5.0.0. Please use {@link com.dotcms.cluster.business.ClusterAPI}
 */

@Deprecated
public class ClusterUtils {

	public static boolean isTransportAutoWire(){
		return Config.getBooleanProperty("AUTOWIRE_CLUSTER_TRANSPORT", true) && LicenseUtil.getLevel()> 200;
	}

	public static boolean isESAutoWire(){
		return Config.getBooleanProperty("AUTOWIRE_CLUSTER_ES", true)&& LicenseUtil.getLevel()> 200;
	}

	public static boolean isReplicasSet(){
		return UtilMethods.isSet(Config.getStringProperty("ES_INDEX_REPLICAS", null)) && LicenseUtil.getLevel()> 200;
	}
	
	
    public static boolean isAutoScaleConfigured() {
        return isTransportAutoWire() || isESAutoWire();
    }
}
