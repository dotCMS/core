package com.dotcms.cluster;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexAPI.ReplicasMode;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;

public class ClusterUtils {

	public static boolean isTransportAutoWire(){
		return Config.getBooleanProperty("AUTOWIRE_CLUSTER_TRANSPORT", true) && LicenseUtil.getLevel()> 200;
	}

	public static boolean isESAutoWire(){
		return Config.getBooleanProperty("AUTOWIRE_CLUSTER_ES", true)&& LicenseUtil.getLevel()> 200;
	}

	public static boolean isESAutoWireReplicas(){
		final String replicasConf = Config.getStringProperty("ES_INDEX_REPLICAS", null);
		return isESAutoWire()
			&& UtilMethods.isSet(replicasConf) &&
				replicasConf.equals(ReplicasMode.AUTOWIRE.getReplicasMode()) &&
				LicenseUtil.getLevel()> 200;
	}

	public static boolean isReplicasSet(){
		return UtilMethods.isSet(Config.getStringProperty("ES_INDEX_REPLICAS", null)) && LicenseUtil.getLevel()> 200;
	}
	
	
    public static boolean isAutoScaleConfigured() {
        return isTransportAutoWire() || isESAutoWire();
    }
}
