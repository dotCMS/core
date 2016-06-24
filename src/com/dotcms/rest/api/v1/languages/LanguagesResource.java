package com.dotcms.rest.api.v1.languages;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Path("/v1/languages")
public class LanguagesResource {

    private final LanguageAPI languageAPI;
    private final WebResource webResource;

    @SuppressWarnings("unused")
    public LanguagesResource() {
        this(APILocator.getLanguageAPI(), new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    protected LanguagesResource(LanguageAPI languageAPI, WebResource webResource) {
        this.languageAPI = languageAPI;
        this.webResource = webResource;
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

}
