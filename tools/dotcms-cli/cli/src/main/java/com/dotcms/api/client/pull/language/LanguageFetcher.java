package com.dotcms.api.client.pull.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.model.language.Language;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;

@Dependent
public class LanguageFetcher implements ContentFetcher<Language>, Serializable {

    private static final long serialVersionUID = 2839872958178410528L;

    @Inject
    protected RestClientFactory clientFactory;

    @ActivateRequestContext
    @Override
    public List<Language> fetch(final boolean failFast, final Map<String, Object> customOptions) {

        final var languageAPI = clientFactory.getClient(LanguageAPI.class);
        return languageAPI.listForPull().entity();
    }

    @ActivateRequestContext
    @Override
    public Language fetchByKey(final String languageIsoOrId,
            boolean failFast, final Map<String, Object> customOptions) throws NotFoundException {

        final Language language;
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);

        if (StringUtils.isNumeric(languageIsoOrId)) {
            language = languageAPI.findByIdForPull(languageIsoOrId).entity();
        } else {
            language = languageAPI.getFromLanguageIsoCodeForPull(languageIsoOrId).entity();
        }
        final String isoCode = language.isoCode();
        if (!StringUtils.isEmpty(isoCode)) {
            return language;
        }

        throw new NotFoundException(String.format("Language [%s] Not found.", languageIsoOrId));
    }

}