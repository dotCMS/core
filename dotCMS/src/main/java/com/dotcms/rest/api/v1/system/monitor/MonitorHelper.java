package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.health.api.HealthService;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
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

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper.DOT_ANALYTICS_BASE_URL;
import static com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper.DOT_ANALYTICS_PROJECT;
import static com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper.DOT_ANALYTICS_TENANT;
import static com.liferay.util.StringPool.BLANK;

/**
 * Provides utility methods for checking the status of the different subsystems of dotCMS, namely:
 * <ul>
 *     <li>Database server connectivity.</li>
 *     <li>Elasticsearch server connectivity.</li>
 *     <li>Caching framework or server connectivity.</li>
 *     <li>Local and asset file system write access.</li>
 *     <li>Telemetry service connectivity.</li>
 *     <li>Content Analytics app configuration and CA Event Manager reachability.</li>
 * </ul>
 *
 * <p>Results are cached for a period defined by {@code SYSTEM_STATUS_CACHE_RESPONSE_SECONDS}
 * (default: 10 s). Only a fully healthy response is cached; a degraded response is always
 * recomputed on the next request.
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
     * Determines whether the Content Analytics subsystem is healthy by running three sequential
     * checks:
     * <ol>
     *   <li><b>App configuration</b> — the {@code dotContentAnalytics-config} App must be
     *       installed and have secrets configured for the current Site (including an HMAC
     *       token provisioned via the save-flow exchange).</li>
     *   <li><b>Required environment variables</b> — {@code DOT_ANALYTICS_BASE_URL},
     *       {@code DOT_ANALYTICS_TENANT}, and {@code DOT_ANALYTICS_PROJECT} must all be
     *       present and non-empty.</li>
     *   <li><b>Service connectivity</b> — the CA Event Manager's {@code /v1/health} endpoint
     *       must respond with a 2xx status, confirming ClickHouse is reachable.</li>
     * </ol>
     *
     * @param request the current HTTP request, used to resolve the active Site
     *
     * @return a {@link Health} name: {@code OK}, {@code NOT_CONFIGURED}, or
     * {@code CONFIGURATION_ERROR}
     */
    private String isContentAnalytics(final HttpServletRequest request) {
        try {
            // Required global infrastructure config — these gate the whole subsystem
            // independent of which site the probe lands on.
            final String baseUrl = Config.getStringProperty(DOT_ANALYTICS_BASE_URL, BLANK);
            final String tenant  = Config.getStringProperty(DOT_ANALYTICS_TENANT, BLANK);
            final String project = Config.getStringProperty(DOT_ANALYTICS_PROJECT, BLANK);
            final boolean globallyConfigured = UtilMethods.isSet(baseUrl)
                    && UtilMethods.isSet(tenant)
                    && UtilMethods.isSet(project);

            // Per-site config (App secrets) — automated probes typically hit the System
            // Host or arrive via IP, in which case getCurrentHostNoThrow may return null.
            // Avoid passing null into getAppSecrets — its catch-block logger dereferences
            // the site identifier and would NPE for every probe, polluting logs.
            final Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            final boolean siteConfigured = site != null
                    && !ContentAnalyticsUtil.getAppSecrets(site).isEmpty();

            if (!globallyConfigured && !siteConfigured) {
                return Health.NOT_CONFIGURED.name();
            }
            if (!globallyConfigured) {
                Logger.warn(this, "Content Analytics health check: missing required env vars " +
                        "(DOT_ANALYTICS_BASE_URL, DOT_ANALYTICS_TENANT, DOT_ANALYTICS_PROJECT)");
                return Health.CONFIGURATION_ERROR.name();
            }

            // Reachability probe — the only check that should fail loudly for monitoring.
            return EventAnalyticsProxyHelper.healthCheck()
                    ? Health.OK.name()
                    : Health.CONFIGURATION_ERROR.name();
        } catch (final Exception e) {
            Logger.error(this, String.format("Content Analytics health check failed: %s",
                    ExceptionUtil.getErrorMessage(e)), e);
            return Health.CONFIGURATION_ERROR.name();
        }
    }

    /**
     * Looks up the unified {@link HealthService} via CDI. A request only reaches this helper
     * through the Jersey REST stack, which runs after CDI is up — so if this lookup fails the
     * application is not healthy and the caller should treat the subsystem as DOWN.
     */
    HealthService healthService() {
        return CDI.current().select(HealthService.class).get();
    }

    /**
     * Determines if the database server is healthy by delegating to the unified health system
     * so the legacy monitor endpoint and {@code /readyz} return the same answer.
     *
     * @return If the database server is healthy, returns {@code true}.
     */
    boolean isDBHealthy()  {
        try {
            return healthService().isDatabaseHealthy();
        } catch (final Exception e) {
            Logger.warnAndDebug(MonitorHelper.class,
                    "unable to query health service for DB: " + ExceptionUtil.getErrorMessage(e), e);
            return false;
        }
    }

    /**
     * Determines if dotCMS can connect to Elasticsearch by delegating to the unified health
     * system (same probe as {@code /readyz}).
     *
     * @return If dotCMS can connect to Elasticsearch, returns {@code true}.
     */
    boolean canConnectToES() {
        try {
            return healthService().isSearchServiceHealthy();
        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(),
                    "unable to query health service for ES: " + ExceptionUtil.getErrorMessage(e), e);
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
     * Determines if the cache is healthy by delegating to the unified health system (same probe
     * as {@code /readyz}).
     *
     * @return If the cache is healthy, returns {@code true}.
     */
    boolean isCacheHealthy()  {
        try {
            return healthService().isHealthCheckUp("cache");
        } catch (final Exception e){
            Logger.warnAndDebug(this.getClass(),
                    "unable to query health service for cache: " + ExceptionUtil.getErrorMessage(e), e);
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
