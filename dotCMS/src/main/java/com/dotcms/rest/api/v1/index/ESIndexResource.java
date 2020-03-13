package com.dotcms.rest.api.v1.index;

import static com.dotcms.util.DotPreconditions.checkArgument;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexHelper;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.io.ByteStreams;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ResourceResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ServiceUnavailableException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.gson.Gson;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

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
		this.indexHelper = ESIndexHelper.getInstance();
		this.responseUtil = ResponseUtil.INSTANCE;
		this.webResource = new WebResource();
		this.layoutAPI = APILocator.getLayoutAPI();
		this.indiciesAPI = APILocator.getIndiciesAPI();
	}

	@VisibleForTesting
	ESIndexResource(ESIndexAPI indexAPI, ESIndexHelper indexHelper, ResponseUtil responseUtil,
			WebResource webResource, LayoutAPI layoutAPI, IndiciesAPI indiciesAPI) {
		this.indexAPI = indexAPI;
		this.indexHelper = indexHelper;
		this.responseUtil = responseUtil;
		this.webResource = webResource;
		this.layoutAPI = layoutAPI;
		this.indiciesAPI = indiciesAPI;
	}

    protected InitDataObject auth(final String params,
                                  final HttpServletRequest httpServletRequest,
                                  final HttpServletResponse httpServletResponse) throws DotSecurityException, DotDataException {

        final InitDataObject init = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();

        if(!this.layoutAPI.doesUserHaveAccessToPortlet("maintenance", init.getUser())) {
            throw new DotSecurityException("unauthorized");
        }
        return init;
    }

    public static void restoreIndex(final File file, final String alias, String index, final boolean clear) throws DotDataException {
        if(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) {
            if(UtilMethods.isSet(alias)) {
                String indexName=APILocator.getESIndexAPI()
                         .getAliasToIndexMap(APILocator.getSiteSearchAPI().listIndices())
                         .get(alias);
                if(UtilMethods.isSet(indexName))
                    index=indexName;
            }
            else if(!UtilMethods.isSet(index)) {
                index=APILocator.getIndiciesAPI().loadIndicies().getSiteSearch();
            }
        }

        if(UtilMethods.isSet(index)) {
            final String indexToRestore=index;
            new Thread() {
                @CloseDBIfOpened
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

    /**
     * @deprecated Generating a manual index backup is not recommended. Snapshot and restore operations
     * via Elastic Search High Level Rest API should be used instead.
     * For further details: https://www.elastic.co/guide/en/elasticsearch/reference/7.x/modules-snapshots.html
     * @param indexName
     * @return
     * @throws DotDataException
     * @throws IOException
     */
    @Deprecated
    public static File downloadIndex(String indexName) throws DotDataException, IOException {

        if(indexName.equalsIgnoreCase("live") || indexName.equalsIgnoreCase("working")){
            IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
            if(indexName.equalsIgnoreCase("live")){
                indexName = info.getLive();
            }
            if(indexName.equalsIgnoreCase("working")){
                indexName = info.getWorking();
            }
        }

        return APILocator.getESIndexAPI().backupIndex(indexName);

    }

    public static String create(String indexName, int shards, boolean live) throws DotIndexException, IOException {
        if(indexName == null)
            indexName=ContentletIndexAPIImpl.timestampFormatter.format(new Date());
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

    public static long indexDocumentCount(final String indexName) {
        return APILocator.getContentletIndexAPI().getIndexDocumentCount(indexName);
    }

    /**
     * Creates a compressed (zip) index snapshot file.
     *
     * @param httpServletRequest request
     * @param params optional parameters, such as "alias"
     * @return
     * @deprecated Use ES Snapshot via ES REST API instead {@see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.7/modules-snapshots.html">}.
     */
    @GET
    @Path("/snapshot/{params:.*}")
    @Produces({"application/zip", MediaType.APPLICATION_JSON})
    @Deprecated
    public Response snapshotIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {

    	try {
        	checkArgument(params != null);
            InitDataObject initDataObject = auth(params,httpServletRequest, httpServletResponse);
            final User user = initDataObject.getUser();
            String indexName = this.indexHelper.getIndexNameOrAlias(initDataObject.getParamsMap(),this.indexAPI);

            if(!UtilMethods.isSet(indexName)){
            	return this.responseUtil.getErrorResponse(httpServletRequest, Response.Status.BAD_REQUEST, user.getLocale(), null, "snapshot.wrong.arguments");
            }

            if("live".equalsIgnoreCase(indexName) || "working".equalsIgnoreCase(indexName)){
                IndiciesInfo info = this.indiciesAPI.loadIndicies();
                if("live".equalsIgnoreCase(indexName)){
                    indexName = info.getLive();
                }
                if("working".equalsIgnoreCase(indexName)){
                    indexName = info.getWorking();
                }
            }

            final File snapshotFile = this.indexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", indexName);
			final InputStream in = Files.newInputStream(snapshotFile.toPath());
			final StreamingOutput stream = os -> {
                try {
                    ByteStreams.copy(in, os);
                } finally {
                    // clean up
                    indexAPI.deleteRepository(ESIndexAPI.BACKUP_REPOSITORY, false);
                    snapshotFile.delete();
                }
            };
			return Response.ok(stream)
					.header("Content-Disposition", "attachment; filename=\"" + indexName + ".zip\"")
					.header("Content-Type", "application/zip").build();
    	} catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to download index snapshot: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Upload snapshot backup using a zip file
     * @param inputFile file to be uploaded
     * @param inputFileDetail file stream details
     * @param params optional parameters
     * @return request status
     * @deprecated Use ES Snapshot via ES REST API instead {@see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/6.7/modules-snapshots.html">}.
     */
    @POST
    @Path("/restoresnapshot/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Deprecated
    public void restoreSnapshotIndex(@Context final HttpServletRequest httpServletRequest,
                                         @Context final HttpServletResponse httpServletResponse,
                                         @Suspended final AsyncResponse asyncResponse,
                                         @FormDataParam("file") final InputStream inputFile,
                                         @FormDataParam("file") final FormDataContentDisposition inputFileDetail,
                                         @PathParam("params") final String params) {

        try {
        	checkArgument(inputFile != null);
        	InitDataObject initDataObject = auth(params,httpServletRequest, httpServletResponse);
        	final User user = initDataObject.getUser();

        	ResponseUtil.handleAsyncResponse(
        	        () -> {

                        try {

                            if(this.indexAPI.uploadSnapshot(inputFile)){
                                return new MessageEntity(LanguageUtil.get(user.getLocale(),
                                        "snapshot.upload.success"));
                            }

                            throw new ServiceUnavailableException("snapshot.upload.failed");
                        } catch (InterruptedException | LanguageException | ExecutionException | IOException e) {

                            Logger.error(ESIndexResource.class, e.getMessage(), e);
                            throw new DotRuntimeException("snapshot.upload.failed", e);
                        }
                    },
                    (Throwable e) -> {

                        Logger.error(this, "Exception trying to restore index snapshot:" + e.getMessage(), e);
                        return ResponseUtil.mapExceptionResponse(e);
                    },
                    asyncResponse);

        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to restore index snapshot: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    }


    /**
     * @deprecated  As of 2016-10-12, replaced by {@link #snapshotIndex(HttpServletRequest,HttpServletResponse ,String)}
     */
    @PUT
    @Path("/create/{params:.*}")
    @Produces("text/plain")
    public Response createIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest, httpServletResponse);

            int shards=Integer.parseInt(init.getParamsMap().get("shards"));
            boolean live = init.getParamsMap().containsKey("live") ? Boolean.parseBoolean(init.getParamsMap().get("live")) : false;
            String indexName = init.getParamsMap().get("index");

            indexName = create(indexName, shards, live);

            return Response.ok(indexName).build();
        } catch (DotSecurityException sec) {
            SecurityLogger.logInfo(this.getClass(), "Access denied on createIndex from "+httpServletRequest.getRemoteAddr());
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception de) {
            Logger.error(this, "Error on createIndex. URI: "+httpServletRequest.getRequestURI(),de);
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("/clear/{params:.*}")
    public Response clearIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest,httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            if(UtilMethods.isSet(indexName)) {
                APILocator.getESIndexAPI().clearIndex(indexName);
            }
            return Response.ok().build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to clear index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @DELETE
    @Path("/{params:.*}")
    public Response deleteIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest, httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            if(UtilMethods.isSet(indexName)) {
                APILocator.getESIndexAPI().delete(indexName);
            }
            return Response.ok().build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to delete index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @PUT
    @Path("/activate/{params:.*}")
    public Response activateIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest, httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);

            activateIndex(indexName);

            return Response.ok().build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to activate index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @PUT
    @Path("/deactivate/{params:.*}")
    public Response deactivateIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest,httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);

            deactivateIndex(indexName);

            return Response.ok().build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to deactivate index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * @deprecated Use the Update Settings API of Elasticsearch. See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-update-settings.html">Update Settings</a>
     */
    @Deprecated
    @PUT
    @Path("/updatereplica/{params:.*}")
    public Response updateReplica(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse,@PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest, httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            int replicas = Integer.parseInt(init.getParamsMap().get("replicas"));
            APILocator.getESIndexAPI().updateReplicas(indexName, replicas);

            return Response.ok().build();
        } catch(DotDataException dt){
        	Logger.error(this, dt.getMessage());
        	throw new BadRequestException(dt, dt.getMessage());
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to update index replicas: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @PUT
    @Path("/close/{params:.*}")
    public Response closeIndex(@Context HttpServletRequest httpServletRequest,@Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest,httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            APILocator.getESIndexAPI().closeIndex(indexName);

            return Response.ok().build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to open index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @PUT
    @Path("/open/{params:.*}")
    public Response openIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest, httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            APILocator.getESIndexAPI().openIndex(indexName);

            return Response.ok().build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to open index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @GET
    @Path("/active/{params:.*}")
    @Produces("text/plain")
    public Response getActive(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(params,httpServletRequest,httpServletResponse);

            //Creating an utility response object
            ResourceResponse responseResource = new ResourceResponse( init.getParamsMap() );

            return responseResource.response( APILocator.getContentletIndexAPI().getActiveIndexName( init.getParamsMap().get( "type" ) ) );
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to get active index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @GET
    @Path("/docscount/{params:.*}")
    @Produces("text/plain")
    public Response getDocumentCount(@Context HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @PathParam("params") String params)
            throws DotSecurityException {

        InitDataObject init= null;
        try {
            init = auth(params,httpServletRequest, httpServletResponse);

            //Creating an utility response object
            ResourceResponse responseResource = new ResourceResponse( init.getParamsMap() );

            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            return responseResource.response( Long.toString( indexDocumentCount( indexName ) ) );

        } catch (DotDataException e) {
            Logger.error(this.getClass(),"Exception trying to get document count: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @GET
    @Path("/indexlist/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response indexList(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init = auth(params,httpServletRequest, httpServletResponse);

            //Creating an utility response object
            ResourceResponse responseResource = new ResourceResponse( init.getParamsMap() );

            return responseResource.response( new Gson().toJson( APILocator.getContentletIndexAPI().listDotCMSIndices() ) );
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to list indices: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
}
