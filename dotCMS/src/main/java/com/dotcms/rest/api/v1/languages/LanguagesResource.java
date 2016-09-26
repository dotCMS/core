package com.dotcms.rest.api.v1.languages;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.I18NForm;
import com.dotcms.util.I18NUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Path("/v1/languages")
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

    @VisibleForTesting
    protected LanguagesResource(final LanguageAPI languageAPI,
                                final WebResource webResource,
                                final I18NUtil i18NUtil) {

        this.languageAPI  = languageAPI;
        this.webResource  = webResource;
        this.i18NUtil     = i18NUtil;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Map<String, RestLanguage> list(@Context HttpServletRequest request) {

        webResource.init(true, request, true);
        List<Language> languages = languageAPI.getLanguages();
        Map<String, RestLanguage> hash = Maps.newHashMapWithExpectedSize(languages.size());
        for (Language language : languages) {
            hash.put(language.getLanguageCode(), new LanguageTransform().appToRest(language));
        }
        return hash;
    }

    @POST
    @JSONP
    @NoCache
    @Path("/i18n")
    @InitRequestRequired
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getMessages(@Context HttpServletRequest request,
                                final I18NForm i18NForm) {

        Response res = null;
        final HttpSession session =
                request.getSession();

        try {

            // try to set to the session the locale company settings
            LocaleUtil.processLocaleCompanySettings(request, session);
            // or the locale user cookie configuration if exits.
            LocaleUtil.processLocaleUserCookie(request, session);

            final Map<String, String> messagesMap =
                    this.i18NUtil.getMessagesMap(
                            // if the user set's a switch, it overrides the session too.
                            i18NForm.getCountry(), i18NForm.getLanguage(),
                            i18NForm.getMessagesKey(), request,
                            true); // want to create a session to store the locale.

            res = Response.ok(new ResponseEntityView(null, messagesMap)).build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        return res;
    } // getMessages.

} // E:O:F:LanguagesResource.
