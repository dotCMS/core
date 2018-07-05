package com.dotcms.rest.api.v1.system.monitor;

import java.io.File;
import java.io.OutputStream;
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

import com.dotcms.repackage.javax.ws.rs.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.WebKeys;
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
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
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
    


    

    final static long LOCAL_FS_TIMEOUT=5000;
    final static long CACHE_TIMEOUT=5000;
    final static long ASSET_FS_TIMEOUT=5000;
    final static long INDEX_TIMEOUT=5000;
    final static long DB_TIMEOUT=5000;
    
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
        System.out.println("********* extendedFormat = |" + extendedFormat + "|");

        try{
            IndiciesInfo idxs=APILocator.getIndiciesAPI().loadIndicies();
            
            
            boolean dotCMSHealthy = false;
            boolean frontendHealthy = false;
            boolean backendHealthy = false;
            boolean dbSelectHealthy = dbCount();
            boolean indexLiveHealthy = indexCount(idxs.live);
            boolean indexWorkingHealthy = indexCount(idxs.working);
            boolean cacheHealthy = cache();
            boolean localFSHealthy = localFiles();
            boolean assetFSHealthy = assetFiles();

            if(dbSelectHealthy && indexLiveHealthy && indexWorkingHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {
                dotCMSHealthy = true;
                frontendHealthy = true;
                backendHealthy = true;
            }
            else if(!indexWorkingHealthy && dbSelectHealthy && indexLiveHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {
                frontendHealthy = true;
            }

            ResponseBuilder builder = null;
            if(extendedFormat) {
                JSONObject jo = new JSONObject();
                jo.put("dotCMSHealthy", dotCMSHealthy);
                jo.put("frontendHealthy", frontendHealthy);
                jo.put("backendHealthy", backendHealthy);
                jo.put("DOTCMS_STARTUP_TIME", System.getProperty(WebKeys.DOTCMS_STARTUP_TIME));

                JSONObject subsystems = new JSONObject();
                subsystems.put("dbSelectHealthy", dbSelectHealthy);
                subsystems.put("indexLiveHealthy", indexLiveHealthy);
                subsystems.put("indexWorkingHealthy", indexWorkingHealthy);
                subsystems.put("cacheHealthy", cacheHealthy);
                subsystems.put("localFSHealthy", localFSHealthy);
                subsystems.put("assetFSHealthy", assetFSHealthy);
                jo.put("subsystems", subsystems);

                builder = Response.ok(jo.toString(2), MediaType.APPLICATION_JSON);
            }
            else {
                if (dotCMSHealthy) {
                    builder = Response.ok("", MediaType.APPLICATION_JSON);
                } else if (!indexWorkingHealthy && dbSelectHealthy && indexLiveHealthy && cacheHealthy && localFSHealthy && assetFSHealthy) {
                    builder = Response.status(507).entity("").type(MediaType.APPLICATION_JSON);
                } else {
                    builder = Response.status(503).entity("").type(MediaType.APPLICATION_JSON);
                }
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
                .get(failFastPolicy(DB_TIMEOUT, () -> { 
                    
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
            .get(failFastPolicy(INDEX_TIMEOUT, () -> {
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
            .get(failFastPolicy(CACHE_TIMEOUT, () -> { 
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
            .get(failFastPolicy(LOCAL_FS_TIMEOUT, () -> { 
    
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
            .get(failFastPolicy(ASSET_FS_TIMEOUT, () -> { 
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
    ExecutorService executorService = Executors.newCachedThreadPool();
    private Callable<Boolean> failFastPolicy(long thresholdMilliseconds, Callable<Boolean> callable) throws Throwable{
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
    private CircuitBreaker breaker(){
        return new CircuitBreaker();
    }
    
}