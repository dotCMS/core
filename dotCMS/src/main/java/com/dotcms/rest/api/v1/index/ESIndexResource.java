package com.dotcms.rest.api.v1.index;

import com.dotcms.content.elasticsearch.util.ESMappingUtilHelper;
import com.liferay.portal.language.LanguageUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ClusterStats;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexHelper;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.business.NodeStats;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResourceResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.reindex.IndexResourceHelper;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Index endpoint for REST calls version 1
 *
 */
@Path("/v1/esindex")
@Tag(name = "Search Index", description = "Elasticsearch index management and operations")
public class ESIndexResource {

    private enum IndexAction{
        ACTIVATE,
        DEACTIVATE,
        CLEAR,
        OPEN,
        CLOSE;
        
        public static IndexAction fromString(String actionStr) {
            for(IndexAction action: IndexAction.values()) {
                if(action.name().equalsIgnoreCase(actionStr)) {
                    return action;
                }
                
            }
            return ACTIVATE;
        }
    }
    
    
    
    
    
    
    
	private final ESIndexAPI indexAPI;
	private final ESIndexHelper indexHelper;
    private final ContentletIndexAPI idxApi;


	public ESIndexResource(){
		this.indexAPI = APILocator.getESIndexAPI();
		this.indexHelper = ESIndexHelper.getInstance();
	    this.idxApi = APILocator.getContentletIndexAPI();


	}

	@VisibleForTesting
	ESIndexResource(ESIndexAPI indexAPI, ESIndexHelper indexHelper,
			WebResource webResource, LayoutAPI layoutAPI, IndiciesAPI indiciesAPI) {
		this.indexAPI = indexAPI;
		this.indexHelper = indexHelper;
        this.idxApi = APILocator.getContentletIndexAPI();

	}

    protected InitDataObject auth(
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) {

        return auth(request, response, null);
    }

    protected InitDataObject auth(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String params) {

        return new WebResource
                .InitBuilder(request, response)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .params(params)
                .init();
    }
    
    
    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/cluster")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getClusterStats(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
                    throws DotDataException {

        auth(request, response);


        final ESIndexAPI esIndexAPI = new ESIndexAPI();
        final ClusterStats clusterStats = esIndexAPI.getClusterStats();

        Builder<String, Object> builder =
                        ImmutableMap.<String, Object>builder().put("clusterName", clusterStats.getClusterName());

        for (NodeStats stats : clusterStats.getNodeStats()) {
            builder.put("name", stats.getName())
                .put("master", stats.isMaster())
                .put("host", stats.getHost())
                .put("address", stats.getTransportAddress())
                .put("size", stats.getSize())
                .put("count", stats.getDocCount());
        }
        return Response.ok(new ResponseEntityView(builder.build())).build();
    }
    
    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/failed")
    @Produces({MediaType.APPLICATION_JSON})
    public Response downloadRemainingRecordsAsCsv(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException {
        auth(request, response);
        List<Map<String, Object>> results = new ArrayList<>();
        
        
        
        APILocator.getReindexQueueAPI().getFailedReindexRecords().stream().forEach(row->{
            Map<String, Object> failure = new HashMap<>();
            
            final Contentlet contentlet = Try.of(()->APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(row.getIdentToIndex()))
                            .onFailure(e->Logger.warn(ESIndexResource.class, e.getMessage(), e))
                            .getOrElse(Contentlet::new);
            Map<String, Object> conMap = Try.of(() -> new ContentletToMapTransformer(contentlet).toMaps().get(0))
                            .onFailure(e->Logger.warn(ESIndexResource.class, e.getMessage(), e))
                            .getOrElse(HashMap::new);

            failure.put("identifier", row.getIdentToIndex());
            failure.put("serverId", row.getServerId());
            failure.put("failureReason", row.getLastResult());
            failure.put("priority", row.getPriority());
            if(contentlet!=null) {
                try {
                    failure.put("title", contentlet.getTitle());
                    failure.put("inode", contentlet.getInode());
                    failure.put("contentlet", conMap);
                }
                catch(Exception e) {
                    Logger.warn(this.getClass(), "unable to map content:" + e, e);
                }
            }
            
            results.add(failure);
        });

        return Response.ok(results).build();
    }

    @CloseDBIfOpened
    @DELETE
    @JSONP
    @NoCache
    @Path("/failed")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteFailedRecords(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
                    throws DotDataException {
        final InitDataObject init = auth(request, response);
        APILocator.getReindexQueueAPI().deleteFailedRecords();
        return Response.ok(new ResponseEntityView(true)).build();
    }

    @CloseDBIfOpened
    @POST
    @JSONP
    @NoCache
    @Path("/optimize")
    @Produces({MediaType.APPLICATION_JSON})
    public Response optimizeIndices(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        final InitDataObject init = auth(request, response);
        final ContentletIndexAPI api = APILocator.getContentletIndexAPI();
        final List<String> indices = api.listDotCMSIndices();
        api.optimize(indices);
        final String message = Try.of(()-> LanguageUtil.get(APILocator.getCompanyAPI().getDefaultCompany(),"message.cmsmaintenance.cache.indexoptimized")).get();
        sendAdminMessage(message, MessageSeverity.INFO,init.getUser(), 0);
        return Response.ok().build();
    }

    @CloseDBIfOpened
    @DELETE
    @JSONP
    @NoCache
    @Path("/cache")
    @Produces({MediaType.APPLICATION_JSON})
    public Response flushIndiciesCache(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        final InitDataObject init = auth(request, response);
        final ContentletIndexAPI api = APILocator.getContentletIndexAPI();
        final List<String> indices = api.listDotCMSIndices();
        final Map<String, Integer> data = APILocator.getESIndexAPI().flushCaches(indices);
        String message = Try.of(()->LanguageUtil.get(APILocator.getCompanyAPI().getDefaultCompany(),"maintenance.index.cache.flush.message")).get();
        message=message.replace("{0}", String.valueOf(data.get("successfulShards")));
        message=message.replace("{1}", String.valueOf(data.get("failedShards")));
        sendAdminMessage(message, MessageSeverity.INFO, init.getUser(),5000);
        return Response.ok(new ResponseEntityView(data)).build();

    }


    @Deprecated
    @PUT
    @Path("/create/{params:.*}")
    @Produces("text/plain")
    public Response createIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(httpServletRequest, httpServletResponse);

            int shards=Integer.parseInt(init.getParamsMap().get("shards"));
            boolean live = init.getParamsMap().containsKey("live") ? Boolean.parseBoolean(init.getParamsMap().get("live")) : false;
            String indexName = init.getParamsMap().get("index");

            if(indexName == null)
                indexName=ContentletIndexAPIImpl.timestampFormatter.format(new Date());
            indexName = (live) ? "live_" + indexName : "working_" + indexName;

            APILocator.getContentletIndexAPI().createContentIndex(indexName, shards);
            ESMappingUtilHelper.getInstance().addCustomMapping(indexName);

            return Response.ok(indexName).build();
        } catch (Exception de) {
            Logger.error(this, "Error on createIndex. URI: "+httpServletRequest.getRequestURI(),de);
            return Response.serverError().build();
        }
    }
    
    /**
     * use modIndex instead, e.g. PUT /api/v1/esindex/{indexName}?action=clear
     * @param httpServletRequest
     * @param httpServletResponse
     * @param params
     * @return
     */
    @Deprecated
    @PUT
    @Path("/clear/{params:.*}")
    public Response clearIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) throws DotDataException, IOException {

        InitDataObject init=auth(httpServletRequest,httpServletResponse);
        String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
        return modIndex(httpServletRequest, httpServletResponse, indexName, IndexAction.CLEAR.name());
    }

    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getReindexationProgress(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException {

        final InitDataObject init = auth(request, response);

        return Response.ok(new ResponseEntityView(ESReindexationProcessStatus.getProcessIndexationMap())).build();

    }
    private static final String DOTALL="DOTALL";
    
    
    @CloseDBIfOpened
    @POST
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response startReindex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @QueryParam("shards") int shards, @DefaultValue(DOTALL) @QueryParam("contentType") String contentType) throws DotDataException, DotSecurityException {
        final InitDataObject init = auth(request, response);
        shards = (shards <= 0) ? Config.getIntProperty("es.index.number_of_shards", 2) : shards;

        System.setProperty("es.index.number_of_shards", String.valueOf(shards));
        Logger.info(this, "Running Contentlet Reindex");

        if(!DOTALL.equals(contentType)) {
            ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType);
            Logger.info(this.getClass(), "Starting reindex of " + type.name());
            APILocator.getContentletIndexAPI().removeContentFromIndexByStructureInode(type.id());
            APILocator.getContentletAPI().refresh(type);
        }
        else {
            APILocator.getContentletAPI().refreshAllContent();
        }

        return getReindexationProgress(request, response);


    }
    
    @CloseDBIfOpened
    @DELETE
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response stopReindexation(@Context final HttpServletRequest request, 
                    @Context final HttpServletResponse response, 
                    @DefaultValue("true") @QueryParam("switch") boolean switchMe)
                    throws DotDataException {
        final InitDataObject init = auth(request, response);
        

        if(switchMe) {
            APILocator.getContentletIndexAPI().stopFullReindexationAndSwitchover();
        }else {
            APILocator.getContentletIndexAPI().stopFullReindexation();
        }
        sendAdminMessage("reindex stopped", MessageSeverity.INFO,init.getUser(), 5000);
        return getReindexationProgress(request, response);
    }
    
    
    @CloseDBIfOpened
    @DELETE
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @PathParam("indexName") final String indexName) throws DotDataException {
        
        final InitDataObject init = auth(request, response);

        if(indexExists(indexName) ){
            return Response.status(404).build();
        }
        
        idxApi.delete(indexName);

        String message = "Index:" + indexName + " deleted";
        
        sendAdminMessage(message, MessageSeverity.INFO,init.getUser(), 5000);
        
        
        return getIndexStatus(request, response);

    }
    /**
     * use modIndex instead, e.g. PUT /api/v1/esindex/{indexName}?action=activate
     * @param httpServletRequest
     * @param httpServletResponse
     * @param params
     * @return
     */
    @Deprecated
    @PUT
    @Path("/activate/{params:.*}")
    public Response activateIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) throws DotDataException, IOException {
        InitDataObject init=auth(httpServletRequest,httpServletResponse);
        String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
        return modIndex(httpServletRequest, httpServletResponse, indexName, IndexAction.ACTIVATE.name());
    }
    
    
    /**
     * use modIndex instead, e.g. PUT /api/v1/esindex/{indexName}?action=deactivate
     * @param httpServletRequest
     * @param httpServletResponse
     * @param params
     * @return
     */
    @Deprecated
    @PUT
    @Path("/deactivate/{params:.*}")
    public Response deactivateIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) throws DotDataException, IOException {
        InitDataObject init=auth(httpServletRequest,httpServletResponse);
        String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
        return modIndex(httpServletRequest, httpServletResponse, indexName, IndexAction.DEACTIVATE.name());
    }

    /**
     * use modIndex instead, e.g. PUT /api/v1/esindex/{indexName}?action=close
     * @param httpServletRequest
     * @param httpServletResponse
     * @param params
     * @return
     */
    @Deprecated
    @PUT
    @Path("/close/{params:.*}")
    public Response closeIndex(@Context HttpServletRequest httpServletRequest,@Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) throws DotDataException, IOException {
        InitDataObject init=auth(httpServletRequest,httpServletResponse);
        String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
        return modIndex(httpServletRequest, httpServletResponse, indexName, IndexAction.CLOSE.name());
    }
    
    /**
     * use modIndex instead, e.g. PUT /api/v1/esindex/{indexName}?action=open
     * @param httpServletRequest
     * @param httpServletResponse
     * @param params
     * @return
     */
    @Deprecated
    @PUT
    @Path("/open/{params:.*}")
    public Response openIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(httpServletRequest, httpServletResponse);
            String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
            APILocator.getESIndexAPI().openIndex(indexName);

            return Response.ok().build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to open index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @Deprecated
    @GET
    @Path("/active/{params:.*}")
    @Produces("text/plain")
    public Response getActive(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init=auth(httpServletRequest,httpServletResponse, params);

            //Creating an utility response object
            ResourceResponse responseResource = new ResourceResponse( init.getParamsMap() );

            return responseResource.response( APILocator.getContentletIndexAPI().getActiveIndexName( init.getParamsMap().get( "type" ) ) );
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to get active index: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }



    @GET
    @Path("/indexlist/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response indexList(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("params") String params) {
        try {
            InitDataObject init = auth(httpServletRequest, httpServletResponse);

            //Creating an utility response object

            return Response.ok(new ResponseEntityView<>(APILocator.getContentletIndexAPI().listDotCMSIndices())).build();
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception trying to list indices: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }
    
    
    /**
     * sends an admin growl message
     * @param message
     * @param severity
     * @param user
     * @param millis
     */
    @CloseDBIfOpened
    private void sendAdminMessage(final String message, final MessageSeverity severity, final User user, final long millis) {


        final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();



        SystemMessage systemMessage = systemMessageBuilder.setMessage(message).setType(MessageType.SIMPLE_MESSAGE)
                        .setSeverity(severity).setLife(millis).create();

        SystemMessageEventUtil.getInstance().pushMessage(systemMessage, ImmutableList.of(user.getUserId()));
    }
    
    @CloseDBIfOpened
    @PUT
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response modIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @PathParam("indexName") final String indexName, @QueryParam("action") final String action) throws DotDataException, IOException {

        final InitDataObject init = auth(request, response);
        final IndexAction indexAction = IndexAction.fromString(action);
        
        if(indexExists(indexName) ){
            return Response.status(404).build();
        }
        
        
        switch(indexAction){
            case DEACTIVATE:
                APILocator.getContentletIndexAPI().deactivateIndex(indexName);
                break;
            case CLEAR:
                APILocator.getESIndexAPI().clearIndex(indexName);
                break;
            case OPEN:
                APILocator.getESIndexAPI().openIndex(indexName);
                break;
            case CLOSE:
                APILocator.getESIndexAPI().closeIndex(indexName);
                break;
            default:
                APILocator.getContentletIndexAPI().activateIndex(indexName);
            
        }
        String message = indexAction.name().toLowerCase() + " " + indexName;
        
        sendAdminMessage(message, MessageSeverity.INFO,init.getUser(), 5000);
        
        
        return getIndexStatus(request, response);

    }
    
    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getIndexStatus(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException  {

        
        final InitDataObject init = auth(request, response);

        return Response.ok(new ResponseEntityView<>(IndexResourceHelper.getInstance().indexStatsList())).build();

    }
    
    private boolean indexExists(final String indexName) {
        return !idxApi.listDotCMSIndices().contains(indexName) && !idxApi.listDotCMSClosedIndices().contains(indexName) ;
    }
}
