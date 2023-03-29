package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.model.language.Language;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author nollymar
 */
public abstract class AbstractLanguageCommand {

    @Inject
    RestClientFactory clientFactory;

    String shortFormat(final Language language) {
        return String.format(
                "language: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] code: [@|bold,underline,green %s|@] country:[@|bold,yellow %s|@] countryCode: [@|bold,yellow %s|@]",
                language.language(),
                language.id(),
                language.languageCode(),
                language.country(),
                language.countryCode()
        );
    }

    /**
     * Find a language by id or language tag
     * @param languageTagOrId
     * @return
     */
    Optional<Language> findExistingLanguage(final String languageTagOrId){
        if (null != languageTagOrId) {
            final Language language;
            final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
            try {
                if (StringUtils.isNumeric(languageTagOrId)) {
                    language = languageAPI.findById(languageTagOrId).entity();
                } else {
                    language = languageAPI.getFromLanguageTag(languageTagOrId).entity();
                }
                return Optional.of(language);
            } catch (NotFoundException nfe){
            // empty
            }
        }
        return Optional.empty();
    }

}
