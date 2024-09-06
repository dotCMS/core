package com.dotcms.cli.command.language;

import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.push.PushService;
import com.dotcms.api.client.push.language.LanguageComparator;
import com.dotcms.api.client.push.language.LanguageFetcher;
import com.dotcms.api.client.push.language.LanguagePushHandler;
import com.dotcms.cli.command.DotPush;
import com.dotcms.cli.common.ApplyCommandOrder;
import com.dotcms.cli.common.FullPushOptionsMixin;
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
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

/**
 * The LanguagePush class represents a command that allows pushing languages to the server. It
 * provides functionality to push a language file or folder path, or create a new language by
 * providing a language ISO code.
 */
@ActivateRequestContext
@CommandLine.Command(
        name = LanguagePush.NAME,
        header = "@|bold,blue Push languages|@",
        description = {
                "This command enables the pushing of languages to the server. It accommodates the "
                        + "specification of either a language file or a folder path. In addition to "
                        + "these options, it also facilitates the creation of a new language through "
                        + "the provision of a language iso code (e.g.: en-us).",
                "" // empty string to add a new line
        }
)
public class LanguagePush extends AbstractLanguageCommand implements Callable<Integer>, DotPush {

    static final String NAME = "push";

    static final String LANGUAGE_PUSH_MIXIN = "languagePushMixin";

    @CommandLine.Mixin
    FullPushOptionsMixin pushMixin;

    @CommandLine.Mixin(name = LANGUAGE_PUSH_MIXIN)
    LanguagePushMixin languagePushMixin;

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

    @Inject
    MapperService mapperService;

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
        final Optional<Workspace> workspace = workspace();
        if (workspace.isEmpty()) {

            var message = "No valid workspace found";
            if (this.getPushMixin().pushPath != null) {
                message = String.format("No valid workspace found at path: [%s]",
                        this.getPushMixin().pushPath.toPath());
            }

            throw new IllegalArgumentException(message);
        }

        File inputFile = this.getPushMixin().path().toFile();
        if (!inputFile.isAbsolute() && inputFile.isFile() ) {
            inputFile = Path.of(workspace.get().languages().toString(), inputFile.getName())
                    .toFile();
        }
        if (!inputFile.exists() || !inputFile.canRead()) {
            throw new IOException(String.format(
                    "Unable to access the path [%s] check that it does exist and that you have "
                            + "read permissions on it.", inputFile)
            );
        }

        if (StringUtils.isEmpty(languageIso)) {

            // To make sure that if the user is passing a directory we use the languages folder
            if (inputFile.isDirectory()) {
                inputFile = workspace.get().languages().toFile();
            }

            // Execute the push
            pushService.push(
                    inputFile,
                    PushOptions.builder().
                            failFast(pushMixin.failFast).
                            allowRemove(languagePushMixin.removeLanguages).
                            disableAutoUpdate(pushMixin.isDisableAutoUpdate()).
                            maxRetryAttempts(pushMixin.retryAttempts).
                            dryRun(pushMixin.dryRun).
                            build(),
                    output,
                    languageProvider,
                    languageComparator,
                    languagePushHandler
            );

        } else {

            final LanguageAPI languageAPI = clientFactory.getClient(LanguageAPI.class);

            // Push the language by its ISO code
            var responseEntityView = pushLanguageByIsoCode(languageAPI);
            final Language response = responseEntityView.entity();

            // Transform the response to JSON and print it
            final ObjectMapper objectMapper = mapperService.objectMapper();
            output.info(objectMapper.writeValueAsString(response));
        }

        return CommandLine.ExitCode.OK;
    }

    /**
     * Pushes a language to the server by its ISO code.
     *
     * @param languageAPI The LanguageAPI object used for creating the language.
     * @return The ResponseEntityView of the created language.
     */
    private ResponseEntityView<Language> pushLanguageByIsoCode(final LanguageAPI languageAPI) {

        output.info(
                String.format("Attempting to create language with iso code @|bold,green [%s]|@",
                        languageIso)
        );

        ResponseEntityView<Language> responseEntityView = languageAPI.create(languageIso);

        output.info(
                String.format("Language with iso code @|bold,green [%s]|@ successfully created.",
                        languageIso)
        );

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
        return Optional.of(LANGUAGE_PUSH_MIXIN);
    }

    @Override
    public int getOrder() {
        return ApplyCommandOrder.LANGUAGE.getOrder();
    }

    @Override
    public WorkspaceManager workspaceManager() {
        return workspaceManager;
    }

    @Override
    public Path workingRootDir() {
        final Optional<Workspace> workspace = workspace();
        if (workspace.isPresent()) {
            return workspace.get().languages();
        }
        throw new IllegalArgumentException("No valid workspace found.");
    }

}
