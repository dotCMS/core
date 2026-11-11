package com.dotcms.rest.api.v1.cluster;

import com.dotcms.content.index.domain.SearchClusterHealth;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Turns a server's cluster status report into the typed sections of
 * {@code GET /api/v1/cluster/nodes}, and computes each server's status.
 *
 * <p>Reports arrive through a JSON round trip on the pub/sub channel, and older servers send older
 * formats, so every value is converted leniently: anything that can't be read as the expected type
 * becomes {@code null}, and nothing here throws. Empty strings (the "unknown" placeholder of the
 * report) and the {@code -1} the cache transport uses for "no value" also become {@code null}.</p>
 *
 * @author hassandotcms
 */
final class ClusterNodeMapper {

    private ClusterNodeMapper() {
    }

    /**
     * Computes a server's status. A heartbeat is fresh when it is at most {@code threshold}
     * seconds old: answering with a fresh heartbeat is {@link NodeStatus#UP}, having only one of
     * the two is {@link NodeStatus#LAGGING}, and having neither is {@link NodeStatus#DOWN}.
     *
     * <p>A server that answered but failed its assets check is {@link NodeStatus#DOWN} whatever its
     * heartbeat: the legacy Network tab shows it red, and a server that can't use the shared
     * assets folder is not healthy. Other report values never affect the status.</p>
     *
     * @param responded          whether the server answered this call's status request
     * @param assetsFailed       whether the server answered with a failed assets check
     * @param lastContactSeconds seconds since its last heartbeat
     * @param threshold          the age, in seconds, after which a heartbeat is stale
     * @return the server's status
     */
    static NodeStatus status(final boolean responded, final boolean assetsFailed, final long lastContactSeconds,
            final long threshold) {
        if (responded && assetsFailed) {
            return NodeStatus.DOWN;
        }
        final boolean fresh = lastContactSeconds <= threshold;
        if (responded && fresh) {
            return NodeStatus.UP;
        }
        return responded || fresh ? NodeStatus.LAGGING : NodeStatus.DOWN;
    }

    /**
     * Tells whether a report says the server's assets check failed: its {@code assetsStatus} is
     * {@code red}, the value the legacy Network tab's status dot is built from (the folder can't
     * be read, written, or have a test file created in it). Every version of the report sends it.
     *
     * @param report the server's status report
     * @return {@code true} only when the report says the check failed
     */
    static boolean assetsFailed(final Map<String, ? extends Serializable> report) {
        return "red".equalsIgnoreCase(toText(report.get("assetsStatus")));
    }

    /**
     * Returns the heartbeat age after which a server counts as stale:
     * {@code CLUSTER_NODE_STALE_HEARTBEAT_SECONDS}, defaulting to three heartbeat intervals
     * ({@code SERVER_HEARTBEAT_RUN_EVERY_SECONDS}, default 60), that is 180 seconds.
     *
     * @return the threshold in seconds
     */
    static long staleThresholdSeconds() {
        return Config.getIntProperty("CLUSTER_NODE_STALE_HEARTBEAT_SECONDS",
                3 * Config.getIntProperty("SERVER_HEARTBEAT_RUN_EVERY_SECONDS", 60));
    }

    /**
     * Maps the cache part of a report. The transport address is always {@code null}: the report's
     * {@code ipAddress} holds the server's own IP, and neither existing transport has an address.
     *
     * @param report the server's status report
     * @return the cache section
     */
    static CacheSectionView cache(final Map<String, ? extends Serializable> report) {
        return new CacheSectionView(
                toText(report.get("clusterName")),
                toText(report.get("cacheTransportClass")),
                toInt(report.get("numberOfNodes")),
                toBoolean(report.get("open")),
                null,
                toInt(report.get("port")),
                toLong(report.get("receivedBytes")),
                toLong(report.get("sentBytes")));
    }

    /**
     * Maps the search part of a report. The queue wait is read only from the numeric
     * {@code esTaskMaxWaitingTimeMillis}; older servers don't send it, and their {@code "0s"}-style
     * string is not parsed.
     *
     * @param report the server's status report
     * @return the search section; every value is {@code null} when the server's search check failed
     */
    static SearchSectionView search(final Map<String, ? extends Serializable> report) {
        final Double percent = toDouble(report.get("esActiveShardsPercentAsNumber"));
        return new SearchSectionView(
                toEngine(report.get("esEngine")),
                toText(report.get("esClusterName")),
                toBoolean(report.get("esTimedout")),
                toInt(report.get("esNumberOfNodes")),
                toInt(report.get("esNumberOfDataNodes")),
                toInt(report.get("esActivePrimaryShards")),
                toInt(report.get("esActiveShards")),
                toInt(report.get("esRelocatingShards")),
                toInt(report.get("esInitializingShards")),
                toInt(report.get("esUnassignedShards")),
                toInt(report.get("esDelayedUnasignedShards")),
                toInt(report.get("esNumberOfPendingTasks")),
                toInt(report.get("esNumberOfInFlightFetch")),
                toLong(report.get("esTaskMaxWaitingTimeMillis")),
                percent);
    }

    /**
     * Maps the assets part of a report.
     *
     * @param report the server's status report
     * @return the assets section
     */
    static AssetsSectionView assets(final Map<String, ? extends Serializable> report) {
        return new AssetsSectionView(
                toText(report.get("assetsPath")),
                toBoolean(report.get("assetsCanRead")),
                toBoolean(report.get("assetsCanWrite")));
    }

    /** Returns the value as trimmed text; blank or {@code "n/a"} becomes {@code null}. */
    private static String toText(final Object value) {
        if (value == null) {
            return null;
        }
        final String text = value.toString().trim();
        return UtilMethods.isSet(text) && !"n/a".equalsIgnoreCase(text) ? text : null;
    }

    /** Returns the value as a long, from a number or numeric text; {@code -1} and anything else become {@code null}. */
    private static Long toLong(final Object value) {
        final Long number;
        if (value instanceof Number) {
            number = ((Number) value).longValue();
        } else {
            final String text = toText(value);
            number = text != null && text.matches("-?\\d+") ? Long.valueOf(text) : null;
        }
        return number != null && number != -1 ? number : null;
    }

    /** Returns the value as an int, following {@link #toLong(Object)}; out-of-range values become {@code null}. */
    private static Integer toInt(final Object value) {
        final Long number = toLong(value);
        return number != null && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE
                ? number.intValue() : null;
    }

    /** Returns the value as a double, from a number or numeric text; anything else becomes {@code null}. */
    private static Double toDouble(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        final String text = toText(value);
        return text != null ? Try.of(() -> Double.valueOf(text)).getOrNull() : null;
    }

    /**
     * Accepts booleans, and boolean text such as "true"/"false" (or "yes"/"no", "on"/"off"), with
     * the trailing "/" the report has always sent; anything else becomes {@code null}.
     */
    private static Boolean toBoolean(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return BooleanUtils.toBooleanObject(StringUtils.removeEnd(toText(value), "/"));
    }

    /** Returns the engine the value names, ignoring case; anything else, or no value, becomes {@code null}. */
    private static SearchClusterHealth.Engine toEngine(final Object value) {
        return EnumUtils.getEnumIgnoreCase(SearchClusterHealth.Engine.class, toText(value));
    }
}
