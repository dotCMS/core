package com.dotcms.ai.rest;

import com.dotcms.ai.client.langchain4j.LangChain4jModelFactory;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Exposes dotAI provider configuration metadata: which providers are available, which
 * capabilities (chat, embeddings, image) each supports, and which {@code providerConfig} fields
 * each capability needs. Backed entirely by {@link LangChain4jModelFactory#listProviderMetadata()}
 * — adding a new provider there makes it appear here automatically, with no REST-layer change.
 */
@Path("/v1/ai/providers")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
public class AiProviderResource {

    /**
     * Lists capability and field metadata for every registered dotAI provider.
     *
     * @param request  the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @return a Response wrapping the list of provider metadata.
     */
    @Operation(
            operationId = "listAiProviders",
            summary = "List dotAI provider configuration metadata",
            description = "Returns, for every registered dotAI provider, the capabilities it "
                    + "supports (chat/embeddings/image) and the providerConfig fields each "
                    + "supported capability requires or accepts."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Provider metadata retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityAiProviderListView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response listProviders(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response) {

        new WebResource.InitBuilder(request, response).requiredBackendUser(true).init();
        return Response.ok(new ResponseEntityAiProviderListView(
                LangChain4jModelFactory.listProviderMetadata())).build();
    }

}
