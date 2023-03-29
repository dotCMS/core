package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.language.Language;
import java.util.List;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguageFind.NAME,
        description = "@|bold,green Get all existing languages|@"
)
/**
 * Command to list all languages
 * @author nollymar
 */
public class LanguageFind extends AbstractLanguageCommand implements Callable<Integer> {

    static final String NAME = "find";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @Override
    public Integer call() throws Exception {
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final List<Language> result = languageAPI.list().entity();

        result.forEach(language -> {
            output.info(shortFormat(language));
        });

        return CommandLine.ExitCode.OK;
    }
}
