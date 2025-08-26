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
