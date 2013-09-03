package com.dotcms.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.dts.spell.utils.FileUtils;
import org.elasticsearch.action.admin.indices.status.IndexStatus;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.gson.Gson;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/esindex")
public class ESIndexResource extends WebResource {
    protected InitDataObject auth(String params,HttpServletRequest request) throws DotSecurityException, DotDataException {
        InitDataObject init=init(params,true,request,true);
        if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", init.getUser()))
            throw new DotSecurityException("unauthorized");
        return init;
    }
    
    public static void restoreIndex(final File file, final String alias, String index, final boolean clear) throws DotDataException {
        if(LicenseUtil.getLevel()>=200) {
            if(UtilMethods.isSet(alias)) {
                String indexName=APILocator.getESIndexAPI()
                         .getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices())
                         .get(alias);
                if(UtilMethods.isSet(indexName))
                    index=indexName;
            }
            else if(!UtilMethods.isSet(index)) {
                index=APILocator.getIndiciesAPI().loadIndicies().site_search;
            }
        }
        
        if(UtilMethods.isSet(index)) {
            final String indexToRestore=index;
            new Thread() {
                public void run() {
                    try {
                        if(clear)
                            APILocator.getESIndexAPI().clearIndex(indexToRestore);
                        APILocator.getESIndexAPI().restoreIndex(file, indexToRestore);
                        Logger.info(this, "finished restoring index "+indexToRestore);
                    }
                    catch(Exception ex) {
                        Logger.error(ESIndexResource.class, "Error restoring "+indexToRestore,ex);
                    }
                }
            }.start();
        }
    }
    
    public static String getIndexNameOrAlias(Map<String, String> map,String indexAttr,String aliasAttr) {
        String indexName = map.get(indexAttr);
        String indexAlias = map.get(aliasAttr);
        if(UtilMethods.isSet(indexAlias) && LicenseUtil.getLevel()>=200) {
            String indexName1=APILocator.getESIndexAPI()
                    .getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices())
                    .get(indexAlias);
            if(UtilMethods.isSet(indexName1))
                indexName=indexName1;
        }
        return indexName;
    }
    
    public static File downloadIndex(String indexName) throws DotDataException, IOException {

        if(indexName.equalsIgnoreCase("live") || indexName.equalsIgnoreCase("working")){
            IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
            if(indexName.equalsIgnoreCase("live")){
                indexName = info.live;
            }
            if(indexName.equalsIgnoreCase("working")){
                indexName = info.working;
            }
        }

        return APILocator.getESIndexAPI().backupIndex(indexName);
   
    }
    
    public static String create(String indexName, int shards, boolean live) throws DotIndexException, IOException {
        if(indexName == null)
            indexName=ESContentletIndexAPI.timestampFormatter.format(new Date());
        indexName = (live) ? "live_" + indexName : "working_" + indexName;
        
        APILocator.getContentletIndexAPI().createContentIndex(indexName, shards);
        
        return indexName;
    }
    
    public static void activateIndex(String indexName) throws DotDataException {
        if(indexName.startsWith(SiteSearchAPI.ES_SITE_SEARCH_NAME)){
            APILocator.getSiteSearchAPI().activateIndex(indexName);
        }
        else{
            APILocator.getContentletIndexAPI().activateIndex(indexName);
        }
    }
    
    public static void deactivateIndex(String indexName) throws DotDataException, IOException {
        if(indexName.startsWith(SiteSearchAPI.ES_SITE_SEARCH_NAME)){
            APILocator.getSiteSearchAPI().deactivateIndex(indexName);
        }
        else{
            APILocator.getContentletIndexAPI().deactivateIndex(indexName);
        }
    }
    
    public static long indexDocumentCount(String indexName) {
        ESIndexAPI esapi = APILocator.getESIndexAPI();
        Map<String, IndexStatus> indexInfo = esapi.getIndicesAndStatus();
        IndexStatus status = indexInfo.get(indexName);
        return (status !=null && status.getDocs() != null) ? status.getDocs().getNumDocs(): 0;
    }
    
    @PUT
    @Path("/restore/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response restoreIndex(@Context HttpServletRequest request, @PathParam("params") String params, 
            @FormDataParam("file") InputStream inputFile, @FormDataParam("file") FormDataContentDisposition inputFileDetail) {
        try {
            InitDataObject init=auth(params,request);
            
            String index=init.getParamsMap().get("index");
            String alias=init.getParamsMap().get("alias");
            final boolean clear=init.getParamsMap().containsKey("clear") ? Boolean.parseBoolean(init.getParamsMap().get("clear")) : false;
            
            File file=File.createTempFile("restore", ".json");
            FileUtils.copyStreamToFile(file, inputFile, null);
            
            restoreIndex(file,alias,index,clear);
            
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on restoreIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on restore index. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
        
        return Response.ok().build();
    }
    
    @GET
    @Path("/download/{params:.*}")
    @Produces("application/zip")
    public Response downloadIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            if(!UtilMethods.isSet(indexName)) return Response.status(Status.BAD_REQUEST).build();
            
            File f=downloadIndex(indexName);
            
            return Response.ok(f)
                    .header("Content-Disposition", "attachment; filename="+indexName+".zip")
                    .header("Content-Type", "application/zip").build();
            
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on downloadIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on downloadIndex. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @PUT
    @Path("/create/{params:.*}")
    @Produces("text/plain")
    public Response createIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            
            int shards=Integer.parseInt(init.getParamsMap().get("shards"));
            boolean live = init.getParamsMap().containsKey("live") ? Boolean.parseBoolean(init.getParamsMap().get("live")) : false;
            String indexName = init.getParamsMap().get("index");
            
            indexName = create(indexName, shards, live);
            
            return Response.ok(indexName).build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on createIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on createIndex. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @PUT
    @Path("/clear/{params:.*}")
    public Response clearIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            if(UtilMethods.isSet(indexName)) {
                APILocator.getESIndexAPI().clearIndex(indexName);
            }
            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on clearIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on clearIndex. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @DELETE
    @Path("/{params:.*}")
    public Response deleteIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            if(UtilMethods.isSet(indexName)) {
                APILocator.getESIndexAPI().delete(indexName);
            }
            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on deleteIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on deleteIndex. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @PUT
    @Path("/activate/{params:.*}")
    public Response activateIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            
            activateIndex(indexName);
            
            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on activate from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on activate. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @PUT
    @Path("/deactivate/{params:.*}")
    public Response deactivateIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            
            deactivateIndex(indexName);
            
            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on deactivateIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on deactivateIndex. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @PUT
    @Path("/updatereplica/{params:.*}")
    public Response updateReplica(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            int replicas = Integer.parseInt(init.getParamsMap().get("replicas"));
            APILocator.getESIndexAPI().updateReplicas(indexName, replicas);
            
            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on updateReplica from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on updateReplica. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @PUT
    @Path("/close/{params:.*}")
    public Response closeIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            APILocator.getESIndexAPI().closeIndex(indexName);
            
            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on closeIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on closeIndex. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @PUT
    @Path("/open/{params:.*}")
    public Response openIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);
            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            APILocator.getESIndexAPI().openIndex(indexName);
            
            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on openIndex from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on openIndex. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @GET
    @Path("/active/{params:.*}")
    @Produces("text/plain")
    public Response getActive(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);

            //Creating an utility response object
            ResourceResponse responseResource = new ResourceResponse( init.getParamsMap() );

            return responseResource.response( APILocator.getContentletIndexAPI().getActiveIndexName( init.getParamsMap().get( "type" ) ) );
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on getActive from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on getActive. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @GET
    @Path("/docscount/{params:.*}")
    @Produces("text/plain")
    public Response getDocumentCount(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,request);

            //Creating an utility response object
            ResourceResponse responseResource = new ResourceResponse( init.getParamsMap() );

            String indexName = getIndexNameOrAlias(init.getParamsMap(),"index","alias");
            return responseResource.response( Long.toString( indexDocumentCount( indexName ) ) );
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on getDocumentCount from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on getDocumentCount. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    @GET
    @Path("/indexlist/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response indexList(@Context HttpServletRequest request, @PathParam("params") String params) {
        try {
            InitDataObject init = auth(params,request);

            //Creating an utility response object
            ResourceResponse responseResource = new ResourceResponse( init.getParamsMap() );

            return responseResource.response( new Gson().toJson( APILocator.getContentletIndexAPI().listDotCMSIndices() ) );
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on indexList from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on indexList. URI: "+request.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
}
