package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.content.elasticsearch.business.ClusterStats;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
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


class MonitorHelper {

    final boolean accessGranted ;
    final boolean useExtendedFormat;
    private static final String[] DEFAULT_IP_ACL_VALUE = new String[]{"127.0.0.1/32", "10.0.0.0/8", "172.16.0.0/12",
            "192.168.0.0/16"};


    private final static String IPV6_LOCALHOST = "0:0:0:0:0:0:0:1";
    private static final String SYSTEM_STATUS_API_IP_ACL = "SYSTEM_STATUS_API_IP_ACL";


    private static final long SYSTEM_STATUS_CACHE_RESPONSE_SECONDS = Config.getLongProperty(
            "SYSTEM_STATUS_CACHE_RESPONSE_SECONDS", 10);

    private static final String[] ACLS_IPS = Config.getStringArrayProperty(SYSTEM_STATUS_API_IP_ACL,
            DEFAULT_IP_ACL_VALUE);


    static final AtomicReference<Tuple2<Long, MonitorStats>> cachedStats = new AtomicReference<>();


    MonitorHelper(final HttpServletRequest request, boolean heavyCheck) {
        this.useExtendedFormat = heavyCheck;
        this.accessGranted = isAccessGranted(request);
    }


    boolean isAccessGranted(HttpServletRequest request){

        try {
            if(IPV6_LOCALHOST.equals(request.getRemoteAddr()) || ACLS_IPS == null || ACLS_IPS.length == 0){
                return true;
            }

            final String clientIP = HttpRequestDataUtil.getIpAddress(request).toString().split(StringPool.SLASH)[1];

            for (String aclIP : ACLS_IPS) {
                if (IPUtils.isIpInCIDR(clientIP, aclIP)) {
                    return true;
                }
            }

        } catch (Exception e) {
            Logger.warnEveryAndDebug(this.getClass(), e, 60000);
        }
        return false;
    }



    boolean isStartedUp() {
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



        final MonitorStats monitorStats = new MonitorStats
                .Builder()
                .cacheHealthy(isCacheHealthy())
                .assetFSHealthy(isAssetFileSystemHealthy())
                .localFSHealthy(isLocalFileSystemHealthy())
                .dBHealthy(isDBHealthy())
                .esHealthy(canConnectToES())
                .build();



        // cache a healthy response
        if (monitorStats.isDotCMSHealthy()) {
            cachedStats.set( Tuple.of(System.currentTimeMillis() + (SYSTEM_STATUS_CACHE_RESPONSE_SECONDS * 1000),
                    monitorStats));
        }
        return monitorStats;
    }



    boolean isDBHealthy()  {


            return Try.of(()->
                            new DotConnect().setSQL("SELECT 1 as count")
                    .loadInt("count"))
                    .onFailure(e->Logger.warnAndDebug(MonitorHelper.class, "unable to connect to db:" + e.getMessage(),e))
                    .getOrElse(0) > 0;


    }


    boolean canConnectToES() {
        try{
            ClusterStats stats = APILocator.getESIndexAPI().getClusterStats();
            if(stats == null || stats.getClusterName()==null){
                return false;
            }
            return true;
        }
        catch (Exception e){
            Logger.warnAndDebug(this.getClass(), "unable to connect to ES: " + e.getMessage(), e);
            return false;
        }
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
                Logger.warnAndDebug(this.getClass(), "Unable to write a file to: " + initialPath + " : " +e.getMessage(), e);
                return false;
            }
            return false;
        }
    }
}
