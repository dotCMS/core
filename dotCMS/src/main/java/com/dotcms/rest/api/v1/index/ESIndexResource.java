package com.dotcms.rest.api.v1.index;

import static com.dotcms.util.DotPreconditions.checkArgument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.status.IndexStatus;
import org.elasticsearch.snapshots.SnapshotRestoreException;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexHelper;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.io.ByteStreams;
import com.dotcms.repackage.com.google.gson.Gson;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.WebApplicationException;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.javax.ws.rs.core.StreamingOutput;
import com.dotcms.repackage.org.dts.spell.utils.FileUtils;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataParam;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ResourceResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;

/**
 * Index endpoint for REST calls version 1
 *
 */
@Path("/v1/esindex")
public class ESIndexResource {

	private final ESIndexAPI indexAPI;
	private final ESIndexHelper indexHelper;
	private final ResponseUtil responseUtil;
	private final WebResource webResource;
	private final LayoutAPI layoutAPI;
	private final IndiciesAPI indiciesAPI;

	public ESIndexResource(){
		this.indexAPI = APILocator.getESIndexAPI();
		this.indexHelper = ESIndexHelper.INSTANCE;
		this.responseUtil = ResponseUtil.INSTANCE;
		this.webResource = new WebResource();
		this.layoutAPI = APILocator.getLayoutAPI();
		this.indiciesAPI = APILocator.getIndiciesAPI();
	}

	@VisibleForTesting
	protected ESIndexResource(ESIndexAPI indexAPI, ESIndexHelper indexHelper, ResponseUtil responseUtil,
			WebResource webResource, LayoutAPI layoutAPI, IndiciesAPI indiciesAPI) {
		this.indexAPI = indexAPI;
		this.indexHelper = indexHelper;
		this.responseUtil = responseUtil;
		this.webResource = webResource;
		this.layoutAPI = layoutAPI;
		this.indiciesAPI = indiciesAPI;
	}

    protected InitDataObject auth(String params,HttpServletRequest request) throws DotSecurityException, DotDataException {
        InitDataObject init= webResource.init(params, true, request, true, null);
        if(!this.layoutAPI.doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", init.getUser()))
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
                    }finally {
                        try {
                            HibernateUtil.closeSession();
                        } catch (DotHibernateException e) {
                            Logger.warn(this, e.getMessage(), e);
                        }finally {
                            DbConnectionFactory.closeConnection();
                        }
                    }
                }
            }.start();
        }
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
    	AdminLogger.log(ESIndexResource.class, "activateIndex", "Trying to activate index: " + indexName);

        if(indexName.startsWith(SiteSearchAPI.ES_SITE_SEARCH_NAME)){
            APILocator.getSiteSearchAPI().activateIndex(indexName);
        }
        else{
            APILocator.getContentletIndexAPI().activateIndex(indexName);
        }

        AdminLogger.log(ESIndexResource.class, "activateIndex", "Index activated: " + indexName);
    }

    public static void deactivateIndex(String indexName) throws DotDataException, IOException {
    	AdminLogger.log(ESIndexResource.class, "deactivateIndex", "Trying to deactivate index: " + indexName);

        if(indexName.startsWith(SiteSearchAPI.ES_SITE_SEARCH_NAME)){
            APILocator.getSiteSearchAPI().deactivateIndex(indexName);
        }
        else{
            APILocator.getContentletIndexAPI().deactivateIndex(indexName);
        }

        AdminLogger.log(ESIndexResource.class, "deactivateIndex", "Index deactivated: " + indexName);
    }

    public static long indexDocumentCount(String indexName) {
        ESIndexAPI esapi = APILocator.getESIndexAPI();
        Map<String, IndexStatus> indexInfo = esapi.getIndicesAndStatus();
        IndexStatus status = indexInfo.get(indexName);
        return (status !=null && status.getDocs() != null) ? status.getDocs().getNumDocs(): 0;
    }

    /**
     * Restore index backup
     * @param inputFile input file stream
     * @param inpurFileDetail input file details
     * @param params optional parameters
     * @return response status
     * @deprecated  As of 2016-10-12, replaced by {@link #snapshotIndex(request, inputFile, inputFileDetail ,params)}
     */
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

    /**
     * Download index as backup
     * @param request
     * @param params
     * @return zip file
     * @deprecated  As of 2016-10-12, replaced by {@link #snapshotIndex(request,params)}
     */
    @GET
    @Path("/download/{params:.*}")
    @Produces("application/zip")
    public Response downloadIndex(@Context HttpServletRequest request, @PathParam("params") String params) {

        try {
            InitDataObject init=auth(params,request);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
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

    /**
     * Creates a compressed (zip) index snapshot file
     * @param request request
     * @param params optional parameters, such as "alias"
     * @return
     */
    @GET
    @Path("/snapshot/{params:.*}")
    @Produces("application/zip")
    public Response snapshotIndex(@Context HttpServletRequest request, @PathParam("params") String params) {
    	try {
        	checkArgument(params != null);
        	InitDataObject init = auth(params,request);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),this.indexAPI);
            if(!UtilMethods.isSet(indexName)){
            	return this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, Locale.getDefault(), null, "snapshot.wrong.arguments");
            }

            if("live".equalsIgnoreCase(indexName) || "working".equalsIgnoreCase(indexName)){
                IndiciesInfo info = this.indiciesAPI.loadIndicies();
                if("live".equalsIgnoreCase(indexName)){
                    indexName = info.live;
                }
                if("working".equalsIgnoreCase(indexName)){
                    indexName = info.working;
                }
            }

            final File snapshotFile = this.indexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", indexName);
			InputStream in = new FileInputStream(snapshotFile);
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream os) throws IOException, WebApplicationException {
					try {
						ByteStreams.copy(in, os);
					} finally {
						// clean up
						indexAPI.deleteRepository(ESIndexAPI.BACKUP_REPOSITORY, true);
						snapshotFile.delete();
					}
				}
			};
			return Response.ok(stream)
					.header("Content-Disposition", "attachment; filename=\"" + indexName + ".zip\"")
					.header("Content-Type", "application/zip").build();
    	} catch (SecurityException sec) {
    		Logger.error(this.getClass(), "Access denied", sec);
            return this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, Locale.getDefault(), null, "authentication-failed");
        } catch (ElasticsearchException esx){
            Logger.error(this.getClass(), "Elastic search exception ", esx);
            return this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, Locale.getDefault(), null, "snapshot.wrong.arguments");
        } catch(IllegalArgumentException iar){
        	Logger.error(this.getClass(), "Invalid request", iar);
        	return this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, Locale.getDefault(), null, "snapshot.wrong.arguments");
        } catch (Exception de) {
            Logger.error(this, "Error on snapshot. URI: "+request.getRequestURI(),de);
            return this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, Locale.getDefault(), null, "snapshot.error");
        }
    }

    /**
     * Upload snapshot backup using a zip file
     * @param inputFile file to be uploaded
     * @param inputFileDetail file stream details
     * @param params optional parameters
     * @return request status
     */
    @POST
    @Path("/restoresnapshot/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response snapshotIndex(@Context HttpServletRequest request,
            @FormDataParam("file") InputStream inputFile, @FormDataParam("file") FormDataContentDisposition inputFileDetail, @PathParam("params") String params) {

        try {
        	checkArgument(inputFile != null);
        	InitDataObject init=auth(params,request);
            if(inputFile!=null) {
            	if(this.indexAPI.uploadSnapshot(inputFile)){
            		return Response.ok(new MessageEntity("Success")).build();
            	}
            }
            return this.responseUtil.getErrorResponse(request, Response.Status.SERVICE_UNAVAILABLE, Locale.getDefault(), null, "snapshot.upload.failed");

        }catch (SecurityException sec) {
            return this.responseUtil.getErrorResponse(request, Response.Status.UNAUTHORIZED, Locale.getDefault(), null, "authentication-failed");
        }catch(IllegalArgumentException iar){
        	return this.responseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, Locale.getDefault(), null, "snapshot.wrong.arguments");
        }catch(IOException ex) {
        	return this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, Locale.getDefault(), null, "snapshot.io.writing.fail");
        }catch(ExecutionException exc){
        	return this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, Locale.getDefault(), null, "snapshot.fail.during.execution");
        }catch(SnapshotRestoreException exc){
        	return this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, Locale.getDefault(), null, "snapshot.fail");
        }catch(InterruptedException exi){
        	return this.responseUtil.getErrorResponse(request, Response.Status.SERVICE_UNAVAILABLE, Locale.getDefault(), null, "snapshot.upload.failed");
        }catch(Exception ex){
        	return this.responseUtil.getErrorResponse(request, Response.Status.INTERNAL_SERVER_ERROR, Locale.getDefault(), null, "snapshot.error");
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
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
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
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
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
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);

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
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);

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
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            int replicas = Integer.parseInt(init.getParamsMap().get("replicas"));
            APILocator.getESIndexAPI().updateReplicas(indexName, replicas);

            return Response.ok().build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on updateReplica from "+request.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        }catch(DotDataException dt){
        	Logger.error(this, dt.getMessage());
        	throw new BadRequestException(dt, dt.getMessage());
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
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
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
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
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

            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
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
