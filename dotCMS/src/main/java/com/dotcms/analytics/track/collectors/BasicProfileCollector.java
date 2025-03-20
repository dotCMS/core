package com.dotcms.analytics.track.collectors;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.telemetry.business.MetricsAPI;
import com.dotcms.util.FunctionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

/**
 * Collects the basic profile information for a collector payload bean. It's worth noting that
 * <b>ALL</b>ALL data collectors will include the information added by this one.
 *
 * @author jsanca
 * @since Sep 17th, 2024
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

        this.setCustomerTelemetryData(collectorPayloadBean);

        if (UtilMethods.isSet(collectorContextMap.get(CollectorContextMap.REFERER))) {
            collectorPayloadBean.put(REFERER, collectorContextMap.get(CollectorContextMap.REFERER).toString());
        }

        if (UtilMethods.isSet(collectorContextMap.get(CollectorContextMap.USER_AGENT))) {
            collectorPayloadBean.put(USER_AGENT, collectorContextMap.get(CollectorContextMap.USER_AGENT).toString());
        }

        final HttpServletRequest request = (HttpServletRequest)collectorContextMap.get("request");

        collectorPayloadBean.put(PERSONA,
                WebAPILocator.getPersonalizationWebAPI().getContainerPersonalization(request));

        // Include default value for other boolean fields in the Clickhouse table
        collectorPayloadBean.put(IS_EXPERIMENT_PAGE, false);
        collectorPayloadBean.put(IS_TARGET_PAGE, false);

        if (Objects.isNull(collectorPayloadBean.get(EVENT_SOURCE))) {
            // this is the default event source
            collectorPayloadBean.put(EVENT_SOURCE, EventSource.DOT_CMS.getName());
        }

        setUserInfo(request, collectorPayloadBean);

        return collectorPayloadBean;
    }

    /**
     * Sets the customer Telemetry data as part of the information that will be persisted to the
     * Content Analytics database.
     *
     * @param collectorPayloadBean The {@link CollectorPayloadBean} that will be persisted to the
     *                             Content Analytics database.
     */
    private void setCustomerTelemetryData(final CollectorPayloadBean collectorPayloadBean) {
        final MetricsAPI metricsAPI = APILocator.getMetricsAPI();
        try {
            final MetricsAPI.Client client = metricsAPI.getClient();
            collectorPayloadBean.put(CUSTOMER_NAME, client.getClientName());
            collectorPayloadBean.put(CUSTOMER_CATEGORY, client.getCategory());
            collectorPayloadBean.put(ENVIRONMENT_NAME, client.getEnvironment());
            collectorPayloadBean.put(ENVIRONMENT_VERSION, client.getVersion());
        } catch (final DotDataException e) {
            Logger.warnAndDebug(BasicProfileCollector.class, String.format("Failed to retrieve customer Telemetry data: " +
                    "%s", ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    private void setUserInfo(final HttpServletRequest request, final CollectorPayloadBean collectorPayloadBean) {

        final User user = WebAPILocator.getUserWebAPI().getUser(request);
        if (Objects.nonNull(user)) {

            final HashMap<String, String> userObject = new HashMap<>();
            userObject.put(ID, user.getUserId());
            userObject.put(EMAIL, user.getEmailAddress());
            collectorPayloadBean.put(USER_OBJECT, userObject);
        }
    }

    @Override
    public boolean isEventCreator(){
        return false;
    }

}
