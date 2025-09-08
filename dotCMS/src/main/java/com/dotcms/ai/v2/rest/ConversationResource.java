package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.api.ChatRequest;
import com.dotcms.ai.v2.api.ChatResponse;
import com.dotcms.ai.v2.api.ConversationAPI;
import com.dotcms.ai.v2.api.provider.config.ModelConfigFactory;
import com.dotmarketing.util.UtilMethods;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/v2/ai/conversations")
@Tag(name = "AI", description = "AI-powered chat/conversation")
public class ConversationResource {

    private final ConversationAPI conversationAPI;
    private final ModelConfigFactory modelConfigFactory;


    @Inject
    public ConversationResource(final ConversationAPI conversationAPI,
                                final ModelConfigFactory modelConfigFactory) {
        this.conversationAPI = conversationAPI;
        this.modelConfigFactory = modelConfigFactory;
    }

    /**
     * Test
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param chatRequestForm the prompt to generate text from
     * @return a Response object containing the generated text
     * @throws IOException if an I/O error occurs
     */
    // todo: conversation should be a post, may receive a jwt token or generate a new one if the user is properly login
    // the form should have as a must the minimum information
    // optional may be temperature, max token length, model desired, deterministic context (for instance the contentlet id if it is on content editor) or
    // the query if it is on content search, etc, any information that may need if there is any interpolation needed
    // the prompt may have interpolations by using velocity syntax, the velocity context will be hydrated by the velocity usual stuff
    // any context deterministics references from the client and params for tools
    // the conversion will be pass the processed information (a decoration of the httpserver etc) into the PromptRequest object
    // to an API, the API will contains the AIService in charge of using the tools and making the prompt interactions
    // we will have tool for most of the contentlet api + other things.
    // the configuration will be in a Json (loaded by default with the known models and extensible by osgi, the osgi may push the ModelProvider + an aggregation of the json to be merge into the
    // global one, similar idea of the external packages in OSGI, undeploy the plugin may remove the configuration from the json
    // There will be a JWT to encapsulate the conversion id + user id + site id (multi tenant), this will be passed on a header and validated in a similar way it is on the
    // api tokens, the api will expect on the PromptRequest the desegregation of the jwt claims in attributes, the api will be agnostic of the JWT this will be helpful to
    // keep the track about the context window
    @Path("/chat")
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(
            operationId = "chat",
            summary = "The user does a prompt and gets an answer",
            description = "Creates AI-powered content Chat.",
            tags = {"AI"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Completion generated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Object.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Missing or invalid prompt"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ChatResponse chat(@Context final HttpServletRequest request,
                             @Context final HttpServletResponse response,
                             final ChatRequestForm chatRequestForm) throws IOException {

        // todo: here authentication and authorization
        final String prompt = chatRequestForm.getPrompt();
        final String conversationId = UtilMethods.isSet(chatRequestForm.getConversationId())? // todo: create something for this, b/c this will be related to the jwt eventually, by now it works such as this
                chatRequestForm.getConversationId():"1234"; // generates the id conversation
        return this.conversationAPI.chat(new ChatRequest.Builder().conversationId(conversationId)
                        .prompt(prompt)
                        .modelProviderKey(chatRequestForm.getModelProvider())
                        .modelConfig(this.modelConfigFactory.get(chatRequestForm.getModelProvider()))
                .build());
    }
}
