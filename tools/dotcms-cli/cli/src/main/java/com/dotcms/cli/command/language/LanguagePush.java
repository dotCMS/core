package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.language.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = LanguagePush.NAME,
        header = "@|bold,blue Push a language|@",
        description = {
                " Save or update a language given a Language object (in JSON or YML format) or iso (e.g.: en-us)",
                " Push a language given a Language object (in JSON or YML format) or iso (e.g.: en-us)",
                " If no file is specified, a new language will be created using the iso provided.",
                "" // empty string to add a new line
        }
)
/**
 * Command to push a language given a Language object (in JSON or YML format) or iso code (e.g.: en-us)
 * @author nollymar
 */
public class LanguagePush extends AbstractLanguageCommand implements Callable<Integer> {
    static final String NAME = "push";

    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Option(names = {"--byIso"}, description = "Code to be used to create a new language. Used when no file is specified. For example: en-us")
    String languageIso;

    @CommandLine.Option(names = {"-f", "--file"}, description = "The json/yml formatted content-type descriptor file to be pushed. ")
    File file;

    @Override
    public Integer call() throws Exception {
        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);

        if (null == file && StringUtils.isEmpty(languageIso)) {
            output.error("You must specify an iso code or file to create a new language.");
            return CommandLine.ExitCode.SOFTWARE;
        }


        ObjectMapper objectMapper = formatOption.objectMapper(file);

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
            responseEntityView = pushLanguageByIsoCode(languageAPI);
        }

        final Language response = responseEntityView.entity();
        output.info(objectMapper.writeValueAsString(response));

        return CommandLine.ExitCode.OK;

    }



    private ResponseEntityView<Language> pushLanguageByFile(final LanguageAPI languageAPI, final Language language) {

        final String languageId = language.id().map(String::valueOf).orElse("");
        final ResponseEntityView<Language> responseEntityView;

        final String isoCode = language.isoCode();
        language.withLanguageCode(isoCode.split("-")[0]);

        if (isoCode.split("-").length > 1) {
            language.withCountryCode(isoCode.split("-")[1]);
        } else {
            language.withCountryCode("");
        }

        output.info(String.format("Attempting to save language with code @|bold,green [%s]|@",language.languageCode().get()));

        if (StringUtils.isNotBlank(languageId)){
            output.info(String.format("The id @|bold,green [%s]|@ provided in the language file will be used for look-up.", languageId));
            responseEntityView = languageAPI.update(
                    languageId, Language.builder().from(language).id(Optional.empty()).build());
        } else  {
            output.info("The language file @|bold did not|@ provide a language id. ");
            responseEntityView = languageAPI.create(Language.builder().from(language).id(Optional.empty()).build());
        }

        output.info(String.format("Language with code @|bold,green [%s]|@ successfully pushed.",language.languageCode().get()));

        return responseEntityView;

    }

    private ResponseEntityView<Language> pushLanguageByIsoCode(final LanguageAPI languageAPI) {

        output.info(String.format("Attempting to create language with iso code @|bold,green [%s]|@",languageIso));

        ResponseEntityView responseEntityView = languageAPI.create(languageIso);

        output.info(String.format("Language with iso code @|bold,green [%s]|@ successfully created.",languageIso));

        return responseEntityView;
    }

}
