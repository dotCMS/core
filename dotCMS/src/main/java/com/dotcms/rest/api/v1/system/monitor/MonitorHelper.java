package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.repackage.javax.ws.rs.InternalServerErrorException;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.*;
import com.liferay.util.StringPool;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

class MonitorHelper {

    public static final String SYSTEM_STATUS_API_IP_ACL = "SYSTEM_STATUS_API_IP_ACL";
    public static final String DEFAULT_IP_ACL_VALUE     = "127.0.0.1/32,0:0:0:0:0:0:0:1/128";
    private final IndiciesAPI  indiciesAPI              =  APILocator.getIndiciesAPI();

    public Boolean getAccessAllowed(final @Context HttpServletRequest request) throws UnknownHostException {

        final String configIpACL = Config.getStringProperty
                (SYSTEM_STATUS_API_IP_ACL, DEFAULT_IP_ACL_VALUE);
        final String[] aclIPs = configIpACL != null?
                configIpACL.split(StringPool.COMMA): null;

        final String clientIP = HttpRequestDataUtil.getIpAddress(request).
                toString().split(StringPool.FORWARD_SLASH)[1];
        boolean accessAllowed = false;

        if(aclIPs == null) {

            accessAllowed = true;
        } else {

            for(final String aclIP : aclIPs) {
                if(IPUtils.isIpInCIDR(clientIP, aclIP)) {
                    accessAllowed = true;
                    break;
                }
            }
        }

        return accessAllowed;
    } // getAccessAllowed.


    @CloseDBIfOpened
    public MonitorResultView getMonitorStats (final boolean extendedFormat) throws Throwable {

        final IndiciesAPI.IndiciesInfo indiciesInfo   = this.indiciesAPI.loadIndicies();
        final long localFsTimeOut                     = Config.getLongProperty("SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT", 1000);
        final long cacheTimeOut                       = Config.getLongProperty("SYSTEM_STATUS_API_CACHE_TIMEOUT", 1000);
        final long assetsFsTimeOut                    = Config.getLongProperty("SYSTEM_STATUS_API_ASSET_FS_TIMEOUT", 1000);
        final long indexTimeout                       = Config.getLongProperty("SYSTEM_STATUS_API_INDEX_TIMEOUT", 1000);
        final long dbTimeout                          = Config.getLongProperty("SYSTEM_STATUS_API_DB_TIMEOUT", 1000);
        final boolean dbSelectHealthy                 = dbCount(dbTimeout);
        final boolean indexLiveHealthy                = indexCount(indiciesInfo.live, indexTimeout);
        final boolean indexWorkingHealthy             = indexCount(indiciesInfo.working, indexTimeout);
        final boolean cacheHealthy                    = cache(cacheTimeOut);
        final boolean localFSHealthy                  = localFiles(localFsTimeOut);
        final boolean assetFSHealthy                  = assetFiles(assetsFsTimeOut);
        final MonitorResultView.Builder monitorView   = new MonitorResultView.Builder();


        monitorView.dotCMSHealthy(false).frontendHealthy(false).backendHealthy(false)
                .dbSelectHealthy(dbSelectHealthy).indexLiveHealthy(indexLiveHealthy)
                .indexWorkingHealthy(indexWorkingHealthy)
                .cacheHealthy(cacheHealthy).localFSHealthy(localFSHealthy)
                .assetFSHealthy(assetFSHealthy);

        if (extendedFormat) {
            monitorView.serverID(getServerID(localFsTimeOut))
                    .clusterID(getClusterID(dbTimeout));
        }

        if (dbSelectHealthy && indexLiveHealthy && indexWorkingHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {

            monitorView.dotCMSHealthy(true).frontendHealthy(true).backendHealthy (true);
        } else if (!indexWorkingHealthy && dbSelectHealthy && indexLiveHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {

            monitorView.frontendHealthy (true);
        }

        return monitorView.build();
    } // getMonitorStats.


    private boolean dbCount(final long dbTimeout) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(dbTimeout, this::dbCountCallback));

    }

    @CloseDBIfOpened
    private Boolean dbCountCallback() throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as count from contentlet");
        final List<Map<String,String>> results = dc.loadResults();
        long count = Long.parseLong(results.get(0).get("count"));
        return count > 0;
    }


    private boolean indexCount(final String idx, final long indexTimeout) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(indexTimeout, () -> indexCountCallback(idx)));
    }

    @CloseDBIfOpened
    private Boolean indexCountCallback(String idx) {
        final Client client = new ESClient().getClient();
        final long totalHits = client.prepareSearch(idx)
                .setQuery(QueryBuilders.termQuery("_type", "content"))
                .setSize(0)
                .execute()
                .actionGet()
                .getHits()
                .getTotalHits();
        return totalHits > 0;
    }

    private boolean cache(final long cacheTimeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(cacheTimeOut, this::cacheCallback));

    }

    @CloseDBIfOpened
    private Boolean cacheCallback() {

        Identifier id = null;

        try {
            id = APILocator.getIdentifierAPI().loadFromCache(Host.SYSTEM_HOST);
            if(id==null || !UtilMethods.isSet(id.getId())){
                id =  APILocator.getIdentifierAPI().find(Host.SYSTEM_HOST);
                id =  APILocator.getIdentifierAPI().loadFromCache(Host.SYSTEM_HOST);
            }
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
        }

        return id!=null && UtilMethods.isSet(id.getId());
    }


    private boolean localFiles(final long localFsTimeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(localFsTimeOut, () -> {

                    final String realPath = ConfigUtils.getDynamicContentPath()
                            + File.separator
                            + "monitor"
                            + File.separator
                            + System.currentTimeMillis();
                    File file = new File(realPath);
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


    private boolean assetFiles(final long assetsFsTimeOut) throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(assetsFsTimeOut, () -> {
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

    private Callable<Boolean> failFastBooleanPolicy(long thresholdMilliseconds, final Callable<Boolean> callable) throws Throwable {
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

    private Callable<String> failFastStringPolicy(long thresholdMilliseconds, final Callable<String> callable) throws Throwable {
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

    private String getServerID(final long localFsTimeOut) throws Throwable{
        return Failsafe
                .with(breaker())
                .withFallback("UNKNOWN")
                .get(failFastStringPolicy(localFsTimeOut, () -> {
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

    private String getClusterID(final long dbTimeout) throws Throwable{
        return Failsafe
                .with(breaker())
                .withFallback("UNKNOWN")
                .get(failFastStringPolicy(dbTimeout, () -> {
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
} // E:O:F:MonitorHelper.
