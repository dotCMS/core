package com.dotcms.rest.api.v2.languages;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Language end point
 */
@Path("/v2/languages")
public class LanguagesResource {

    private final LanguageAPI languageAPI;
    private final WebResource webResource;
    private final com.dotcms.rest.api.v1.languages.LanguagesResource oldLanguagesResource;

    public LanguagesResource() {
        this(APILocator.getLanguageAPI(),
                new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    public LanguagesResource(final LanguageAPI languageAPI,
                                final WebResource webResource) {

        this.languageAPI  = languageAPI;
        this.webResource  = webResource;
        this.oldLanguagesResource = new com.dotcms.rest.api.v1.languages.LanguagesResource();
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    /**
     * return a array with all the languages
     */
    public Response  list(@Context final HttpServletRequest request) {
        Logger.debug(this, String.format("listing languages %s", request.getRequestURI()));

        webResource.init(true, request, true);
        return Response.ok(new ResponseEntityView(ImmutableList.copyOf(languageAPI.getLanguages()))).build();
    }

    @POST
    @JSONP
    @NoCache
    @Path("/i18n")
    @InitRequestRequired
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getMessages(@Context HttpServletRequest request,
                                final I18NForm i18NForm) {
        return oldLanguagesResource.getMessages(request, i18NForm);
    }
}
