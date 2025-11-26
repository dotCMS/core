package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.cli.common.AuthenticationMixin;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.language.Language;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
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
    protected AuthenticationMixin authenticationMixin;

    @CommandLine.Mixin
    protected HelpOptionMixin helpOptionMixin;

    @Inject
    protected RestClientFactory clientFactory;

    String shortFormat(final Language language) {
        return String.format(
                "id: [@|bold,underline,cyan %s|@] isoCode: [@|bold,yellow %s|@]",
                language.id().get(),
                language.isoCode()
        );
    }

    /**
     * Find a language by id or language iso code
     * @param languageIsoOrId
     * @return
     */
    Language findExistingLanguage(final String languageIsoOrId) throws NotFoundException {

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
