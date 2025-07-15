package com.dotcms.rest.api.v1.index;

import com.dotcms.content.elasticsearch.util.ESMappingUtilHelper;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityListMapView;
import com.dotcms.rest.ResponseEntityListStringView;
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
import com.dotcms.rest.annotation.SwaggerCompliant;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Index endpoint for REST calls version 1
 *
 */
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Path("/v1/esindex")
@Tag(name = "Search Index")
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
    
    
    @Operation(
        summary = "Get Elasticsearch cluster statistics",
        description = "Retrieves comprehensive statistics about the Elasticsearch cluster including cluster name, node information, and performance metrics. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Cluster statistics retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error retrieving cluster statistics",
                    content = @Content(mediaType = "application/json"))
    })
    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/cluster")
    @Produces(MediaType.APPLICATION_JSON)
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
        return Response.ok(new ResponseEntityMapView(builder.build())).build();
    }
    
    @Operation(
        summary = "Download failed reindex records",
        description = "Retrieves a list of failed reindex records in JSON format. Each record includes identifier, server ID, failure reason, and priority information. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Failed reindex records retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error retrieving failed records",
                    content = @Content(mediaType = "application/json"))
    })
    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
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

        return Response.ok(new ResponseEntityListMapView(results)).build();
    }

    @CloseDBIfOpened
    @Operation(
        summary = "Delete failed reindex records",
        description = "Deletes all failed reindex records from the reindex queue. This clears the list of content items that previously failed to reindex. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Failed reindex records deleted successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error deleting failed records",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @JSONP
    @NoCache
    @Path("/failed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFailedRecords(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
                    throws DotDataException {
        final InitDataObject init = auth(request, response);
        APILocator.getReindexQueueAPI().deleteFailedRecords();
        return Response.ok(new ResponseEntityIndexOperationView(true)).build();
    }

    @Operation(
        summary = "Optimize Elasticsearch indices",
        description = "Optimizes all Elasticsearch indices to improve search performance by reducing the number of segments and freeing up disk space. This operation can be resource-intensive. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Index optimization completed successfully (no body)"),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during index optimization",
                    content = @Content(mediaType = "application/json"))
    })
    @CloseDBIfOpened
    @POST
    @JSONP
    @NoCache
    @Path("/optimize")
    @Produces(MediaType.APPLICATION_JSON)
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
    @Operation(
        summary = "Flush Elasticsearch indices cache",
        description = "Flushes the cache for all dotCMS Elasticsearch indices to ensure data consistency. Returns the number of successful and failed shard operations. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Index cache flushed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityIndexOperationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during cache flush",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @JSONP
    @NoCache
    @Path("/cache")
    @Produces(MediaType.APPLICATION_JSON)
    public Response flushIndiciesCache(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        final InitDataObject init = auth(request, response);
        final ContentletIndexAPI api = APILocator.getContentletIndexAPI();
        final List<String> indices = api.listDotCMSIndices();
        final Map<String, Integer> data = APILocator.getESIndexAPI().flushCaches(indices);
        String message = Try.of(()->LanguageUtil.get(APILocator.getCompanyAPI().getDefaultCompany(),"maintenance.index.cache.flush.message")).get();
        message=message.replace("{0}", String.valueOf(data.get("successfulShards")));
        message=message.replace("{1}", String.valueOf(data.get("failedShards")));
        sendAdminMessage(message, MessageSeverity.INFO, init.getUser(),5000);
        return Response.ok(new ResponseEntityIndexOperationView(data)).build();

    }


    @Deprecated
    @Operation(
        summary = "Create index (deprecated)",
        description = "Creates a new Elasticsearch index with specified parameters. This endpoint is deprecated - use the modern reindexing endpoints instead. Requires CMS Administrator role.",
        deprecated = true
    )
    @PUT
    @Path("/create/{params:.*}")
    @Produces("text/plain")
    public Response createIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Index creation parameters") @PathParam("params") String params) {
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
    @Operation(
        summary = "Clear index (deprecated)",
        description = "Clears an Elasticsearch index by name. This endpoint is deprecated - use PUT /api/v1/esindex/{indexName}?action=clear instead. Requires CMS Administrator role.",
        deprecated = true
    )
    @PUT
    @Path("/clear/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Index clearing parameters") @PathParam("params") String params) throws DotDataException, IOException {

        InitDataObject init=auth(httpServletRequest,httpServletResponse);
        String indexName = this.indexHelper.getIndexNameOrAlias(init.getParamsMap(),"index","alias",this.indexAPI);
        return modIndex(httpServletRequest, httpServletResponse, indexName, IndexAction.CLEAR.name());
    }

    @CloseDBIfOpened
    @Operation(
        summary = "Get reindexation progress",
        description = "Retrieves the current progress status of content reindexation process including percentage completed, records processed, and estimated time remaining. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Reindexation progress retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityIndexOperationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error retrieving reindexation progress",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReindexationProgress(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException {

        final InitDataObject init = auth(request, response);

        return Response.ok(new ResponseEntityIndexOperationView(ESReindexationProcessStatus.getProcessIndexationMap())).build();

    }
    private static final String DOTALL="DOTALL";
    
    
    @CloseDBIfOpened
    @Operation(
        summary = "Start content reindexation",
        description = "Initiates a content reindexation process for all content or a specific content type. Optionally allows configuration of the number of Elasticsearch shards. This is a resource-intensive operation that rebuilds the search index. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Reindexation started successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityIndexOperationView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid content type or shard count",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error starting reindexation",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startReindex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @Parameter(description = "Number of Elasticsearch shards (defaults to configuration value if 0 or negative)", required = false) @QueryParam("shards") int shards, 
                    @Parameter(description = "Content type to reindex (defaults to 'DOTALL' for all content)", required = false) @DefaultValue(DOTALL) @QueryParam("contentType") String contentType) throws DotDataException, DotSecurityException {
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
    @Operation(
        summary = "Stop content reindexation",
        description = "Stops the currently running content reindexation process. Optionally performs a switchover to activate the new index after stopping. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Reindexation stopped successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityIndexOperationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error stopping reindexation",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopReindexation(@Context final HttpServletRequest request, 
                    @Context final HttpServletResponse response, 
                    @Parameter(description = "Whether to perform switchover to activate new index after stopping (defaults to true)", required = false) @DefaultValue("true") @QueryParam("switch") boolean switchMe)
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
    @Operation(
        summary = "Delete specific Elasticsearch index",
        description = "Deletes a specific Elasticsearch index by name. This is a destructive operation that permanently removes the index and all its data. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Index deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityIndexOperationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Index not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error deleting index",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @Parameter(description = "Name of the Elasticsearch index to delete", required = true) @PathParam("indexName") final String indexName) throws DotDataException {
        
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
    @Operation(
        summary = "Activate index (deprecated)",
        description = "Activates an Elasticsearch index by name. This endpoint is deprecated - use PUT /api/v1/esindex/{indexName}?action=activate instead. Requires CMS Administrator role.",
        deprecated = true
    )
    @PUT
    @Path("/activate/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Index activation parameters") @PathParam("params") String params) throws DotDataException, IOException {
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
    @Operation(
        summary = "Deactivate index (deprecated)",
        description = "Deactivates an Elasticsearch index by name. This endpoint is deprecated - use PUT /api/v1/esindex/{indexName}?action=deactivate instead. Requires CMS Administrator role.",
        deprecated = true
    )
    @PUT
    @Path("/deactivate/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deactivateIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Index deactivation parameters") @PathParam("params") String params) throws DotDataException, IOException {
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
    @Operation(
        summary = "Close index (deprecated)",
        description = "Closes an Elasticsearch index by name. This endpoint is deprecated - use PUT /api/v1/esindex/{indexName}?action=close instead. Requires CMS Administrator role.",
        deprecated = true
    )
    @PUT
    @Path("/close/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response closeIndex(@Context HttpServletRequest httpServletRequest,@Context final HttpServletResponse httpServletResponse, @Parameter(description = "Index closing parameters") @PathParam("params") String params) throws DotDataException, IOException {
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
    @Operation(
        summary = "Open index (deprecated)",
        description = "Opens an Elasticsearch index by name. This endpoint is deprecated - use PUT /api/v1/esindex/{indexName}?action=open instead. Requires CMS Administrator role.",
        deprecated = true
    )
    @PUT
    @Path("/open/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response openIndex(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Index opening parameters") @PathParam("params") String params) {
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
    @Operation(
        summary = "Get active index (deprecated)",
        description = "Retrieves the name of the active Elasticsearch index for a given type. This endpoint is deprecated - use the modern index status endpoints instead. Requires CMS Administrator role.",
        deprecated = true
    )
    @GET
    @Path("/active/{params:.*}")
    @Produces("text/plain")
    public Response getActive(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Active index query parameters") @PathParam("params") String params) {
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



    @Operation(
        summary = "List Elasticsearch indices",
        description = "Retrieves a list of all dotCMS Elasticsearch indices. Requires CMS Administrator role."
    )
    @GET
    @Path("/indexlist/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response indexList(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Index list query parameters") @PathParam("params") String params) {
        try {
            InitDataObject init = auth(httpServletRequest, httpServletResponse);

            //Creating an utility response object

            return Response.ok(new ResponseEntityListStringView(APILocator.getContentletIndexAPI().listDotCMSIndices())).build();
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
    @Operation(
        summary = "Modify Elasticsearch index",
        description = "Performs various operations on a specific Elasticsearch index including activate, deactivate, clear, open, and close actions. This replaces several deprecated endpoints. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Index operation completed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityIndexOperationView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid action parameter",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Index not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during index operation",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response modIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @Parameter(description = "Name of the Elasticsearch index to modify", required = true) @PathParam("indexName") final String indexName, 
                    @Parameter(description = "Action to perform: ACTIVATE, DEACTIVATE, CLEAR, OPEN, CLOSE", required = true) @QueryParam("action") final String action) throws DotDataException, IOException {

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
    @Operation(
        summary = "Get Elasticsearch index status",
        description = "Retrieves the status information for all Elasticsearch indices including their state, document count, and health information. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Index status retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error retrieving index status",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIndexStatus(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException  {

        
        final InitDataObject init = auth(request, response);

        return Response.ok(new ResponseEntityListMapView(IndexResourceHelper.getInstance().indexStatsList())).build();

    }
    
    private boolean indexExists(final String indexName) {
        return !idxApi.listDotCMSIndices().contains(indexName) && !idxApi.listDotCMSClosedIndices().contains(indexName) ;
    }
}
