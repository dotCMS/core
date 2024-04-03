package com.dotcms.ai.rest;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.LineReadingOutputStream;
import com.dotcms.ai.util.OpenAIModel;
import com.dotcms.rest.WebResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Call
 */
@Path("/v1/ai/completions")
public class CompletionsResource {


    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response summarizeFromContent(@Context final HttpServletRequest request, @Context final HttpServletResponse response, CompletionsForm formIn

    ) {

        // get user if we have one (this is allow anon)
        User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();

        if (formIn.prompt == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "query required")).build();
        }
        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);


        final CompletionsForm form = (!user.isAdmin()) ? CompletionsForm.copy(formIn).model(ConfigService.INSTANCE.config(host).getModel()).build() : formIn;


        long startTime = System.currentTimeMillis();


        if (!form.stream) {
            JSONObject jsonResponse = CompletionsAPI.impl().summarize(form);
            jsonResponse.put("totalTime", System.currentTimeMillis() - startTime + "ms");
            return Response.ok(jsonResponse.toString(), MediaType.APPLICATION_JSON).build();
        }

        final StreamingOutput streaming = output -> {
            CompletionsAPI.impl().summarizeStream(form, new LineReadingOutputStream(output));
            output.flush();
            output.close();

        };

        return Response.ok(streaming).build();
    }


    @Path("/rawPrompt")
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response rawPrompt(@Context final HttpServletRequest request, @Context final HttpServletResponse response, CompletionsForm formIn

    ) {

        // get user if we have one (this is allow anon)
        User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();

        if (formIn.prompt == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "query required")).build();
        }

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final CompletionsForm form = (!user.isAdmin()) ? CompletionsForm.copy(formIn).model(ConfigService.INSTANCE.config(host).getModel()).build() : formIn;


        long startTime = System.currentTimeMillis();


        if (!form.stream) {
            JSONObject jsonResponse = CompletionsAPI.impl().raw(form);
            jsonResponse.put("totalTime", System.currentTimeMillis() - startTime + "ms");
            return Response.ok(jsonResponse.toString(), MediaType.APPLICATION_JSON).build();
        }

        final StreamingOutput streaming = output -> {
            CompletionsAPI.impl().rawStream(form, new LineReadingOutputStream(output));
            output.flush();
            output.close();

        };

        return Response.ok(streaming).build();
    }


    @GET
    @JSONP
    @Path("/config")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response getConfig(@Context final HttpServletRequest request, @Context final HttpServletResponse response) {
        // get user if we have one (this is allow anon)
        User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .init()
                .getUser();

        Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        AppConfig app = ConfigService.INSTANCE.config(host);


        Map<String, Object> map = new HashMap<>();
        map.put("configHost", host.getHostname() + " (falls back to system host)");
        for (AppKeys config : AppKeys.values()) {
            String key = config.key;
            map.put(config.key, app.getConfig(config));
        }
        String apiKEYYY = app.getApiKey();

        String apiKey = UtilMethods.isSet(app.getApiKey()) ? "*****" : "NOT SET";
        map.put(AppKeys.API_KEY.key, apiKey);

        List<String> models = Arrays.stream(OpenAIModel.values()).filter(m->m.completionModel).map(m-> m.modelName).collect(Collectors.toList());
        map.put("availableModels", models);
        return Response.ok(map).build();
    }


}
