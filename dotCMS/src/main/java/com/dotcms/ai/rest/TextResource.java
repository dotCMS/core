package com.dotcms.ai.rest;

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

@Path("/v1/ai/text")
public class TextResource {

    @Path("/generate")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response doGet(@Context HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("prompt") String prompt) throws IOException {

        return doPost(request, response, new CompletionsForm.Builder().prompt(prompt).build());
    }

    @Path("/generate")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            CompletionsForm formIn) throws IOException {

        new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init().getUser();

        if (UtilMethods.isEmpty(formIn.prompt)) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(Map.of("error", "`prompt` is required"))
                    .build();
        }

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        ConfigService.INSTANCE.config();


        return Response.ok(CompletionsAPI.impl().raw(generateRequest(formIn,config )).toString()).build();

    }

    JSONObject generateRequest(CompletionsForm form, AppConfig config){

        String systemPrompt = UtilMethods.isSet(config.getRolePrompt()) ? config.getRolePrompt() : null;
        String prompt = form.prompt;
        String model = form.model;
        float temperature = form.temperature;
        JSONObject request = new JSONObject();
        JSONArray messages = new JSONArray();
        if(UtilMethods.isSet(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", form.prompt));
        request.put("model", model);
        request.put("temperature", temperature);
        request.put("messages", messages);
        return request;

    }





}
