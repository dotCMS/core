package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Map;

/**
 * The TextResource class provides REST endpoints for interacting with the AI text generation service.
 * It includes methods for generating text based on a given prompt.
 */
@Path("/v1/ai/text")
public class TextResource {

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
     * @param formIn the form data containing the prompt
     * @return a Response object containing the generated text
     * @throws IOException if an I/O error occurs
     */
    @Path("/generate")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           final CompletionsForm formIn) throws IOException {

        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();

        if (UtilMethods.isEmpty(formIn.prompt)) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(Map.of(AiKeys.ERROR, "`prompt` is required"))
                    .build();
        }

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        return Response.ok(CompletionsAPI.impl().raw(generateRequest(formIn, config)).toString()).build();
    }

    /**
     * Generates a request for the AI text generation service based on the given form data and configuration.
     *
     * @param form the form data containing the prompt
     * @param config the configuration for the AI text generation service
     * @return a JSONObject representing the request
     */
    private JSONObject generateRequest(final CompletionsForm form, final AppConfig config) {
        final String systemPrompt = UtilMethods.isSet(config.getRolePrompt()) ? config.getRolePrompt() : null;
        final String model = form.model;
        final float temperature = form.temperature;
        final JSONObject request = new JSONObject();
        final JSONArray messages = new JSONArray();

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
