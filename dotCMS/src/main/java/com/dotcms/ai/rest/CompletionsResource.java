package com.dotcms.ai.rest;

import com.dotcms.ai.AiKeys;
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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The CompletionsResource class provides REST endpoints for interacting with the AI completions service.
 * It includes methods for generating completions based on a given prompt.
 */
@Path("/v1/ai/completions")
public class CompletionsResource {

    /**
     * Handles POST requests to generate completions based on a given prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param formIn the form data containing the prompt
     * @return a Response object containing the generated completions
     */
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response summarizeFromContent(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               final CompletionsForm formIn) {

        if (formIn.prompt == null) {
            return badRequestResponse();
        }

        final CompletionsForm form = resolveForm(request, response, formIn);
        return fromForm(
                form,
                () -> CompletionsAPI.impl().summarize(form),
                out -> CompletionsAPI.impl().summarizeStream(form, new LineReadingOutputStream(out)));
    }

    /**
     * Handles POST requests to generate completions based on a raw prompt.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param formIn the form data containing the prompt
     * @return a Response object containing the generated completions
     */
    @Path("/rawPrompt")
    @POST
    @JSONP
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response rawPrompt(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response,
                                    final CompletionsForm formIn) {

        if (formIn.prompt == null) {
            return badRequestResponse();
        }
        
        final CompletionsForm form = resolveForm(request, response, formIn);
        return fromForm(
                form,
                () -> CompletionsAPI.impl().raw(form),
                out -> CompletionsAPI.impl().rawStream(form, new LineReadingOutputStream(out)));
    }

    /**
     * Handles GET requests to retrieve the configuration of the completions service.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return a Response object containing the configuration of the completions service
     */
    @GET
    @JSONP
    @Path("/config")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response getConfig(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response) {
        // get user if we have one (this is allow anon)
        new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .init()
                .getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final AppConfig app = ConfigService.INSTANCE.config(host);


        final Map<String, Object> map = new HashMap<>();
        map.put(AiKeys.CONFIG_HOST, host.getHostname() + " (falls back to system host)");
        for (final AppKeys config : AppKeys.values()) {
            map.put(config.key, app.getConfig(config));
        }

        final String apiKey = UtilMethods.isSet(app.getApiKey()) ? "*****" : "NOT SET";
        map.put(AppKeys.API_KEY.key, apiKey);

        final List<String> models = Arrays.stream(OpenAIModel.values())
                .filter(m->m.completionModel)
                .map(m-> m.modelName)
                .collect(Collectors.toList());
        map.put(AiKeys.AVAILABLE_MODELS, models);

        return Response.ok(map).build();
    }

    private static Response badRequestResponse() {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of(AiKeys.ERROR, "query required")).build();
    }

    private static CompletionsForm resolveForm(final HttpServletRequest request,
                                               final HttpServletResponse response,
                                               final CompletionsForm formIn) {
        // get user if we have one (this allows anon)
        final User user = new WebResource
                .InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init()
                .getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        return (!user.isAdmin())
                ? CompletionsForm.copy(formIn).model(ConfigService.INSTANCE.config(host).getModel()).build()
                : formIn;
    }

    private static Response fromForm(final CompletionsForm form,
                                     final Supplier<JSONObject> noStream,
                                     final Consumer<OutputStream> stream) {
        final long startTime = System.currentTimeMillis();

        if (!form.stream) {
            final JSONObject jsonResponse = noStream.get();
            jsonResponse.put(AiKeys.TOTAL_TIME, System.currentTimeMillis() - startTime + "ms");
            return Response.ok(jsonResponse.toString(), MediaType.APPLICATION_JSON).build();
        }

        final StreamingOutput streaming = output -> {
            stream.accept(output);
            output.flush();
            output.close();
        };

        return Response.ok(streaming).build();
    }

}
