package com.dotcms.rest.api.v1.system.monitor;

import java.io.File;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.ResponseBuilder;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.json.JSONObject;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;

/**
 * 
 * 
 * Call
 *
 */
@Path("/v1/system-status")
public class MonitorResource {

    private final WebResource webResource = new WebResource();

    @Context
    private HttpServletRequest httpRequest;

    static long LOCAL_FS_TIMEOUT=5000;
    static long CACHE_TIMEOUT=5000;
    static long ASSET_FS_TIMEOUT=5000;
    static long INDEX_TIMEOUT=5000;
    static long DB_TIMEOUT=5000;
    ExecutorService executorService = Executors.newCachedThreadPool();

    @NoCache
    @GET
    @JSONP
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(@Context HttpServletRequest request) throws Throwable {
        // force authentication
        //InitDataObject auth = webResource.init(false, httpRequest, false);  cannot require as we cannot assume db or other subsystems are functioning
        boolean extendedFormat = false;
        if (request.getQueryString() != null && "extended".equals(request.getQueryString()))
            extendedFormat = true;

        String config_ip_acl = Config.getStringProperty("SYSTEM_STATUS_API_IP_ACL", "127.0.0.1/32,0:0:0:0:0:0:0:1");
        String[] aclIPs = null;
        if(config_ip_acl != null)
            aclIPs = config_ip_acl.split(",");

        String clientIP = HttpRequestDataUtil.getIpAddress(request).toString().split("/")[1];
        Boolean accessAllowed = false;
        if(aclIPs == null) {
            accessAllowed = true;
        }
        else {
            for(String aclIP : aclIPs) {
                if(HttpRequestDataUtil.isIpMatchingNetmask(clientIP, aclIP)){
                    accessAllowed = true;
                    break;
                }
            }
        }

        try {
            ResponseBuilder builder = null;
            if (accessAllowed) {
                IndiciesInfo idxs = APILocator.getIndiciesAPI().loadIndicies();
                LOCAL_FS_TIMEOUT = Config.getLongProperty("SYSTEM_STATUS_API_LOCAL_FS_TIMEOUT", 1000);
                CACHE_TIMEOUT = Config.getLongProperty("SYSTEM_STATUS_API_CACHE_TIMEOUT", 1000);
                ;
                ASSET_FS_TIMEOUT = Config.getLongProperty("SYSTEM_STATUS_API_ASSET_FS_TIMEOUT", 1000);
                ;
                INDEX_TIMEOUT = Config.getLongProperty("SYSTEM_STATUS_API_INDEX_TIMEOUT", 1000);
                ;
                DB_TIMEOUT = Config.getLongProperty("SYSTEM_STATUS_API_DB_TIMEOUT", 1000);
                ;

                boolean dotCMSHealthy = false;
                boolean frontendHealthy = false;
                boolean backendHealthy = false;
                boolean dbSelectHealthy = dbCount();
                boolean indexLiveHealthy = indexCount(idxs.live);
                boolean indexWorkingHealthy = indexCount(idxs.working);
                boolean cacheHealthy = cache();
                boolean localFSHealthy = localFiles();
                boolean assetFSHealthy = assetFiles();

                if (dbSelectHealthy && indexLiveHealthy && indexWorkingHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {
                    dotCMSHealthy = true;
                    frontendHealthy = true;
                    backendHealthy = true;
                } else if (!indexWorkingHealthy && dbSelectHealthy && indexLiveHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {
                    frontendHealthy = true;
                }

                if (extendedFormat) {
                    String serverID = getServerID();
                    String clusterID = getClusterID();
                    JSONObject jo = new JSONObject();
                    jo.put("serverID", serverID);
                    jo.put("clusterID", clusterID);
                    jo.put("dotCMSHealthy", dotCMSHealthy);
                    jo.put("frontendHealthy", frontendHealthy);
                    jo.put("backendHealthy", backendHealthy);

                    JSONObject subsystems = new JSONObject();
                    subsystems.put("dbSelectHealthy", dbSelectHealthy);
                    subsystems.put("indexLiveHealthy", indexLiveHealthy);
                    subsystems.put("indexWorkingHealthy", indexWorkingHealthy);
                    subsystems.put("cacheHealthy", cacheHealthy);
                    subsystems.put("localFSHealthy", localFSHealthy);
                    subsystems.put("assetFSHealthy", assetFSHealthy);
                    jo.put("subsystems", subsystems);

                    builder = Response.ok(jo.toString(2), MediaType.APPLICATION_JSON);
                } else {
                    if (dotCMSHealthy) {
                        builder = Response.ok("", MediaType.APPLICATION_JSON);
                    } else if (!indexWorkingHealthy && dbSelectHealthy && indexLiveHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {
                        builder = Response.status(507).entity("").type(MediaType.APPLICATION_JSON);
                    } else {
                        builder = Response.status(503).entity("").type(MediaType.APPLICATION_JSON);
                    }
                }
            }
            else {
                builder = Response.status(403).entity("").type(MediaType.APPLICATION_JSON); // Access is forbidden because IP is not in any range in ACL list
            }
            builder.header("Access-Control-Expose-Headers", "Authorization");
            builder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
            return builder.build();
        }
        finally{
            DbConnectionFactory.closeSilently();
        }
    }


    
    private boolean dbCount() throws Throwable {

        return Failsafe
                .with(breaker())
                .withFallback(Boolean.FALSE)
                .get(this.failFastBooleanPolicy(DB_TIMEOUT, () -> {
                    
                    try{
                        DotConnect dc = new DotConnect();
                        dc.setSQL("select count(*) as count from contentlet");
                        List<Map<String,String>> results = dc.loadResults();
                        long count = Long.parseLong(results.get(0).get("count"));
                        return count > 0;
                    }
                    finally{
                        DbConnectionFactory.closeSilently();
                    }
                }));

    }
    
    
    private boolean indexCount(String idx) throws Throwable {

        return Failsafe
            .with(breaker())
            .withFallback(Boolean.FALSE)
            .get(this.failFastBooleanPolicy(INDEX_TIMEOUT, () -> {
                try{
                    Client client=new ESClient().getClient();
                    long totalHits = client.prepareSearch(idx)
                        .setQuery(QueryBuilders.termQuery("_type", "content"))
                        .setSize(0)
                        .execute()
                        .actionGet()
                        .getHits()
                        .getTotalHits();
                    return totalHits > 0;
                }finally{
                    DbConnectionFactory.closeSilently();
                }
        }));
    }
    
    private boolean cache() throws Throwable {


        return Failsafe
            .with(breaker())
            .withFallback(Boolean.FALSE)
            .get(this.failFastBooleanPolicy(CACHE_TIMEOUT, () -> {
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
    
    
    
    
    
    private boolean localFiles() throws Throwable {

        boolean test = Failsafe
            .with(breaker())
            .withFallback(Boolean.FALSE)
            .get(this.failFastBooleanPolicy(LOCAL_FS_TIMEOUT, () -> {
    
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
                return new Boolean(true);
            }));
        return test;
    }
    
    
    private boolean assetFiles() throws Throwable {
    
        return Failsafe
            .with(breaker())
            .withFallback(Boolean.FALSE)
            .get(this.failFastBooleanPolicy(ASSET_FS_TIMEOUT, () -> {
                final String realPath =APILocator.getFileAssetAPI().getRealAssetPath(UUIDUtil.uuid());
                File file = new File(realPath);
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

    private Callable<Boolean> failFastBooleanPolicy(long thresholdMilliseconds, Callable<Boolean> callable) throws Throwable{
        return ()-> {
            try {
                Future<Boolean> task = executorService.submit(callable);
                return task.get(thresholdMilliseconds, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new InternalServerErrorException("Internal exception ", e.getCause());
            } catch (TimeoutException e) {
                throw new InternalServerErrorException("Execution aborted, exceeded allowed " + thresholdMilliseconds + " threshold");
            }
        };
    }

    private Callable<String> failFastStringPolicy(long thresholdMilliseconds, Callable<String> callable) throws Throwable{
        return ()-> {
            try {
                Future<String> task = executorService.submit(callable);
                return task.get(thresholdMilliseconds, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new InternalServerErrorException("Internal exception ", e.getCause());
            } catch (TimeoutException e) {
                throw new InternalServerErrorException("Execution aborted, exceeded allowed " + thresholdMilliseconds + " threshold");
            }
        };
    }

    private CircuitBreaker breaker(){
        return new CircuitBreaker();
    }

    private String getServerID() throws Throwable{
        return Failsafe
                .with(breaker())
                .withFallback("UNKNOWN")
                .get(failFastStringPolicy(LOCAL_FS_TIMEOUT, () -> {
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

    private String getClusterID() throws Throwable{
        return Failsafe
                .with(breaker())
                .withFallback("UNKNOWN")
                .get(failFastStringPolicy(DB_TIMEOUT, () -> {
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