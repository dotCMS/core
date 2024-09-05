package com.dotcms.analytics.track.collectors;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.FunctionUtils;
import com.dotmarketing.business.APILocator;

import java.util.Objects;

public class BasicProfileCollector implements Collector {

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {

        return true; // every one needs a basic profile
    }

    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        final String requestId = (String)collectorContextMap.get("requestId");
        final Long time = (Long)collectorContextMap.get("time");
        final String clusterId   = (String)collectorContextMap.get("cluster");
        final String serverId   = (String)collectorContextMap.get("server");
        final Boolean sessionId   = (Boolean)collectorContextMap.get("session");
        final Boolean sessionNew   = (Boolean)collectorContextMap.get("sessionNew");

        collectorPayloadBean.put("request_id", requestId);
        collectorPayloadBean.put("timestamp",
                FunctionUtils.getOrDefault(Objects.nonNull(time), ()->time,System::currentTimeMillis));
        collectorPayloadBean.put("cluster",
                FunctionUtils.getOrDefault(Objects.nonNull(clusterId), ()->clusterId,()->ClusterFactory.getClusterId()));
        collectorPayloadBean.put("server",
                FunctionUtils.getOrDefault(Objects.nonNull(serverId), ()->serverId,()->APILocator.getServerAPI().readServerId()));
        collectorPayloadBean.put("sessionId", sessionId);
        collectorPayloadBean.put("sessionNew", sessionNew);
        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
