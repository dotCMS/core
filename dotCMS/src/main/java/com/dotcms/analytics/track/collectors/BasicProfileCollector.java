package com.dotcms.analytics.track.collectors;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.FunctionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Collects the basic profile information for a collector payload bean.
 * @author jsanca
 */
public class BasicProfileCollector implements Collector {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    @Override
    public boolean test(CollectorContextMap collectorContextMap) {
        // Every collector needs a basic profile
        return true;
    }

    @Override
    public CollectorPayloadBean collect(final CollectorContextMap collectorContextMap,
                                        final CollectorPayloadBean collectorPayloadBean) {

        final String requestId = (String)collectorContextMap.get(CollectorContextMap.REQUEST_ID);
        final Long time = (Long)collectorContextMap.get(CollectorContextMap.TIME);
        final String clusterId   = (String)collectorContextMap.get(CollectorContextMap.CLUSTER);
        final String serverId   = (String)collectorContextMap.get(CollectorContextMap.SERVER);
        final String sessionId   = (String)collectorContextMap.get(CollectorContextMap.SESSION);
        final Boolean sessionNew   = (Boolean)collectorContextMap.get(CollectorContextMap.SESSION_NEW);

        final Long timestamp = FunctionUtils.getOrDefault(Objects.nonNull(time), () -> time, System::currentTimeMillis);
        final Instant instant = Instant.ofEpochMilli(timestamp);
        final ZonedDateTime zonedDateTimeUTC = instant.atZone(ZoneId.of("UTC"));

        collectorPayloadBean.put(REQUEST_ID, requestId);
        collectorPayloadBean.put(UTC_TIME, FORMATTER.format(zonedDateTimeUTC));
        collectorPayloadBean.put(CLUSTER,
                FunctionUtils.getOrDefault(Objects.nonNull(clusterId), ()->clusterId, ClusterFactory::getClusterId));
        collectorPayloadBean.put(SERVER,
                FunctionUtils.getOrDefault(Objects.nonNull(serverId), ()->serverId,()->APILocator.getServerAPI().readServerId()));
        collectorPayloadBean.put(SESSION_ID, sessionId);
        collectorPayloadBean.put(SESSION_NEW, sessionNew);

        if (UtilMethods.isSet(collectorContextMap.get(CollectorContextMap.REFERER))) {
            collectorPayloadBean.put(REFERER, collectorContextMap.get(CollectorContextMap.REFERER).toString());
        }

        if (UtilMethods.isSet(collectorContextMap.get(CollectorContextMap.USER_AGENT))) {
            collectorPayloadBean.put(USER_AGENT, collectorContextMap.get(CollectorContextMap.USER_AGENT).toString());
        }

        final HttpServletRequest request = (HttpServletRequest)collectorContextMap.get("request");

        collectorPayloadBean.put(PERSONA,
                WebAPILocator.getPersonalizationWebAPI().getContainerPersonalization(request));

        collectorPayloadBean.put(RENDER_MODE, PageMode.get(request).toString().replace("_MODE", StringPool.BLANK));

        // Include default value for other boolean fields in the Clickhouse table
        collectorPayloadBean.put(COME_FROM_VANITY_URL, false);
        collectorPayloadBean.put(ISEXPERIMENTPAGE, false);
        collectorPayloadBean.put(ISTARGETPAGE, false);
        return collectorPayloadBean;
    }

    @Override
    public boolean isEventCreator(){
        return false;
    }

}
