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
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;


class MonitorHelper {
    private static final long   DEFAULT_LOCAL_FS_TIMEOUT    = 1000;
    private static final long   DEFAULT_CACHE_TIMEOUT       = 1000;
    private static final long   DEFAULT_ASSET_FS_TIMEOUT    = 1000;
    private static final long   DEFAULT_INDEX_TIMEOUT       = 1000;
    private static final long   DEFAULT_DB_TIMEOUT          = 1000;
    private static final String[] DEFAULT_IP_ACL_VALUE        = new String[] {"127.0.0.1/32","10.0.0.0/8","172.16.0.0/12","192.168.0.0/16"};

    private static final String SYSTEM_STATUS_API_IP_ACL           = "SYSTEM_STATUS_API_IP_ACL";
    private static final String SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT = "SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT";
    private static final String SYSTEM_STATUS_API_CACHE_TIMEOUT    = "SYSTEM_STATUS_API_CACHE_TIMEOUT";
    private static final String SYSTEM_STATUS_API_ASSET_FS_TIMEOUT = "SYSTEM_STATUS_API_ASSET_FS_TIMEOUT";
    private static final String SYSTEM_STATUS_API_INDEX_TIMEOUT    = "SYSTEM_STATUS_API_INDEX_TIMEOUT";
    private static final String SYSTEM_STATUS_API_DB_TIMEOUT       = "SYSTEM_STATUS_API_DB_TIMEOUT";

    private static final int SYSTEM_STATUS_CACHE_RESPONSE_SECONDS = Config.getIntProperty("SYSTEM_STATUS_CACHE_RESPONSE_SECONDS",10);
    
    private static final String[] ACLS_IPS = Config.getStringArrayProperty(SYSTEM_STATUS_API_IP_ACL, DEFAULT_IP_ACL_VALUE);

    private static final long localFSTimeout = Config.getLongProperty(SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT, DEFAULT_LOCAL_FS_TIMEOUT);
    private static final long cacheTimeout  = Config.getLongProperty(SYSTEM_STATUS_API_CACHE_TIMEOUT, DEFAULT_CACHE_TIMEOUT);
    private static final long assetTimeout = Config.getLongProperty(SYSTEM_STATUS_API_ASSET_FS_TIMEOUT, DEFAULT_ASSET_FS_TIMEOUT);
    private static final long indexTimeout = Config.getLongProperty(SYSTEM_STATUS_API_INDEX_TIMEOUT, DEFAULT_INDEX_TIMEOUT);
    private static final long dbTimeout = Config.getLongProperty(SYSTEM_STATUS_API_DB_TIMEOUT, DEFAULT_DB_TIMEOUT);

    private static final String UNKNOWN = "UNKNOWN";

    boolean accessGranted = false;
    boolean useExtendedFormat;

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
        }catch(Exception e){
            Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    
    static Tuple2<Long,MonitorStats> cachedStats=null;
    
    MonitorStats getMonitorStats() throws Throwable{
        if(cachedStats!=null && cachedStats._1 > System.currentTimeMillis()) {
            return cachedStats._2;
        }
        return getMonitorStatsNoCache();
    }
    
    MonitorStats getMonitorStatsNoCache() throws Throwable{

        final MonitorStats monitorStats = new MonitorStats();

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        monitorStats.subSystemStats.isDBHealthy = isDBHealthy(dbTimeout);
        monitorStats.subSystemStats.isLiveIndexHealthy = isIndexHealthy(indiciesInfo.getLive(), indexTimeout);
        monitorStats.subSystemStats.isWorkingIndexHealthy = isIndexHealthy(indiciesInfo.getWorking(), indexTimeout);
        monitorStats.subSystemStats.isCacheHealthy = isCacheHealthy(cacheTimeout);
        monitorStats.subSystemStats.isLocalFileSystemHealthy = isLocalFileSystemHealthy(localFSTimeout);
        monitorStats.subSystemStats.isAssetFileSystemHealthy = isAssetFileSystemHealthy(assetTimeout);

        if (useExtendedFormat) {
            monitorStats.serverId = getServerID(assetTimeout);
            monitorStats.clusterId = getClusterID(dbTimeout);
        }
        
        // cache a healthy response
        if(monitorStats.isDotCMSHealthy()) {
            cachedStats = Tuple.of(System.currentTimeMillis()+(SYSTEM_STATUS_CACHE_RESPONSE_SECONDS*1000) , monitorStats);
        }
        return monitorStats;
    }

    boolean isDBHealthy(final long timeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(timeOut, () -> {
                    try{
                        final DotConnect dc = new DotConnect();
                        if(DbConnectionFactory.isPostgres()) {
                            return dc.setSQL("SELECT count(*) as count FROM (SELECT 1 FROM dot_cluster LIMIT 1) AS t").loadInt("count")>0;
                        }
                        else {
                            return  dc.setSQL("SELECT count(*) as count from dot_cluster").loadInt("count")>0;
                        }
                    }
                    catch(Exception e) {
                        Logger.warn(getClass(), "db connection failing:" + e.getMessage() );
                        return false;
                    }
                    finally{
                        DbConnectionFactory.closeSilently();
                    }
                }));

    }

    boolean isIndexHealthy(final String index, final long timeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(timeOut, () -> {
                    try{

                        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                        searchSourceBuilder.size(0);
                        searchSourceBuilder.timeout(TimeValue
                                .timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
                        searchSourceBuilder.fetchSource(new String[] {"inode"}, null);
                        SearchRequest searchRequest = new SearchRequest();
                        searchRequest.source(searchSourceBuilder);
                        searchRequest.indices(index);

                        final SearchResponse response = Sneaky.sneak(()->
                                RestHighLevelClientProvider.getInstance().getClient().search(searchRequest,
                                        RequestOptions.DEFAULT));
                        return response.getHits().getTotalHits().value>0;
                    }catch(Exception e) {
                        Logger.warn(getClass(), "ES connection failing: " + e.getMessage() );
                        return false;
                    }finally{
                        DbConnectionFactory.closeSilently();
                    }
                }));
    }

    private boolean isCacheHealthy() {
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

    private static final class FileSystemTest {

        private final String initialPath;

        public FileSystemTest(final String initialPath) {
            this.initialPath = initialPath.endsWith(File.separator) ? initialPath : initialPath + File.separator;
        }

        public Boolean get() throws Exception {
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

    private boolean getBoolean(final CheckedFunction0<Boolean> getter) {
        return Try.of(getter).getOrElse(Boolean.FALSE);
    }

    private boolean getBoolean(final String path) {
        return getBoolean(() -> new FileSystemTest(path).get());
    }
    
    private boolean isLocalFileSystemHealthy() {
        return getBoolean(ConfigUtils.getDynamicContentPath());
    }

    private boolean isAssetFileSystemHealthy() {
        return getBoolean(ConfigUtils.getAbsoluteAssetsRootPath());
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

    private Callable<Boolean> failFastBooleanPolicy(long thresholdMilliseconds, final Callable<Boolean> callable) throws Throwable{
        return ()-> {
            try {
                final DotSubmitter executorService = DotConcurrentFactory.getInstance().getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
                final Future<Boolean> task = executorService.submit(callable);
                return task.get(thresholdMilliseconds, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new InternalServerErrorException("Internal exception ", e.getCause());
            } catch (TimeoutException e) {
                throw new InternalServerErrorException("Execution aborted, exceeded allowed " + thresholdMilliseconds + " threshold", e.getCause());
            }
        };
    }

    /*private Callable<String> failFastStringPolicy(long thresholdMilliseconds, final Callable<String> callable) throws Throwable{
        return ()-> {
            try {
                final DotSubmitter executorService = DotConcurrentFactory.getInstance().getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
                final Future<String> task = executorService.submit(callable);
                return task.get(thresholdMilliseconds, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new InternalServerErrorException("Internal exception ", e.getCause());
            } catch (TimeoutException e) {
                throw new InternalServerErrorException("Execution aborted, exceeded allowed " + thresholdMilliseconds + " threshold", e.getCause());
            }
        };
    }*/

    private CircuitBreaker breaker(){
        return new CircuitBreaker();
    }

}
