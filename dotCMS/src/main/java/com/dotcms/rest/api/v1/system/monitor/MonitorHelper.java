package com.dotcms.rest.api.v1.system.monitor;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;


class MonitorHelper {


    private static final String[] DEFAULT_IP_ACL_VALUE = new String[]{"127.0.0.1/32", "10.0.0.0/8", "172.16.0.0/12",
            "192.168.0.0/16"};


    private static final String SYSTEM_STATUS_API_IP_ACL = "SYSTEM_STATUS_API_IP_ACL";


    private static final int SYSTEM_STATUS_CACHE_RESPONSE_SECONDS = Config.getIntProperty(
            "SYSTEM_STATUS_CACHE_RESPONSE_SECONDS", 10);

    private static final String[] ACLS_IPS = Config.getStringArrayProperty(SYSTEM_STATUS_API_IP_ACL,
            DEFAULT_IP_ACL_VALUE);

    
    static Tuple2<Long, MonitorStats> cachedStats = null;
    boolean accessGranted = false;
    boolean useExtendedFormat = false;


    MonitorHelper(final HttpServletRequest request) throws UnknownHostException {
        try {
            this.useExtendedFormat = request.getParameter("extended") != null;

            // set this.accessGranted

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
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    MonitorStats getMonitorStats() throws Throwable {
        if (cachedStats != null && cachedStats._1 > System.currentTimeMillis()) {
            return cachedStats._2;
        }
        return getMonitorStatsNoCache();
    }


    MonitorStats getMonitorStatsNoCache() throws Throwable {

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

        // cache a healthy response
        if (monitorStats.isDotCMSHealthy()) {
            cachedStats = Tuple.of(System.currentTimeMillis() + (SYSTEM_STATUS_CACHE_RESPONSE_SECONDS * 1000),
                    monitorStats);
        }
        return monitorStats;
    }


    boolean isDBHealthy() throws Throwable {

        return new DotConnect().setSQL("SELECT count(*) as count FROM (SELECT 1 FROM dot_cluster LIMIT 1) AS t")
                .loadInt("count") > 0;


    }


    boolean isIndexHealthy(final String index) throws Throwable {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(0);
        searchSourceBuilder.timeout(TimeValue
                .timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchSourceBuilder.fetchSource(new String[]{"inode"}, null);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(index);

        final SearchResponse response = Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient().search(searchRequest,
                        RequestOptions.DEFAULT));
        return response.getHits().getTotalHits().value > 0;

    }


    boolean isCacheHealthy() throws Throwable {

        APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST);
        return UtilMethods.isSet(APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST).getId());


    }

    boolean isLocalFileSystemHealthy() throws Throwable {

        return new FileSystemTest(ConfigUtils.getDynamicContentPath()).call();

    }

    boolean isAssetFileSystemHealthy() throws Throwable {

        return new FileSystemTest(ConfigUtils.getAssetPath()).call();

    }


    private String getServerID() throws Throwable {
        return APILocator.getServerAPI().readServerId();

    }

    private String getClusterID() throws Throwable {
        return ClusterFactory.getClusterId();


    }

    final class FileSystemTest implements Callable<Boolean> {

        final String initialPath;

        public FileSystemTest(String initialPath) {
            this.initialPath = initialPath.endsWith(File.separator) ? initialPath : initialPath + File.separator;
        }

        @Override
        public Boolean call() throws Exception {
            final String uuid = UUIDUtil.uuid();
            final String realPath = initialPath
                    + "monitor"
                    + File.separator
                    + uuid;
            final File file = new File(realPath);
            if (file.mkdirs() && file.delete() && file.createNewFile()) {
                try (OutputStream os = Files.newOutputStream(file.toPath())) {
                    os.write(uuid.getBytes());
                }catch (Exception e){
                    Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
                    return false;
                }
                return file.delete();
            }
            return false;
        }


    }
}
