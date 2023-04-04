package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.common.HelpOption;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.language.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguagePush.NAME,
        description = "@|bold,green Save or update a language given a Language object (in JSON or YML format) or tag (e.g.: en-us)|@"
)
/**
 * Command to push a language given a Language object (in JSON or YML format) or tag (e.g.: en-us)
 * @author nollymar
 */
public class LanguagePush extends AbstractLanguageCommand implements Callable<Integer> {
    static final String NAME = "push";

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Option(names = {"--byTag"}, description = "Tag to be used to create a new language. Used when no file is specified. For example: en-us")
    String languageTag;

    @CommandLine.Option(names = {"-f", "--file"}, description = "The json/yml formatted content-type descriptor file to be pushed. ")
    File file;

    @Inject
    RestClientFactory clientFactory;

    @Override
    public Integer call() throws Exception {
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);

        if (null == file && StringUtils.isEmpty(languageTag)) {
            output.error("You must specify a file or a tag to create a new language.");
            return CommandLine.ExitCode.SOFTWARE;
        }

        final ObjectMapper objectMapper = output.objectMapper();

        ResponseEntityView<Language> responseEntityView;
        if (null != file) {
            if (!file.exists() || !file.canRead()) {
                output.error(String.format(
                        "Unable to read the input file [%s] check that it does exist and that you have read permissions on it.",
                        file.getAbsolutePath()));
                return CommandLine.ExitCode.SOFTWARE;
            }
            try{
                final Language language = objectMapper.readValue(file, Language.class);
                responseEntityView = pushLanguageByFile(languageAPI, language);
            } catch (IOException e) {
                output.error("Unable to parse the input file. Please check that it is a valid JSON or YML file.");
                return CommandLine.ExitCode.SOFTWARE;
            }

        } else {
            responseEntityView = pushLanguageByTag(languageAPI);
        }

        final Language response = responseEntityView.entity();
        output.info(objectMapper.writeValueAsString(response));

        return CommandLine.ExitCode.OK;

    }

    private ResponseEntityView<Language> pushLanguageByFile(final LanguageAPI languageAPI, final Language language) {

        final String languageId = language.id().map(String::valueOf).orElse("");
        final ResponseEntityView<Language> responseEntityView;

        output.info(String.format("Attempting to save language with code @|bold,green [%s]|@",language.languageCode()));

        if (StringUtils.isNotBlank(languageId)){
            output.info(String.format("The id @|bold,green [%s]|@ provided in the language file will be used for look-up.", languageId));
            responseEntityView = languageAPI.update(
                    languageId, Language.builder().from(language).id(Optional.empty()).build());
        } else  {
            output.info("The language file @|bold did not|@ provide a language id. ");
            responseEntityView = languageAPI.create(Language.builder().from(language).id(Optional.empty()).build());
        }

        output.info(String.format("Language with code @|bold,green [%s]|@ successfully pushed.",language.languageCode()));

        return responseEntityView;

    }

    private ResponseEntityView<Language> pushLanguageByTag(final LanguageAPI languageAPI) {

        output.info(String.format("Attempting to create language with tag @|bold,green [%s]|@",languageTag));

        ResponseEntityView responseEntityView = languageAPI.create(languageTag);

        output.info(String.format("Language with tag @|bold,green [%s]|@ successfully created.",languageTag));

        return responseEntityView;
    }

}
