package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.language.Language;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

/**
 *
 * @author nollymar
 */
public abstract class AbstractLanguageCommand {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOptionMixin;

    @Inject
    protected RestClientFactory clientFactory;

    String shortFormat(final Language language) {
        return String.format(
                "language: [@|bold,underline,blue %s|@] id: [@|bold,underline,cyan %s|@] code: [@|bold,underline,green %s|@] country:[@|bold,yellow %s|@] countryCode: [@|bold,yellow %s|@] isoCode: [@|bold,yellow %s|@]",
                language.language().orElse(""),
                language.id().get(),
                language.languageCode().get(),
                language.country().orElse(""),
                language.countryCode().orElse(""),
                language.isoCode()

        );
    }

    /**
     * Find a language by id or language iso code
     * @param languageIsoOrId
     * @return
     */
    Optional<Language> findExistingLanguage(final String languageIsoOrId){
        if (null != languageIsoOrId) {
            final Language language;
            final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
            try {
                if (StringUtils.isNumeric(languageIsoOrId)) {
                    language = languageAPI.findById(languageIsoOrId).entity();
                } else {
                    language = languageAPI.getFromLanguageIsoCode(languageIsoOrId).entity();
                }

                if (language.id().isPresent() && language.id().get() > 0) {
                    return Optional.of(language);
                }
            } catch (NotFoundException nfe){
                output.error("Language not found: " + languageIsoOrId);
            }
        }
        return Optional.empty();
    }

}
