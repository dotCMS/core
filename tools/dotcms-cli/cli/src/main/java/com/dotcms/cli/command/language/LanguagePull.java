package com.dotcms.cli.command.language;

import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.cli.common.ShortOutputOptionMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.language.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguagePull.NAME,
        header = "@|bold,blue dotCMS Language Pull|@",
        description = {
                " This command pulls a language given its id or iso code (e.g.: en-us).",
                " A language descriptor file will be created in the current directory.",
                " If the language already exists, it will be overwritten.",
                " The file name will be the language's iso code (e.g.: en-us.json).",
                " if a lang is pulled with the same name as an existing one,",
                " the existing one will be overwritten.",
                "" // empty string here so we can have a new line
        }
)
/**
 * Command to pull a language given its id or iso code (e.g.: en-us)
 * @author nollymar
 */
public class LanguagePull extends AbstractLanguageCommand implements Callable<Integer> {
    static final String NAME = "pull";

    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Mixin(name = "shorten")
    ShortOutputOptionMixin shortOutputOption;

    @Inject
    WorkspaceManager workspaceManager;

    @Parameters(index = "0", arity = "1", paramLabel = "idOrIso", description = "Language Id or ISO Code.")
    String languageIdOrIso;

    @Override
    public Integer call() throws Exception {
        try {
            final Optional<Language> result = super.findExistingLanguage(languageIdOrIso);

            if (result.isEmpty()){
                output.error(String.format(
                        "A language with id or ISO code [%s] could not be found.", languageIdOrIso));
                return CommandLine.ExitCode.SOFTWARE;
            }
            final Language language = result.get();
            final ObjectMapper objectMapper = formatOption.objectMapper();

            if(shortOutputOption.isShortOutput()) {
                final String asString = shortFormat(language);
                output.info(asString);
            } else {
                final String asString = objectMapper.writeValueAsString(language);
                output.info(asString);

                final Workspace workspace = workspaceManager.resolve();
                final String fileName = String.format("%s.%s", language.isoCode(), formatOption.getInputOutputFormat().getExtension());
                final Path path = Path.of(workspace.languages().toString(),fileName);

                Files.writeString(path, asString);
                output.info(String.format("Output has been written to file [%s].",path));
            }
        } catch (IOException | NotFoundException e) {
            output.error(String.format(
                    "Error occurred while pulling Language: [%s] with message: [%s:%s].",
                    languageIdOrIso, e.getClass().getSimpleName(),e.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }
}
