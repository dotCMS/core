package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.content.elasticsearch.business.ClusterStats;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.telemetry.util.JsonUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class provides several utility methods aimed to check the status of the different subsystems
 * of dotCMS, namely:
 * <ul>
 *     <li>Database server connectivity.</li>
 *     <li>Elasticsearch server connectivity.</li>
 *     <li>Caching framework or server connectivity.</li>
 *     <li>File System access.</li>
 *     <li>Assets folder write/delete operations.</li>
 * </ul>
 *
 * @author Brent Griffin
 * @since Jul 18th, 2018
 */
class MonitorHelper {

    final boolean accessGranted ;
    final boolean useExtendedFormat;

    private static final String[] DEFAULT_IP_ACL_VALUE = new String[]{"127.0.0.1/32", "10.0.0.0/8", "172.16.0.0/12",
            "192.168.0.0/16"};
    private static final String IPV6_LOCALHOST = "0:0:0:0:0:0:0:1";
    private static final String SYSTEM_STATUS_API_IP_ACL = "SYSTEM_STATUS_API_IP_ACL";

    private static final long SYSTEM_STATUS_CACHE_RESPONSE_SECONDS = Config.getLongProperty(
            "SYSTEM_STATUS_CACHE_RESPONSE_SECONDS", 10);

    private static final String[] ACLS_IPS = Config.getStringArrayProperty(SYSTEM_STATUS_API_IP_ACL,
            DEFAULT_IP_ACL_VALUE);


    private final Lazy<String> telemetryEndPointUrl = Lazy.of(() -> Config.getStringProperty(
            "TELEMETRY_PERSISTENCE_ENDPOINT", null));


    static final AtomicReference<Tuple2<Long, MonitorStats>> cachedStats = new AtomicReference<>();

    MonitorHelper(final HttpServletRequest request, final boolean heavyCheck) {
        this.useExtendedFormat = heavyCheck;
        this.accessGranted = isAccessGranted(request);
    }

    /**
     * Determines if the IP address of the request is allowed to access this monitor service. We use
     * an ACL list to determine if the user/service accessing the monitor has permission to do so.
     * ACL IPs can be defined via the {@code SYSTEM_STATUS_API_IP_ACL} property.
     *
     * @param request The current instance of the {@link HttpServletRequest}.
     *
     * @return If the IP address of the request is allowed to access this monitor service, returns
     * {@code true}.
     */
    boolean isAccessGranted(final HttpServletRequest request){
        try {
            final String remoteAddr = request.getRemoteAddr();
            
            // Check if localhost using proper InetAddress API (handles all IPv4 and IPv6 localhost forms)
            if (isLocalhostAddress(remoteAddr) || ACLS_IPS == null || ACLS_IPS.length == 0){
                return true;
            }

            final String clientIP = HttpRequestDataUtil.getIpAddress(request).toString().split(StringPool.SLASH)[1];

            for (final String aclIP : ACLS_IPS) {
                if (IPUtils.isIpInCIDR(clientIP, aclIP)) {
                    return true;
                }
            }
        } catch (final Exception e) {
            Logger.warnEveryAndDebug(this.getClass(), e, 60000);
        }
        return false;
    }

    /**
     * Checks if the given address is a localhost address (IPv4 or IPv6).
     * Properly handles all valid IPv6 representations per RFC 5952, including
     * collapsed forms like "::1" and expanded forms like "0:0:0:0:0:0:0:1".
     * 
     * @param addr The IP address string from request.getRemoteAddr()
     * @return true if address is localhost (127.0.0.1, ::1, or any loopback address)
     */
    boolean isLocalhostAddress(final String addr) {
        if (!UtilMethods.isSet(addr)) {
            return false;
        }
        
        try {
            final InetAddress inetAddr = InetAddress.getByName(addr);
            return inetAddr.isLoopbackAddress();
        } catch (final UnknownHostException e) {
            Logger.debug(this, "Unable to parse address for localhost check: " + addr);
            return false;
        }
    }

    /**
     * Determines if dotCMS has started up by checking if the {@code dotcms.started.up} system
     * property has been set.
     *
     * @return If dotCMS has started up, returns {@code true}.
     */
    boolean isStartedUp() {
        return System.getProperty(WebKeys.DOTCMS_STARTED_UP)!=null;
    }

    /**
     * Retrieves the current status of the different subsystems of dotCMS. This method caches the
     * response for a period of time defined by the {@code SYSTEM_STATUS_CACHE_RESPONSE_SECONDS}
     * property.
     *
     * @return An instance of {@link MonitorStats} containing the status of the different
     * subsystems.
     */
    MonitorStats getMonitorStats(final HttpServletRequest request)  {
        if (cachedStats.get() != null && cachedStats.get()._1 > System.currentTimeMillis()) {
            return cachedStats.get()._2;
        }
        return getMonitorStatsNoCache(request);
    }

    /**
     * Retrieves the current status of the different subsystems of dotCMS. If cached monitor stats
     * are available, return them instead.
     *
     * @return An instance of {@link MonitorStats} containing the status of the different
     * subsystems.
     */
    synchronized MonitorStats getMonitorStatsNoCache(final HttpServletRequest request)  {
        // double check
        if (cachedStats.get() != null && cachedStats.get()._1 > System.currentTimeMillis()) {
            return cachedStats.get()._2;
        }
        final MonitorStats monitorStats = new MonitorStats
                .Builder()
                .cacheHealthy(isCacheHealthy())
                .assetFSHealthy(isAssetFileSystemHealthy())
                .localFSHealthy(isLocalFileSystemHealthy())
                .dBHealthy(isDBHealthy())
                .esHealthy(canConnectToES())
                .telemetry(Try.of(()->canConnectToTelemetry()).getOrElse(false))
                .contentAnalytics(isContentAnalytics(request))
                .build();
        // cache a healthy response
        if (monitorStats.isDotCMSHealthy()) {
            cachedStats.set( Tuple.of(System.currentTimeMillis() + (SYSTEM_STATUS_CACHE_RESPONSE_SECONDS * 1000),
                    monitorStats));
        }
        return monitorStats;
    }



    /**
     * Determines if the content analytics is healthy by sending a test event to the analytics
     * @param request
     * @return
     */
    private String isContentAnalytics(final HttpServletRequest request) {

        try {

            final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            final AnalyticsApp analyticsApp = Try.of(()-> AnalyticsHelper.get().appFromHost(host))
                    .getOrNull();

            if(analyticsApp==null) {
                return ExperimentsAPI.Health.NOT_CONFIGURED.name();
            }

            final Optional<CircuitBreakerUrl.Response<String>> responseOptional =
                    new EventLogRunnable(host).sendTestEvent();

            return responseOptional.isPresent()
                    && UtilMethods.isSet(responseOptional.get().getResponse())
                    ? ExperimentsAPI.Health.OK.name(): ExperimentsAPI.Health.CONFIGURATION_ERROR.name();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ExperimentsAPI.Health.CONFIGURATION_ERROR.name();
        }
    }

    /**
     * Determines if the database server is healthy by executing a simple query.
     *
     * @return If the database server is healthy, returns {@code true}.
     */
    boolean isDBHealthy()  {
            return Try.of(()->
                            new DotConnect().setSQL("SELECT 1 as count")
                    .loadInt("count"))
                    .onFailure(e->Logger.warnAndDebug(MonitorHelper.class, "unable to connect to db:" + e.getMessage(),e))
                    .getOrElse(0) > 0;
    }

    /**
     * Determines if dotCMS can connect to Elasticsearch by checking the ES Server cluster
     * statistics. If they're available, it means dotCMS can connect to ES.
     *
     * @return If dotCMS can connect to Elasticsearch, returns {@code true}.
     */
    boolean canConnectToES() {
        try {
            final ClusterStats stats = APILocator.getESIndexAPI().getClusterStats();
            return stats != null && stats.getClusterName() != null;
        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(),
                    "Unable to connect to ES: " + ExceptionUtil.getErrorMessage(e), e);
            return false;
        }
    }

    /**
     * Returns true if dotCMS can connect to telemetry server
     * @return
     */
    boolean canConnectToTelemetry() {

        if (!UtilMethods.isSet(telemetryEndPointUrl.get())) {
            return false;
        }
        return CircuitBreakerUrl.builder()
                .setUrl(telemetryEndPointUrl.get())
                .doPing()
                .build().ping();
    }


    /**
     * Determines if the cache is healthy by checking if the SYSTEM_HOST identifier is available.
     *
     * @return If the cache is healthy, returns {@code true}.
     */
    boolean isCacheHealthy()  {
        try {
            APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST);
            return UtilMethods.isSet(APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST).getId());
        } catch (final Exception e){
            Logger.warnAndDebug(this.getClass(), "unable to find SYSTEM_HOST: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Determines if the local file system is healthy by writing a file to the Dynamic Content Path
     * directory.
     *
     * @return If the local file system is healthy, returns {@code true}.
     */
    boolean isLocalFileSystemHealthy()  {
        return new FileSystemTest(ConfigUtils.getDynamicContentPath()).call();
    }

    /**
     * Determines if the asset file system is healthy by writing a file to the Asset Path
     * directory.
     *
     * @return If the asset file system is healthy, returns {@code true}.
     */
    boolean isAssetFileSystemHealthy() {
        return new FileSystemTest(ConfigUtils.getAssetPath()).call();
    }

    /**
     * This class is used to test the health of the file system by writing a file to a given path.
     */
    static final class FileSystemTest implements Callable<Boolean> {

        final String initialPath;

        public FileSystemTest(String initialPath) {
            this.initialPath = initialPath.endsWith(File.separator) ? initialPath : initialPath + File.separator;
        }

        @Override
        public Boolean call() {
            final String uuid = UUIDUtil.uuid();
            final String realPath = initialPath
                    + "monitor"
                    + File.separator
                    + uuid;
            final File file = new File(realPath);
            try {
                if (file.mkdirs() && file.delete() && file.createNewFile()) {
                    try (OutputStream os = Files.newOutputStream(file.toPath())) {
                        os.write(uuid.getBytes());
                    }
                    return file.delete();
                }
            } catch (Exception e) {
                Logger.warnAndDebug(this.getClass(), "Unable to write a file to: " + initialPath + " : " +e.getMessage(), e);
                return false;
            }
            return false;
        }
    }

}
