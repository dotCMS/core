package com.dotmarketing.common.reindex;

import java.io.IOException;
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
    
    

    @DELETE
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @PathParam("indexName") final String indexName) {

        validateUser(request, response);
        return Response.ok(new ResponseEntityView(APILocator.getContentletIndexAPI().delete(indexName))).build();


    }


    @PUT
    @JSONP
    @NoCache
    @Path("/{indexName: .*}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deactivateIndex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @PathParam("indexName") final String indexName, @QueryParam("actionStr") final String actionStr) throws DotDataException, IOException {

        validateUser(request, response);
        IndexAction action = IndexAction.fromString(actionStr);
        switch(action){
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
    
    
    
    
    @GET
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getReindexationProgress(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException {

        validateUser(request, response);

        return Response.ok(new ResponseEntityView(ESReindexationProcessStatus.getProcessIndexationMap())).build();

    }

    private final String DOTALL="DOTALL";
    
    
    
    @POST
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response startReindex(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                    @QueryParam("shards") int shards, @DefaultValue(DOTALL) @QueryParam("contentType") String contentType) throws DotDataException, DotSecurityException {
        validateUser(request, response);
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

    @DELETE
    @JSONP
    @NoCache
    @Path("/reindex")
    @Produces({MediaType.APPLICATION_JSON})
    public Response stopReindexation(@Context final HttpServletRequest request, 
                    @Context final HttpServletResponse response, 
                    @DefaultValue("true") @QueryParam("switch") boolean switchMe)
                    throws DotDataException {
        validateUser(request, response);
        
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



        return Response.ok(new ResponseEntityView(message)).build();


    }

    @POST
    @JSONP
    @NoCache
    @Path("/optimize")
    public Response optimizeIndices(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        validateUser(request, response);
        ContentletIndexAPI api = APILocator.getContentletIndexAPI();
        List<String> indices = api.listDotCMSIndices();

        return Response.ok(new ResponseEntityView(api.optimize(indices))).build();
    }

    @DELETE
    @JSONP
    @NoCache
    @Path("/cache")
    public Response flushIndiciesCache(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        validateUser(request, response);
        ContentletIndexAPI api = APILocator.getContentletIndexAPI();
        List<String> indices = api.listDotCMSIndices();
        return Response.ok(new ResponseEntityView(APILocator.getESIndexAPI().flushCaches(indices))).build();

    }

    private InitDataObject validateUser(final HttpServletRequest request, final HttpServletResponse response) {
        return new WebResource.InitBuilder(request, response).requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .requiredPortlet("maintenance").init();

    }


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

    @GET
    @JSONP
    @NoCache
    @Path("/failed")
    @Produces({MediaType.APPLICATION_JSON})
    public Response downloadRemainingRecordsAsCsv(@Context final HttpServletRequest request,
                    @Context final HttpServletResponse response) throws DotDataException {
        validateUser(request, response);
        Map<String, Object> results = new HashMap<>();
        
        
        
        APILocator.getReindexQueueAPI().getFailedReindexRecords().stream().forEach(row->{
            final Contentlet contentlet = Try.of(()->APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(row.getIdentToIndex()))
                            .onFailure(e->Logger.warn(IndexResource.class, e.getMessage(), e))
                            .getOrNull();
            Map<String, Object> conMap = Try.of(() -> new ContentletToMapTransformer(contentlet).toMaps().get(0))
                            .onFailure(e->Logger.warn(IndexResource.class, e.getMessage(), e))
                            .getOrElse(HashMap::new);
            
            conMap.put("TITLE", contentlet.getTitle());
            
            conMap.put("REINDEX_FAILURE", row.getLastResult());
            conMap.put("REINDEX_PRIORITY", row.getPriority());
            results.put(row.getIdentToIndex(), conMap);
        });

        return Response.ok(new ResponseEntityView(results)).build();
    }


    @DELETE
    @JSONP
    @NoCache
    @Path("/failed")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteFailedRecords(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
                    throws DotDataException {
        validateUser(request, response);
        APILocator.getReindexQueueAPI().deleteFailedRecords();
        return Response.ok(new ResponseEntityView(true)).build();
    }


}
