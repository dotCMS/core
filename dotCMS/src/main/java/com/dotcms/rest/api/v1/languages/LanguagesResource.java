package com.dotcms.rest.api.v1.languages;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.I18NForm;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.util.LocaleUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

@SwaggerCompliant(value = "Site architecture and template management APIs", batch = 3)
@Path("/v1/languages")
@Tag(name = "Internationalization")
public class LanguagesResource {

    private final LanguageAPI languageAPI;
    private final WebResource webResource;
    private final I18NUtil i18NUtil;

    @SuppressWarnings("unused")
    public LanguagesResource() {
        this(APILocator.getLanguageAPI(),
                new WebResource(new ApiProvider()),
                I18NUtil.INSTANCE);
    }


    public LanguagesResource(final LanguageAPI languageAPI,
                                final WebResource webResource,
                                final I18NUtil i18NUtil) {

        this.languageAPI  = languageAPI;
        this.webResource  = webResource;
        this.i18NUtil     = i18NUtil;
    }

    @Operation(
        summary = "List all languages (deprecated)",
        description = "Returns a map of all available languages in the system. This method is deprecated - use v2 LanguagesResource instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Languages retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = MapStringRestLanguageView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, RestLanguage> list(@Context HttpServletRequest request, @Context final HttpServletResponse response) {

        webResource.init(request, response, true);
        List<Language> languages = languageAPI.getLanguages();
        Map<String, RestLanguage> hash = Maps.newHashMapWithExpectedSize(languages.size());
        for (Language language : languages) {
            hash.put(language.getLanguageCode(), new LanguageTransform().appToRest(language));
        }
        return hash;
    }

    @Operation(
        summary = "Get internationalization messages (deprecated)",
        description = "Retrieves internationalization messages for specified language and country. Processes custom locale and stores in session. This method is deprecated - use v2 LanguagesResource instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Messages retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityLanguageMessagesView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid language or country parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Path("/i18n")
    @InitRequestRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessages(@Context HttpServletRequest request,
                                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                    description = "Internationalization form with language, country, and message keys", 
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = I18NForm.class))
                                ) final I18NForm i18NForm) {

        Response res = null;
        final HttpSession session =
                request.getSession();

        try {

            //Trying to find out and process the locale to use
            LocaleUtil.processCustomLocale(request, session);

            final Map<String, String> messagesMap =
                    this.i18NUtil.getMessagesMap(
                            // if the user set's a switch, it overrides the session too.
                            i18NForm.getCountry(), i18NForm.getLanguage(),
                            i18NForm.getMessagesKey(), request,
                            true); // want to create a session to store the locale.

            res = Response.ok(new ResponseEntityView<>(null, messagesMap)).build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        return res;
    } // getI18nmessages.

} // E:O:F:LanguagesResource.
