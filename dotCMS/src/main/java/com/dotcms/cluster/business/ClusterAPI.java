package com.dotcms.cluster.business;

public interface ClusterAPI {

    boolean isTransportAutoWire();

    boolean isESAutoWire();

    boolean isReplicasSetInConfig();

    boolean isAutoScaleConfigured();

    ReplicasMode getReplicasMode();
}
