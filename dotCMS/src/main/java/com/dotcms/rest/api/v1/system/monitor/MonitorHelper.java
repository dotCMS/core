package com.dotcms.rest.api.v1.system.monitor;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import java.io.File;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;


class MonitorHelper {
    private static final long   DEFAULT_LOCAL_FS_TIMEOUT    = 1000;
    private static final long   DEFAULT_CACHE_TIMEOUT       = 1000;
    private static final long   DEFAULT_ASSET_FS_TIMEOUT    = 1000;
    private static final long   DEFAULT_INDEX_TIMEOUT       = 1000;
    private static final long   DEFAULT_DB_TIMEOUT          = 1000;
    private static final String[] DEFAULT_IP_ACL_VALUE        = new String[] {"127.0.0.1/32","10.0.0.0/8","172.16.0.0/12","192.168.0.0/16","0:0:0:0:0:0:0:1"};


    private static final String SYSTEM_STATUS_API_IP_ACL           = "SYSTEM_STATUS_API_IP_ACL";
    private static final String SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT = "SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT";
    private static final String SYSTEM_STATUS_API_CACHE_TIMEOUT    = "SYSTEM_STATUS_API_CACHE_TIMEOUT";
    private static final String SYSTEM_STATUS_API_ASSET_FS_TIMEOUT = "SYSTEM_STATUS_API_ASSET_FS_TIMEOUT";
    private static final String SYSTEM_STATUS_API_INDEX_TIMEOUT    = "SYSTEM_STATUS_API_INDEX_TIMEOUT";
    private static final String SYSTEM_STATUS_API_DB_TIMEOUT       = "SYSTEM_STATUS_API_DB_TIMEOUT";

    private static final int SYSTEM_STATUS_CACHE_RESPONSE_SECONDS = Config.getIntProperty("SYSTEM_STATUS_CACHE_RESPONSE_SECONDS",10);



    boolean accessGranted = false;
    boolean useExtendedFormat = false;

    MonitorHelper(final HttpServletRequest request) throws UnknownHostException {
        this.useExtendedFormat = request.getParameter("extended")!=null;

        // set this.accessGranted
        final String[] aclIPs = Config.getStringArrayProperty(SYSTEM_STATUS_API_IP_ACL, DEFAULT_IP_ACL_VALUE);

        final String clientIP = HttpRequestDataUtil.getIpAddress(request).toString().split(StringPool.SLASH)[1];
        if(aclIPs == null) {
            this.accessGranted = true;
        }
        else {
            for(String aclIP : aclIPs) {
                if(IPUtils.isIpInCIDR(clientIP, aclIP)){
                    this.accessGranted = true;
                    break;
                }
            }
        }
    }


    static Tuple2<Long,MonitorStats> cachedStats=null;



    MonitorStats getMonitorStats() throws Throwable{

        if(cachedStats!=null && cachedStats._1 > System.currentTimeMillis()) {
            return cachedStats._2;
        }


        final MonitorStats monitorStats = new MonitorStats();

        final IndiciesInfo indiciesInfo = APILocator.getIndiciesAPI().loadIndicies();

        final long localFSTimeout = Config.getLongProperty(SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT, DEFAULT_LOCAL_FS_TIMEOUT);
        final long cacheTimeout  = Config.getLongProperty(SYSTEM_STATUS_API_CACHE_TIMEOUT, DEFAULT_CACHE_TIMEOUT);
        final long assetTimeout = Config.getLongProperty(SYSTEM_STATUS_API_ASSET_FS_TIMEOUT, DEFAULT_ASSET_FS_TIMEOUT);
        final long indexTimeout = Config.getLongProperty(SYSTEM_STATUS_API_INDEX_TIMEOUT, DEFAULT_INDEX_TIMEOUT);
        final long dbTimeout = Config.getLongProperty(SYSTEM_STATUS_API_DB_TIMEOUT, DEFAULT_DB_TIMEOUT);

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

    private boolean isDBHealthy(final long timeOut) throws Throwable {

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
                        return false;
                    }
                    finally{
                        DbConnectionFactory.closeSilently();
                    }
                }));

    }

    private boolean isIndexHealthy(final String index, final long timeOut) throws Throwable {

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
                    }finally{
                        DbConnectionFactory.closeSilently();
                    }
                }));
    }

    private boolean isCacheHealthy(final long timeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(timeOut, () -> {
                    try{
                        Identifier id =  APILocator.getIdentifierAPI().loadFromCache(Host.SYSTEM_HOST);
                        if(id==null || !UtilMethods.isSet(id.getId())){
                            id =  APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST);
                            id =  APILocator.getIdentifierAPI().loadFromCache(Host.SYSTEM_HOST);
                        }
                        return id!=null && UtilMethods.isSet(id.getId());
                    }finally{
                        DbConnectionFactory.closeSilently();
                    }
                }));

    }

    private boolean isLocalFileSystemHealthy(final long timeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(timeOut, () -> {

                    final String realPath = ConfigUtils.getDynamicContentPath()
                            + File.separator
                            + "monitor"
                            + File.separator
                            + System.currentTimeMillis();
                    final File file = new File(realPath);
                    file.mkdirs();
                    file.delete();
                    file.createNewFile();

                    try(OutputStream os = Files.newOutputStream(file.toPath())){
                        os.write(UUIDUtil.uuid().getBytes());
                    }
                    file.delete();
                    return Boolean.TRUE;
                }));
    }


    private boolean isAssetFileSystemHealthy(final long timeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(timeOut, () -> {
                    final String realPath =APILocator.getFileAssetAPI().getRealAssetPath(UUIDUtil.uuid());
                    final File file = new File(realPath);
                    file.mkdirs();
                    file.delete();
                    file.createNewFile();

                    try(OutputStream os = Files.newOutputStream(file.toPath())){
                        os.write(UUIDUtil.uuid().getBytes());
                    }
                    file.delete();

                    return true;
                }));
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

    private Callable<String> failFastStringPolicy(long thresholdMilliseconds, final Callable<String> callable) throws Throwable{
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
    }

    private CircuitBreaker breaker(){
        return new CircuitBreaker();
    }

    private String getServerID(final long timeOut) throws Throwable{
        return Failsafe
                .with(breaker())
                .withFallback("UNKNOWN")
                .get(failFastStringPolicy(timeOut, () -> {
                    String serverID = "UNKNOWN";
                    try {
                        serverID=APILocator.getServerAPI().readServerId();
                    }
                    catch (Throwable t) {
                        Logger.error(this, "Error - unable to get the serverID", t);
                    }
                    return serverID;
                }));

    }

    private String getClusterID(final long timeOut) throws Throwable{
        return Failsafe
                .with(breaker())
                .withFallback("UNKNOWN")
                .get(failFastStringPolicy(timeOut, () -> {
                    String clusterID = "UNKNOWN";
                    try {
                        clusterID=ClusterFactory.getClusterId();
                    }
                    catch (Throwable t) {
                        Logger.error(this, "Error - unable to get the clusterID", t);
                    }
                    return clusterID;
                }));

    }
}
