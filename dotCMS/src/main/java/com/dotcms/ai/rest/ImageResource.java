package com.dotcms.ai.rest;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotcms.ai.service.OpenAIImageService;
import com.dotcms.ai.service.OpenAIImageServiceImpl;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Map;


@Path("/v1/ai/image")
public class ImageResource {


    @GET
    @JSONP
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        Response.ResponseBuilder builder = Response.ok(Map.of("type", "image"), MediaType.APPLICATION_JSON);
        return builder.build();
    }

    @GET
    @JSONP
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response indexByInode(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("prompt") String prompt) throws IOException {
        AIImageRequestDTO.Builder dto = new AIImageRequestDTO.Builder();
        dto.prompt(prompt);
        return handleImageRequest(request, response, dto.build());
    }


    /**
     *
     * @param request
     * @param response
     * @param aiImageRequestDTO
     * @return
     * @throws IOException
     */
    @POST
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleImageRequest(@Context HttpServletRequest request,
            @Context HttpServletResponse response,
            AIImageRequestDTO aiImageRequestDTO) throws IOException {

        User user = new WebResource.InitBuilder(request, response)
                .requiredBackendUser(true)
                .requiredFrontendUser(true)
                .init().getUser();

        Logger.debug(this.getClass(), String.format(
                "[DotAI API request] : IP address = %s, URL = %s, method = %s, parameters = %s, body = %s",
                request.getRemoteAddr(), request.getRequestURL().toString(), request.getMethod(),
                readParameters(request.getParameterMap()), Marshaller.marshal(aiImageRequestDTO)));

        final AppConfig config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(request));

        if (UtilMethods.isEmpty(config.getApiKey())) {
            return Response
                    .status(Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "App Config missing"))
                    .build();
        }

        if (StringUtils.isBlank(aiImageRequestDTO.getPrompt())) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(Map.of("error", "`prompt` is required"))
                    .build();

        }

        final OpenAIImageService service = new OpenAIImageServiceImpl(
                config,
                user,
                APILocator.getHostAPI(),
                APILocator.getTempFileAPI());
        final JSONObject resp = service.sendRequest(aiImageRequestDTO);

        return Response.ok(Marshaller.marshal(resp)).type(MediaType.APPLICATION_JSON_TYPE).build();

    }

    private String readParameters(Map<String, String[]> parameterMap) {

        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();

            sb.append(paramName).append(":");

            String[] paramValues = entry.getValue();
            for (String paramValue : paramValues) {
                sb.append(paramValue);
            }
        }
        sb.append("]");

        return sb.toString();
    }
}
