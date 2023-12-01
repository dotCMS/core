package com.dotcms.api.client.push.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.ContentFetcher;
import com.dotcms.model.language.Language;
import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

@Dependent
public class LanguageFetcher implements ContentFetcher<Language> {

    @Inject
    protected RestClientFactory clientFactory;

    @ActivateRequestContext
    @Override
    public List<Language> fetch() {
        var languageAPI = clientFactory.getClient(LanguageAPI.class);
        return languageAPI.list().entity();
    }

}
