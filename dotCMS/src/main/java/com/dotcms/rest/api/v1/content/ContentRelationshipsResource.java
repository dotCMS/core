package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Provides different methods to access information about content relationships in dotCMS. This
 * resource will allow you to pull content using similar syntax as the normal content resource,
 * but will also include the related contents in the payload.
 *
 * @author Jose Castro
 * @version 4.2
 * @since Oct 11, 2017
 * @deprecated This endpoint should be used only when legacy relationships need to be returned.
 * Otherwise use {@link com.dotcms.rest.ContentResource#getContent(HttpServletRequest, HttpServletResponse, String)},
 * which returns all relationships fields in a contentlet when the `depth` param is sent. Possible values for depth:
 *      0 --> The contentlet object will contain the identifiers of the related contentlets
 *      1 --> The contentlet object will contain the related contentlets
 *      2 --> The contentlet object will contain the related contentlets, which in turn will contain the identifiers of their related contentlets
 *      3 --> The contentlet object will contain the related contentlets, which in turn will contain a list of their related contentlets
 *      null --> Relationships will not be sent in the response
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Path("/v1/contentrelationships")
@Tag(name = "Content")
@Deprecated
public class ContentRelationshipsResource {

    private final WebResource webResource;
    private final ContentRelationshipsHelper contentRelationshipsHelper;

    /**
     * Creates an instance of this REST end-point.
     */
    public ContentRelationshipsResource() {
        this(ContentRelationshipsHelper.getInstance(), new WebResource());
    }

    @VisibleForTesting
    protected ContentRelationshipsResource(final ContentRelationshipsHelper
                                                       contentRelationshipsHelper, final
    WebResource webResource) {
        this.contentRelationshipsHelper = contentRelationshipsHelper;
        this.webResource = webResource;
    }

    /**
     * Retrieves content relationships based on the specified query parameters. For example, you
     * can:
     * <br/><br/>
     * Pass a query:
     * <pre>
     * http://localhost:8080/api/contentRelationships/query/+contentType:News%20+
     * (conhost:48190c8c-42c4-46af-8d1a-0cd5db894797%20conhost:SYSTEM_HOST)
     * %20+deleted:false%20+working:true/limit/3/orderby/modDate%20desc
     * </pre>
     * An Identifier:
     * <pre>
     * http://localhost:8080/api/contentRelationships/id/2943b5eb-9105-4dcf-a1c7-87a9d4dc92a6
     * </pre>
     * Or an Inode:
     * <pre>
     * http://localhost:8080/api/contentRelationships/inode/aaee9776-8fb7-4501-8048-844912a20405
     * </pre>
     *
     * @param request  The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param params   A Map of parameters that will define the search criteria.
     * @return The list of associated contents.
     */
    @Operation(
        summary = "Get content with relationships (deprecated)",
        description = "Retrieves content with relationships based on query parameters, identifier, or inode. This endpoint is deprecated - use /v1/content with depth parameter instead."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Content with relationships retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "Content with relationships in JSON format including contentlets and their related content"))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Path("/{params: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContent(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @Parameter(description = "Query parameters, identifier, or inode for content lookup", required = true)
                               @PathParam("params") final String params) {
        final InitDataObject initData = this.webResource.init(params, request, response, false, null);
        final Map<String, String> paramsMap = initData.getParamsMap();
        final User user = initData.getUser();
        Response res = null;
        try {
            final List<Contentlet> cons = this.contentRelationshipsHelper.getRelatedContent
                    (request, user, paramsMap);
            final String relatedContents = this.contentRelationshipsHelper.contentsAsJson(cons);
            final Response.ResponseBuilder builder = Response.ok(relatedContents, MediaType
                    .APPLICATION_JSON);

            res = builder.build();
        } catch (JSONException e) {
            final String errorMsg = "An error occurred when generating the JSON response (" + e
                    .getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        } catch (DotDataException e) {
            final String errorMsg = "An error occurred when accessing the content information ("
                    + e.getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        }
        return res;
    }

}
