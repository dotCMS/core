package com.dotcms.api.client.pull.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.model.language.Language;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<Language> fetch(final Map<String, Object> customOptions) {

        final var languageAPI = clientFactory.getClient(LanguageAPI.class);
        return languageAPI.list().entity();
    }

    @ActivateRequestContext
    @Override
    public Language fetchByKey(final String languageIsoOrId,
            final Map<String, Object> customOptions) throws NotFoundException {

        final Language language;
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);

        if (StringUtils.isNumeric(languageIsoOrId)) {
            language = languageAPI.findById(languageIsoOrId).entity();
        } else {
            language = languageAPI.getFromLanguageIsoCode(languageIsoOrId).entity();
        }
        final Optional<Long> id = language.id();
        if (id.isPresent() && id.get() > 0) {
            return language;
        }

        throw new NotFoundException(String.format("Language [%s] Not found.", languageIsoOrId));
    }

}