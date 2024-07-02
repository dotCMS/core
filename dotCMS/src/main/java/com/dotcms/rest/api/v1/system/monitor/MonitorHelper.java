package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.CheckedFunction0;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

/**
 * Provides utilities for monitoring various system and cluster health aspects.
 * It includes checks for database connectivity, index health, cache functionality,
 * file system integrity, and more. It also supports generating extended format
 * statistics based on request parameters.
 */
class MonitorHelper {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String SYSTEM_STATUS_API_IP_ACL = "SYSTEM_STATUS_API_IP_ACL";
    private static final String[] DEFAULT_IP_ACL_VALUE = new String[] {
            "127.0.0.1/32",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16"
    };
    private static final String[] ACLS_IPS = Config.getStringArrayProperty(
            SYSTEM_STATUS_API_IP_ACL,
            DEFAULT_IP_ACL_VALUE);
    private static final int SYSTEM_STATUS_CACHE_RESPONSE_SECONDS = Config.getIntProperty(
            "SYSTEM_STATUS_CACHE_RESPONSE_SECONDS",
            10);

    private boolean accessGranted;
    private boolean useExtendedFormat;

    MonitorHelper(final HttpServletRequest request) {
        try {
            this.useExtendedFormat = request.getParameter("extended") != null;
            final String clientIP = HttpRequestDataUtil.getIpAddress(request).toString().split(StringPool.SLASH)[1];

            if (ACLS_IPS == null || ACLS_IPS.length == 0) {
                this.accessGranted = true;
            } else {
                for (String aclIP : ACLS_IPS) {
                    if (IPUtils.isIpInCIDR(clientIP, aclIP)) {
                        this.accessGranted = true;
                        break;
                    }
                }
            }
        } catch(Exception e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Determines if the extended format is used for the monitor stats.
     *
     * @return true if the extended format is requested, false otherwise.
     */
    boolean isUseExtendedFormat() {
        return useExtendedFormat;
    }

    /**
     * Checks if the access is granted based on the client IP and configured ACLs.
     *
     * @return true if the access is granted, false otherwise.
     */
    boolean isAccessGranted() {
        return accessGranted;
    }

    /**
     * Retrieves the monitor statistics, either from cache or by calculating them
     * if the cache is stale or disabled. This includes health checks for the database,
     * indices, cache, and file systems.
     *
     * @return an instance of {@link MonitorStats} containing the health check results.
     */
    MonitorStats getMonitorStats() {
        return CachedMonitorStats.get().getMonitorStats(() -> {
            try {
                return getMonitorStatsNoCache();
            } catch (DotDataException e) {
                throw new DotRuntimeException(e);
            }
        });
    }

    /**
     * Performs a health check on the cache system by attempting to retrieve a known contentlet.
     *
     * @return true if the cache system is operational, false if it fails the health check.
     */
    boolean isCacheHealthy() {
        return getBoolean(() -> {
            try {
                final Contentlet con = APILocator
                        .getContentletAPI()
                        .findContentletByIdentifier(
                                Host.SYSTEM_HOST,
                                false,
                                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                                APILocator.systemUser(), false);
                return UtilMethods.isSet(con::getIdentifier);
            } catch(Exception e) {
                Logger.warn(getClass(), "Cache is failing: " + e.getMessage() );
                throw e;
            } finally {
                DbConnectionFactory.closeSilently();
            }
        });
    }

    private MonitorStats getMonitorStatsNoCache() throws DotDataException {
        final MonitorStats monitorStats = new MonitorStats();
        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        monitorStats.subSystemStats.isDBHealthy = isDBHealthy();
        monitorStats.subSystemStats.isLiveIndexHealthy = isIndexHealthy(indiciesInfo.getLive());
        monitorStats.subSystemStats.isWorkingIndexHealthy = isIndexHealthy(indiciesInfo.getWorking());
        monitorStats.subSystemStats.isCacheHealthy = isCacheHealthy();
        monitorStats.subSystemStats.isLocalFileSystemHealthy = isLocalFileSystemHealthy();
        monitorStats.subSystemStats.isAssetFileSystemHealthy = isAssetFileSystemHealthy();

        if (useExtendedFormat) {
            monitorStats.serverId = getServerID();
            monitorStats.clusterId = getClusterID();
        }

        return monitorStats;
    }

    private boolean getBoolean(final CheckedFunction0<Boolean> getter) {
        return Try.of(getter).getOrElse(Boolean.FALSE);
    }

    private boolean getBoolean(final String path) {
        return getBoolean(() -> new FileSystemTest(path).get());
    }

    private boolean isDBHealthy() {
        return getBoolean(() -> {
            try {
                final DotConnect dc = new DotConnect();
                // Isn't this our only supported DB?
                if (DbConnectionFactory.isPostgres()) {
                    return dc
                            .setSQL("SELECT count(*) as count FROM (SELECT 1 FROM dot_cluster LIMIT 1) AS t")
                            .loadInt("count") > 0;
                } else {
                    return dc.setSQL("SELECT count(*) as count from dot_cluster").loadInt("count") > 0;
                }
            } catch(Exception e) {
                Logger.warn(getClass(), "db connection failing:" + e.getMessage() );
                return false;
            } finally {
                DbConnectionFactory.closeSilently();
            }
        });
    }

    private boolean isIndexHealthy(final String index) {
        return getBoolean(() -> {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.size(0);
            searchSourceBuilder.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
            searchSourceBuilder.fetchSource(new String[] {"inode"}, null);
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.source(searchSourceBuilder);
            searchRequest.indices(index);
            try {
                final SearchResponse response = Sneaky.sneak(() -> RestHighLevelClientProvider
                        .getInstance()
                        .getClient()
                        .search(searchRequest, RequestOptions.DEFAULT));
                return response.getHits().getTotalHits().value > 0;
            } catch(Exception e) {
                Logger.warn(getClass(), "ES connection failing: " + e.getMessage() );
                throw e;
            } finally {
                DbConnectionFactory.closeSilently();
            }
        });
    }

    private boolean isLocalFileSystemHealthy() {
        return getBoolean(ConfigUtils.getDynamicContentPath());
    }

    private boolean isAssetFileSystemHealthy() {
        return getBoolean(ConfigUtils.getAssetPath());
    }

    private String getString(final CheckedFunction0<String> getter) {
        return Try.of(getter).getOrElse(UNKNOWN);
    }

    private String getServerID() {
        return getString(() -> APILocator.getServerAPI().readServerId());
    }

    private String getClusterID() {
        return getString(ClusterFactory::getClusterId);
    }

    private static final class FileSystemTest {

        private final String initialPath;

        public FileSystemTest(final String initialPath) {
            this.initialPath = initialPath.endsWith(File.separator) ? initialPath : initialPath + File.separator;
        }

        public Boolean get() throws IOException {
            final String uuid = UUIDUtil.uuid();
            final String realPath = Path.of(initialPath,"monitor", uuid).toString();
            final File file = new File(realPath);
            if (file.mkdirs() && Files.deleteIfExists(file.toPath()) && file.createNewFile()) {
                try (OutputStream os = Files.newOutputStream(file.toPath())) {
                    os.write(uuid.getBytes());
                }
                return Files.deleteIfExists(file.toPath());
            }
            return false;
        }

    }

    private static final class CachedMonitorStats {

        private static final Lazy<CachedMonitorStats> INSTANCE = Lazy.of(CachedMonitorStats::new);

        static CachedMonitorStats get() {
            return INSTANCE.get();
        }

        private long cachedAt;
        private MonitorStats monitorStats;

        private CachedMonitorStats() {
            // no-op
        }

        MonitorStats getMonitorStats(final Supplier<MonitorStats> newMonitorStats) {
            if (Objects.nonNull(monitorStats) && cachedAt < System.currentTimeMillis()) {
                return monitorStats;
            }

            cachedAt = System.currentTimeMillis() + (SYSTEM_STATUS_CACHE_RESPONSE_SECONDS * 1000L);
            monitorStats = newMonitorStats.get();
            return monitorStats;
        }

    }

}
