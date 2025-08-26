package com.dotcms.ai.v2.rest;

import com.dotcms.ai.v2.provider.Model;
import com.dotcms.ai.v2.provider.ModelProviderFactory;
import com.dotmarketing.util.Config;
import dev.langchain4j.model.chat.ChatModel;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/v2/ai/conversation")
@Tag(name = "AI", description = "AI-powered chat/conversation")
public class ConversationResource {

    private final ModelProviderFactory modelProviderFactory;

    @Inject
    public ConversationResource(ModelProviderFactory modelProviderFactory) {
        this.modelProviderFactory = modelProviderFactory;
    }

    /**
     * Test
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param prompt the prompt to generate text from
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
    @Path("/test")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String doGet(@Context final HttpServletRequest request,
                          @Context final HttpServletResponse response,
                          @QueryParam("prompt") String prompt) throws IOException {


        final String apiKey = Config.getStringProperty("OPEN_AI_PROMPT_API_KEY");
        final ChatModel chatLanguageModel = this.modelProviderFactory
                .get(Model.OPEN_AI_GPT_40.getProviderName(), Model.OPEN_AI_GPT_40.toConfig(apiKey));

        return chatLanguageModel.chat(prompt);
    }
}
