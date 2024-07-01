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
import com.dotmarketing.util.WebKeys;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;


class MonitorHelper {


    private static final String[] DEFAULT_IP_ACL_VALUE = new String[]{"127.0.0.1/32", "10.0.0.0/8", "172.16.0.0/12",
            "192.168.0.0/16"};


    private static final String SYSTEM_STATUS_API_IP_ACL = "SYSTEM_STATUS_API_IP_ACL";


    private static final long SYSTEM_STATUS_CACHE_RESPONSE_SECONDS = Config.getLongProperty(
            "SYSTEM_STATUS_CACHE_RESPONSE_SECONDS", 10);

    private static final String[] ACLS_IPS = Config.getStringArrayProperty(SYSTEM_STATUS_API_IP_ACL,
            DEFAULT_IP_ACL_VALUE);

    
    static final AtomicReference<Tuple2<Long, MonitorStats>> cachedStats = new AtomicReference<>();
    boolean accessGranted = false;
    boolean useExtendedFormat = false;


    MonitorHelper(final HttpServletRequest request)  {
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

    boolean startedUp() {
        return System.getProperty(WebKeys.DOTCMS_STARTED_UP)!=null;
    }



    MonitorStats getMonitorStats()  {
        if (cachedStats.get() != null && cachedStats.get()._1 > System.currentTimeMillis()) {
            return cachedStats.get()._2;
        }
        return getMonitorStatsNoCache();
    }


    synchronized MonitorStats getMonitorStatsNoCache()  {
        // double check
        if (cachedStats.get() != null && cachedStats.get()._1 > System.currentTimeMillis()) {
            return cachedStats.get()._2;
        }



        final MonitorStats monitorStats = new MonitorStats();

        final IndiciesInfo indiciesInfo = Try.of(()->APILocator.getIndiciesAPI().loadIndicies()).getOrElseThrow(DotRuntimeException::new);

        monitorStats.subSystemStats.isDBHealthy = isDBHealthy();
        monitorStats.subSystemStats.isLiveIndexHealthy = isIndexHealthy(indiciesInfo.getLive());
        monitorStats.subSystemStats.isWorkingIndexHealthy = isIndexHealthy(indiciesInfo.getWorking());
        monitorStats.subSystemStats.isCacheHealthy = isCacheHealthy();
        monitorStats.subSystemStats.isLocalFileSystemHealthy = isLocalFileSystemHealthy();
        monitorStats.subSystemStats.isAssetFileSystemHealthy = isAssetFileSystemHealthy();


        monitorStats.serverId = getServerID();
        monitorStats.clusterId = getClusterID();


        // cache a healthy response
        if (monitorStats.isDotCMSHealthy()) {
            cachedStats.set( Tuple.of(System.currentTimeMillis() + (SYSTEM_STATUS_CACHE_RESPONSE_SECONDS * 1000),
                    monitorStats));
        }
        return monitorStats;
    }


    boolean isDBHealthy()  {


            return Try.of(()->
                            new DotConnect().setSQL("SELECT count(*) as count FROM (SELECT 1 FROM dot_cluster LIMIT 1) AS t")
                    .loadInt("count"))
                    .onFailure(e->Logger.warnAndDebug(MonitorHelper.class, "unable to connect to db:" + e.getMessage(),e))
                    .getOrElse(0) > 0;


    }


    boolean isIndexHealthy(final String index) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(0);
        searchSourceBuilder.timeout(TimeValue
                .timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        searchSourceBuilder.fetchSource(new String[]{"inode"}, null);
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(index);

        long totalHits = Try.of(()->
                RestHighLevelClientProvider
                        .getInstance()
                        .getClient()
                        .search(searchRequest,RequestOptions.DEFAULT)
                        .getHits()
                        .getTotalHits()
                        .value)
                .onFailure(e->Logger.warnAndDebug(MonitorHelper.class, "unable to connect to index:" + e.getMessage(),e))
                .getOrElse(0L);

        return totalHits > 0;

    }


    boolean isCacheHealthy()  {
        try {
            APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST);
            return UtilMethods.isSet(APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST).getId());
        }
        catch (Exception e){
            Logger.warnAndDebug(this.getClass(), "unable to find SYSTEM_HOST: " + e.getMessage(), e);
            return false;
        }

    }

    boolean isLocalFileSystemHealthy()  {

        return new FileSystemTest(ConfigUtils.getDynamicContentPath()).call();

    }

    boolean isAssetFileSystemHealthy() {

        return new FileSystemTest(ConfigUtils.getAssetPath()).call();

    }


    private String getServerID()  {
        return APILocator.getServerAPI().readServerId();

    }

    private String getClusterID()  {
        return ClusterFactory.getClusterId();


    }

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
                Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
                return false;
            }
            return false;
        }
    }
}
