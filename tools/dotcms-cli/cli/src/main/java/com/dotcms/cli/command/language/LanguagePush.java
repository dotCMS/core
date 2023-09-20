package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.push.PushService;
import com.dotcms.api.client.push.language.LanguageComparator;
import com.dotcms.api.client.push.language.LanguageFetcher;
import com.dotcms.api.client.push.language.LanguagePushHandler;
import com.dotcms.cli.command.DotCommand;
import com.dotcms.cli.command.DotPush;
import com.dotcms.cli.common.FormatOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.language.Language;
import com.dotcms.model.push.PushOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

/**
 * The LanguagePush class represents a command that allows pushing languages to the server. It
 * provides functionality to push a language file or folder path, or create a new language by
 * providing a language ISO code.
 */
@ActivateRequestContext
@CommandLine.Command(
        name = LanguagePush.NAME,
        header = "@|bold,blue Push a language|@",
        description = {
                "This command enables the pushing of languages to the server. It accommodates the "
                        + "specification of either a language file or a folder path. In addition to "
                        + "these options, it also facilitates the creation of a new language through "
                        + "the provision of a language iso code (e.g.: en-us).",
                "" // empty string to add a new line
        }
)
public class LanguagePush extends AbstractLanguageCommand implements Callable<Integer>, DotCommand,
        DotPush {

    static final String NAME = "push";

    static final String LANGUAGES_PUSH_MIXIN = "languagesPushMixin";

    @CommandLine.Mixin
    PushMixin pushMixin;

    @CommandLine.Mixin(name = LANGUAGES_PUSH_MIXIN)
    LanguagesPushMixin languagesPushMixin;

    @CommandLine.Mixin(name = "format")
    FormatOptionMixin formatOption;

    @CommandLine.Option(names = {"--byIso"}, description =
            "Code to be used to create a new language. "
                    + "Used when no file is specified. For example: en-us")
    String languageIso;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    PushService pushService;

    @Inject
    LanguageFetcher languageProvider;

    @Inject
    LanguageComparator languageComparator;

    @Inject
    LanguagePushHandler languagePushHandler;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        // When calling from the global push we should avoid the validation of the unmatched
        // arguments as we may send arguments meant for other push subcommands
        if (!pushMixin.noValidateUnmatchedArguments) {
            // Checking for unmatched arguments
            output.throwIfUnmatchedArguments(spec.commandLine());
        }

        // Make sure the path is within a workspace
        final Optional<Workspace> workspace = workspaceManager.findWorkspace(
                this.getPushMixin().path.toPath());
        if (workspace.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("No valid workspace found at path: [%s]",
                            this.getPushMixin().path.toPath()));
        }

        final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);

        File inputFile = this.getPushMixin().path;

        if (null == inputFile && StringUtils.isEmpty(languageIso)) {
            output.error("You must specify an iso code or file or folder to push a languages.");
            return ExitCode.USAGE;
        }

        if (null != inputFile) {

            if (!inputFile.isAbsolute()) {
                inputFile = Path.of(workspace.get().languages().toString(), inputFile.getName()).toFile();
            }
            if (!inputFile.exists() || !inputFile.canRead()) {
                throw new IOException(String.format(
                        "Unable to access the path [%s] check that it does exist and that you have "
                                + "read permissions on it.", inputFile)
                );
            }

            // To make sure that if the user is passing a directory we use the languages folder
            if (inputFile.isDirectory()) {
                inputFile = workspace.get().languages().toFile();
            }

            // Execute the push
            pushService.push(
                    inputFile,
                    PushOptions.builder().
                            failFast(pushMixin.failFast).
                            allowRemove(languagesPushMixin.removeLanguages).
                            maxRetryAttempts(pushMixin.retryAttempts).
                            dryRun(pushMixin.dryRun).
                            build(),
                    output,
                    languageProvider,
                    languageComparator,
                    languagePushHandler
            );

        } else {
            var responseEntityView = pushLanguageByIsoCode(languageAPI);
            final Language response = responseEntityView.entity();

            final ObjectMapper objectMapper = formatOption.objectMapper();
            output.info(objectMapper.writeValueAsString(response));
        }

        return CommandLine.ExitCode.OK;
    }

    private ResponseEntityView<Language> pushLanguageByIsoCode(final LanguageAPI languageAPI) {

        output.info(String.format("Attempting to create language with iso code @|bold,green [%s]|@",languageIso));

        ResponseEntityView responseEntityView = languageAPI.create(languageIso);

        output.info(String.format("Language with iso code @|bold,green [%s]|@ successfully created.",languageIso));

        return responseEntityView;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    @Override
    public PushMixin getPushMixin() {
        return pushMixin;
    }

    @Override
    public Optional<String> getCustomMixinName() {
        return Optional.of(LANGUAGES_PUSH_MIXIN);
    }

}
