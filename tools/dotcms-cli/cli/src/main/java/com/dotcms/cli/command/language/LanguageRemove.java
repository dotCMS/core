package com.dotcms.cli.command.language;

import static com.dotcms.cli.common.Utils.nextFileName;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.language.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.BooleanUtils;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguageRemove.NAME,
        header = "@|bold,blue Remove a language|@",
        description = {
                " Remove a language given its id or tag (e.g.: en-us)",
                "" // empty string here so we can have a new line
        }
)
/**
 * Command to delete a language given its id or tag (e.g.: en-us)
 * @author nollymar
 */
public class LanguageRemove extends AbstractLanguageCommand implements Callable<Integer> {
    static final String NAME = "remove";
    @Parameters(index = "0", arity = "1", description = "Language Id or Tag.")
    String languageIdOrTag;

    @Override
    public Integer call() throws Exception {

        final Optional<Language> result = super.findExistingLanguage(languageIdOrTag);

        if (result.isEmpty()){
            output.error(String.format(
                    "Error occurred while pulling Language Info: [%s].", languageIdOrTag));
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
        return BooleanUtils.toBoolean(
                System.console().readLine("\nAre you sure you want to continue? [y/n]: "));
    }
}
