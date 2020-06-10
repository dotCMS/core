package com.dotmarketing.common.reindex;

import java.io.IOException;
import java.util.ArrayList;
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
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.NodeStats;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.control.Try;

@Path("/v1/index")
public class IndexResource {

    public IndexResource() {


    }


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
    
    
    @CloseDBIfOpened
    @DELETE
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @PathParam("indexName") final String indexName) {

        final InitDataObject init = validateUser(request, response);
        return Response.ok(new ResponseEntityView(APILocator.getContentletIndexAPI().delete(indexName))).build();


    }

    @CloseDBIfOpened
    @PUT
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deactivateIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @PathParam("indexName") final String indexName, @QueryParam("action") final String action) throws DotDataException, IOException {

        final InitDataObject init = validateUser(request, response);
        final IndexAction indexAction = IndexAction.fromString(action);
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

        
        
        return Response.ok(new ResponseEntityView(ESReindexationProcessStatus.getProcessIndexationMap())).build();



    }
    
    
    
    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getReindexationProgress(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException {

        final InitDataObject init = validateUser(request, response);

        return Response.ok(new ResponseEntityView(ESReindexationProcessStatus.getProcessIndexationMap())).build();

    }

    private final String DOTALL="DOTALL";
    
    
    @CloseDBIfOpened
    @POST
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response startReindex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @QueryParam("shards") int shards, @DefaultValue(DOTALL) @QueryParam("contentType") String contentType) throws DotDataException, DotSecurityException {
        final InitDataObject init = validateUser(request, response);
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

        return Response.ok(new ResponseEntityView(ESReindexationProcessStatus.getProcessIndexationMap())).build();


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
        final InitDataObject init = validateUser(request, response);
        
        if(!APILocator.getContentletIndexAPI().isInFullReindex()) {
            return Response.ok(new ResponseEntityView(ESReindexationProcessStatus.getProcessIndexationMap())).build();
        }
        
        if(switchMe) {
            APILocator.getContentletIndexAPI().stopFullReindexationAndSwitchover();
        }else {
            APILocator.getContentletIndexAPI().stopFullReindexation();
        }
        return Response.ok(new ResponseEntityView(ESReindexationProcessStatus.getProcessIndexationMap())).build();
    }

    @CloseDBIfOpened
    @POST
    @JSONP
    @NoCache
    @Path("/clean")
    public Response cleanContentType(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    String inode) throws DotDataException, DotSecurityException, LanguageException {

        final InitDataObject init = validateUser(request, response);
        ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(inode);
        APILocator.getContentletIndexAPI().removeContentFromIndexByStructureInode(type.id());
        APILocator.getContentletAPI().refresh(type);

        String message = LanguageUtil.get("message.cmsmaintenance.cache.indexrebuilt", init.getUser());

        sendAdminMessage(message, MessageSeverity.INFO, 10000);

        return Response.ok(new ResponseEntityView(message)).build();


    }
    @CloseDBIfOpened
    @POST
    @JSONP
    @NoCache
    @Path("/optimize")
    public Response optimizeIndices(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        final InitDataObject init = validateUser(request, response);
        ContentletIndexAPI api = APILocator.getContentletIndexAPI();
        List<String> indices = api.listDotCMSIndices();
        api.optimize(indices);
        String message = Try.of(()->LanguageUtil.get(APILocator.getCompanyAPI().getDefaultCompany(),"message.cmsmaintenance.cache.indexoptimized")).get();


        sendAdminMessage(message, MessageSeverity.INFO, 0);
        return Response.ok().build();
    }

    @CloseDBIfOpened
    @DELETE
    @JSONP
    @NoCache
    @Path("/cache")
    public Response flushIndiciesCache(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        final InitDataObject init = validateUser(request, response);
        ContentletIndexAPI api = APILocator.getContentletIndexAPI();
        List<String> indices = api.listDotCMSIndices();
        
        
        Map<String, Integer> data = APILocator.getESIndexAPI().flushCaches(indices);
        String message = Try.of(()->LanguageUtil.get(APILocator.getCompanyAPI().getDefaultCompany(),"maintenance.index.cache.flush.message")).get();
        
        
        
        
        message=message.replace("{0}", String.valueOf(data.get("successfulShards")));
        message=message.replace("{1}", String.valueOf(data.get("failedShards")));
        
        
        sendAdminMessage(message, MessageSeverity.INFO, 5000);
        
        
        
        return Response.ok(new ResponseEntityView(data)).build();

    }

    private InitDataObject validateUser(final HttpServletRequest request, final HttpServletResponse response) {
        return new WebResource.InitBuilder(request, response).requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .requiredPortlet("maintenance").init();

    }

    @CloseDBIfOpened
    @GET
    @JSONP
    @NoCache
    @Path("/cluster")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getClusterStats(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
                    throws DotDataException {

        validateUser(request, response);


        final ESIndexAPI esIndexAPI = new ESIndexAPI();
        final ClusterStats clusterStats = esIndexAPI.getClusterStats();

        Builder<String, Object> builder =
                        ImmutableMap.<String, Object>builder().put("clusterName", clusterStats.getClusterName());

        for (NodeStats stats : clusterStats.getNodeStats()) {
            builder.put("name", stats.getName()).put("master", stats.isMaster()).put("host", stats.getHost())
                            .put("address", stats.getTransportAddress()).put("size", stats.getSize())
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
        validateUser(request, response);
        List<Map<String, Object>> results = new ArrayList<>();
        
        
        
        APILocator.getReindexQueueAPI().getFailedReindexRecords().stream().forEach(row->{
            Map<String, Object> failure = new HashMap<>();
            
            final Contentlet contentlet = Try.of(()->APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(row.getIdentToIndex()))
                            .onFailure(e->Logger.warn(IndexResource.class, e.getMessage(), e))
                            .getOrElse(Contentlet::new);
            Map<String, Object> conMap = Try.of(() -> new ContentletToMapTransformer(contentlet).toMaps().get(0))
                            .onFailure(e->Logger.warn(IndexResource.class, e.getMessage(), e))
                            .getOrElse(HashMap::new);
            
            failure.put("title", contentlet.getTitle());
            failure.put("inode", contentlet.getInode());
            failure.put("identifier", row.getIdentToIndex());
            failure.put("serverId", row.getServerId());
            failure.put("failureReason", row.getLastResult());
            failure.put("priority", row.getPriority());
            failure.put("contentlet", conMap);
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
        final InitDataObject init = validateUser(request, response);
        APILocator.getReindexQueueAPI().deleteFailedRecords();
        return Response.ok(new ResponseEntityView(true)).build();
    }

    private void sendAdminMessage(String message, MessageSeverity severity, long millis) {
    
    
            final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
    
    
            
            SystemMessage systemMessage = systemMessageBuilder.setMessage(message)
                 .setType(MessageType.SIMPLE_MESSAGE)
                 .setSeverity(severity)
                 .setLife(millis)
                 .create();
             List<String> users = Try.of(()->APILocator.getRoleAPI().findUserIdsForRole(APILocator.getRoleAPI().loadCMSAdminRole())).getOrElse(ImmutableList.of());
             SystemMessageEventUtil.getInstance().pushMessage(systemMessage, users);
        }
       

   
    
    
    
    

}
