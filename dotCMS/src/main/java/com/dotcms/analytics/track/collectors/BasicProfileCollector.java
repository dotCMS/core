package com.dotcms.analytics.track.collectors;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.FunctionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class BasicProfileCollector implements Collector {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
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
        final String sessionId   = (String)collectorContextMap.get("session");
        final Boolean sessionNew   = (Boolean)collectorContextMap.get("sessionNew");

        final Long timestamp = FunctionUtils.getOrDefault(Objects.nonNull(time), () -> time, System::currentTimeMillis);
        final Instant instant = Instant.ofEpochMilli(timestamp);
        final ZonedDateTime zonedDateTimeUTC = instant.atZone(ZoneId.of("UTC"));

        collectorPayloadBean.put("request_id", requestId);
        collectorPayloadBean.put("utc_time", FORMATTER.format(zonedDateTimeUTC));
        collectorPayloadBean.put("cluster",
                FunctionUtils.getOrDefault(Objects.nonNull(clusterId), ()->clusterId, ClusterFactory::getClusterId));
        collectorPayloadBean.put("server",
                FunctionUtils.getOrDefault(Objects.nonNull(serverId), ()->serverId,()->APILocator.getServerAPI().readServerId()));
        collectorPayloadBean.put("sessionId", sessionId);
        collectorPayloadBean.put("sessionNew", sessionNew);

        if (UtilMethods.isSet(collectorContextMap.get("referer"))) {
            collectorPayloadBean.put("referer", collectorContextMap.get("referer").toString());
        }

        if (UtilMethods.isSet(collectorContextMap.get("user-agent"))) {
            collectorPayloadBean.put("userAgent", collectorContextMap.get("user-agent").toString());
        }

        final HttpServletRequest request = (HttpServletRequest)collectorContextMap.get("request");

        collectorPayloadBean.put("persona",
                WebAPILocator.getPersonalizationWebAPI().getContainerPersonalization(request));

        collectorPayloadBean.put("renderMode", PageMode.get(request).toString().replace("_MODE", ""));
        return collectorPayloadBean;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isEventCreator(){
        return false;
    }
}
