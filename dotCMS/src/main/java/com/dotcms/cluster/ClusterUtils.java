package com.dotcms.cluster;

import com.dotmarketing.util.Config;

public class ClusterUtils {

	public static boolean isESAutoWire(){
		return Config.getBooleanProperty("CLUSTER_AUTOWIRE", false)
			|| Config.getBooleanProperty("AUTOWIRE_CLUSTER_ES",true);
	}

	public static boolean isTransportAutoWire(){
		return Config.getBooleanProperty("CLUSTER_AUTOWIRE", false)
			|| Config.getBooleanProperty("AUTOWIRE_CLUSTER_TRANSPORT",true);
	}

}
