package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.api.CompletionRequest;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiModelConfigFactory;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.rest.WebResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * The TextResource class provides REST endpoints for interacting with the AI text generation service.
 * It includes methods for generating text based on a given prompt.
 */
@Path("/v1/ai/text")
@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints")
public class TextResource {

    private final AiModelConfigFactory modelConfigFactory;

    @Inject
    public TextResource(AiModelConfigFactory modelConfigFactory) {
        this.modelConfigFactory = modelConfigFactory;
    }

    /**
     * Handles GET requests to generate text based on a given prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param prompt the prompt to generate text from
     * @return a Response object containing the generated text
     * @throws IOException if an I/O error occurs
     */
    @Path("/generate")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response doGet(@Context final HttpServletRequest request,
                          @Context final HttpServletResponse response,
                          @QueryParam("prompt") String prompt) throws IOException {

        return doPost(request, response, new CompletionsForm.Builder().prompt(prompt).build());
    }

    /**
     * Handles POST requests to generate text based on a given prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param form the form data containing the prompt
     * @return a Response object containing the generated text
     * @throws IOException if an I/O error occurs
     */
    @Path("/generate")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           final CompletionsForm form) throws IOException {

        final User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();
        final CompletionsForm formIn = CompletionsForm.copy(form).user(user).build();

        if (UtilMethods.isEmpty(formIn.prompt)) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "`prompt` is required"))
                    .build();
        }

        final Host site = WebAPILocator.getHostWebAPI().getHost(request);
        final Optional<AiModelConfig> modelConfigOpt = this.modelConfigFactory.getAiModelConfig(site, form.model);

        if(modelConfigOpt.isPresent()){

            Logger.debug(this, ()-> "Using new AI api for the text resource");
            return Response.ok(
                            APILocator.getDotAIAPI()
                                    .getCompletionsAPI()
                                    .raw(toCompletionRequest(form, modelConfigOpt.get()))
                                    .toString())
                    .build();
        }

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        return Response.ok(
                APILocator.getDotAIAPI()
                        .getCompletionsAPI()
                        .raw(generateRequest(formIn, config), user.getUserId())
                        .toString())
                .build();
    }

    private CompletionRequest toCompletionRequest(final CompletionsForm form,
                                                  final AiModelConfig aiModelConfig) {

        final CompletionRequest.Builder builder = CompletionRequest.builder()
                .vendorModelPath(form.model)
                .prompt(form.prompt)
                .temperature(form.temperature);

        // todo: we need to figured out how to set the system prompts per functionality
        return builder.build();
    }

    /**
     * Generates a request for the AI text generation service based on the given form data and configuration.
     *
     * @param form the form data containing the prompt
     * @param config the configuration for the AI text generation service
     * @return a JSONObject representing the request
     */
    private JSONObject generateRequest(final CompletionsForm form, final AppConfig config) {
        final String model = form.model;
        final float temperature = form.temperature;
        final JSONObject request = new JSONObject();
        final JSONArray messages = new JSONArray();

        final String systemPrompt = UtilMethods.isSet(config.getRolePrompt()) ? config.getRolePrompt() : null;
        if (UtilMethods.isSet(systemPrompt)) {
            messages.add(Map.of(AiKeys.ROLE, AiKeys.SYSTEM, AiKeys.CONTENT, systemPrompt));
        }
        messages.add(Map.of(AiKeys.ROLE, AiKeys.USER, AiKeys.CONTENT, form.prompt));

        request.put(AiKeys.MODEL, model);
        request.put(AiKeys.TEMPERATURE, temperature);
        request.put(AiKeys.MESSAGES, messages);

        return request;
    }

}
