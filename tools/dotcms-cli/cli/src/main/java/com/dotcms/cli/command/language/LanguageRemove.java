package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.cli.common.InteractiveOptionMixin;
import com.dotcms.model.language.Language;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import org.apache.commons.lang3.BooleanUtils;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguageRemove.NAME,
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
public class LanguageRemove extends AbstractLanguageCommand implements Callable<Integer> {

    @CommandLine.Mixin
    InteractiveOptionMixin interactiveOption;

    static final String NAME = "remove";
    @Parameters(index = "0", arity = "1", description = "Language Id or Iso.")
    String languageIdOrIso;

    @Override
    public Integer call() throws Exception {

        final Optional<Language> result = super.findExistingLanguage(languageIdOrIso);

        if (result.isEmpty()){
            output.error(String.format(
                    "A language with id or ISO code [%s] could not be found.", languageIdOrIso));
            return CommandLine.ExitCode.SOFTWARE;
        }
        final Language language = result.get();
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
            return BooleanUtils.toBoolean(
                    System.console().readLine("\nAre you sure you want to continue? [y/n]: "));
        }
        return true;
    }
}
