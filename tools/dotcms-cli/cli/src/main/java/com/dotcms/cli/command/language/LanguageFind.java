package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.language.Language;
import java.util.List;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguageFind.NAME,
        header = "@|bold,blue Get all existing languages|@",
        description = {
                " This command will list all the languages available in the system.",
                " The output will be a list of languages with their respective information.",
                "" // empty line left here on purpose to make room at the end
        }
)
/**
 * Command to list all languages
 * @author nollymar
 */
public class LanguageFind extends AbstractLanguageCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "find";

    @Inject
    RestClientFactory clientFactory;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
        final List<Language> result = languageAPI.list().entity();

        result.forEach(language -> {
            output.info(shortFormat(language));
        });

        return CommandLine.ExitCode.OK;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

}
