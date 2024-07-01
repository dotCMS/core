package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.language.Language;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguageRemove.NAME,
        aliases = LanguageRemove.ALIAS,
        header = "@|bold,blue Remove a language|@",
        description = {
                " Remove a language given its id or iso (e.g.: en-us)",
                "" // empty string here so we can have a new line
        }
)
/**
 * Command to delete a language given its id or iso (e.g.: en-us)
 * @author nollymar
 */
public class LanguageRemove extends AbstractLanguageCommand implements Callable<Integer>, DotCommand {

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    static final String NAME = "remove";
    static final String ALIAS = "rm";

    @Parameters(index = "0", arity = "1", description = "Language Id or Iso.")
    String languageIdOrIso;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject
    Prompt prompt;

    @Override
    public Integer call() throws Exception {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        final Language language = findExistingLanguage(languageIdOrIso);

        output.info("Attempting to delete the following language:");
        output.info(shortFormat(language));

        if(output.isCliTest() || isDeleteConfirmed()) {
            final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);
            languageAPI.delete(language.id().map(String::valueOf).orElseThrow());
            output.info("Language deleted successfully.");
            return CommandLine.ExitCode.OK;
        }else {
            output.info("Delete operation cancelled.");
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    private boolean isDeleteConfirmed() {
        if(interactiveOption.isInteractive()){
           return prompt.yesOrNo(false,"Are you sure you want to continue ");
        }
        return true;
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
