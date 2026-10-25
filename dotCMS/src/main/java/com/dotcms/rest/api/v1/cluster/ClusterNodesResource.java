package com.dotcms.rest.api.v1.cluster;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.util.PortletID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * Cluster status for the Network portlet: the servers of the cluster, each with a status computed
 * on the backend and typed cache, search and assets details (#37875).
 *
 * <p>The legacy {@code GET /api/cluster/getNodesStatus/} stays as it is for the old Configuration →
 * Network tab.</p>
 *
 * @author hassandotcms
 */
@Path("/v1/cluster")
@Tag(name = "Cluster Management")
public class ClusterNodesResource {

    private final WebResource webResource;
    private final ClusterNodesHelper helper;

    /** Creates the resource with a new {@link WebResource} and the default {@link ClusterNodesHelper}. */
    public ClusterNodesResource() {
        this(new WebResource(), new ClusterNodesHelper());
    }

    /**
     * Creates the resource with the given collaborators.
     *
     * @param webResource authenticates the request and checks access
     * @param helper      builds the list of cluster nodes
     */
    ClusterNodesResource(final WebResource webResource, final ClusterNodesHelper helper) {
        this.webResource = webResource;
        this.helper = helper;
    }

    /**
     * Lists the servers of the cluster. Requires an authenticated back-end user with the
     * {@code configuration} or {@code network-beta} portlet, or the CMS Administrator role.
     *
     * @param request  the current request
     * @param response the current response
     * @return the servers of the cluster
     */
    @Operation(
            operationId = "getClusterNodes",
            summary = "List the cluster's servers with their status and cache, search and assets details",
            description = "Lists the servers with a recent heartbeat, in database order, and asks every server "
                    + "for a status report with one cluster request. Each server has a status computed on the "
                    + "backend: UP when it answered and its heartbeat is fresh, LAGGING when only one of the two "
                    + "holds, DOWN when neither does or when it answered with a failed assets check (the legacy red). Servers that don't answer within the wait window are listed "
                    + "with responded=false and null sections. Requires a back-end user with the configuration or "
                    + "network-beta portlet, or a CMS Administrator. There is no license check."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Servers listed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityClusterNodesView.class))),
            @ApiResponse(responseCode = "401",
                    description = "No authenticated back-end user, or the caller holds neither the configuration "
                            + "nor the network-beta portlet and is not a CMS Administrator",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "The cluster's servers could not be read from the database",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/nodes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getClusterNodes(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString(), PortletID.NETWORK_BETA.toString())
                .init();

        return Response.ok(new ResponseEntityClusterNodesView(helper.nodes())).build();
    }
}
