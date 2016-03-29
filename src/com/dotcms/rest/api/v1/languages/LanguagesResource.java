package com.dotcms.rest.api.v1.languages;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.util.List;
import java.util.Map;

@Path("/v1/languages")
public class LanguagesResource {

    private final LanguageAPI languageAPI;

    @SuppressWarnings("unused")
    public LanguagesResource() {
        languageAPI = APILocator.getLanguageAPI();
    }

    @VisibleForTesting
    protected LanguagesResource(LanguageAPI languageAPI) {
        this.languageAPI = languageAPI;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Map<String, RestLanguage> list() {
        List<Language> languages = languageAPI.getLanguages();
        Map<String, RestLanguage> hash = Maps.newHashMapWithExpectedSize(languages.size());
        for (Language language : languages) {
            hash.put(language.getLanguageCode(), new LanguageTransform().appToRest(language));
        }
        return hash;
    }

}
