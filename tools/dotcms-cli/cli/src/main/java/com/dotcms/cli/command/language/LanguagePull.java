package com.dotcms.cli.command.language;

import static com.dotcms.cli.common.Utils.nextFileName;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.language.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.ws.rs.NotFoundException;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguagePull.NAME,
        description = "@|bold,green Get a language given its id or tag (e.g.: en-us)|@"
)
public class LanguagePull extends AbstractLanguageCommand implements Callable<Integer> {
    static final String NAME = "pull";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @Parameters(index = "0", arity = "1", description = "Language Id or Tag.")
    String languageIdOrTag;

    @CommandLine.Option(names = {"-to", "--saveTo"}, order = 5, description = "Save the returned language to a file.")
    File saveAs;

    @Override
    public Integer call() throws Exception {
        try {
            final Optional<Language> result = super.findExistingLanguage(languageIdOrTag);

            if (result.isEmpty()){
                output.error(String.format(
                        "Error occurred while pulling Language Info: [%s].", languageIdOrTag));
                return CommandLine.ExitCode.SOFTWARE;
            }
            final Language language = result.get();
            final ObjectMapper objectMapper = output.objectMapper();

            if(output.isShortenOutput()) {
                final String asString = shortFormat(language);
                output.info(asString);
            } else {
                final String asString = objectMapper.writeValueAsString(language);
                output.info(asString);

                //Saves the pulled language to a file. If the path is given, it will use it, otherwise it will use the language's name
                final Path path;
                if (null != saveAs) {
                    path = saveAs.toPath();
                } else {
                    //But this behavior can be modified if we explicitly add a file name
                    final String fileName = String.format("%s.%s",language.language(),output.getInputOutputFormat().getExtension());
                    final Path next = Path.of(".", fileName);
                    path = nextFileName(next);
                }
                Files.writeString(path, asString);
                output.info(String.format("Output has been written to file [%s].",path));
            }
        } catch (IOException | NotFoundException e) {
            output.error(String.format(
                    "Error occurred while pulling Language: [%s] with message: [%s].",
                    languageIdOrTag, e.getMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }
}
