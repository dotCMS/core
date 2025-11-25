package com.dotcms.cli.command;

import com.dotcms.api.client.model.AuthenticationParam;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.contenttype.ContentTypeCommand;
import com.dotcms.cli.command.files.FilesCommand;
import com.dotcms.cli.command.language.LanguageCommand;
import com.dotcms.cli.command.site.SiteCommand;
import com.dotcms.cli.common.DirectoryWatcherService;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.VersionProvider;
import com.dotcms.cli.exception.ExceptionHandlerImpl;
import com.dotcms.model.annotation.SecuredPassword;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
        name = EntryCommand.NAME,
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = {
                "@|bold,underline,blue dotCMS|@ dotCLI is a command line interface to interact with your @|bold,underline,blue dotCMS|@ instance.",
        },
        header = "dotCMS dotCLI",
        subcommands = {
                //-- Miscellaneous stuff
                ConfigCommand.class,
                InstanceCommand.class,
                StatusCommand.class,
                LoginCommand.class,
                PushCommand.class,
                PullCommand.class,

                //---- ContentType Related stuff
                ContentTypeCommand.class,
                //--- Site related stuff
                SiteCommand.class,
                //--- Language related stuff
                LanguageCommand.class,
                //--- Files related stuff
                FilesCommand.class
        }
)
public class EntryCommand implements DotCommand {

    public static final String NAME = "dotCLI";

    // Declared here, so we have an instance available via Arc container on the Customized CommandLine
    @Inject
    ExceptionHandlerImpl exceptionHandler;

    @Inject
    AuthenticationParam authenticationParam;

    @SecuredPassword
    @Inject
    ServiceManager serviceManager;

    @Inject
    DirectoryWatcherService directoryWatcherService;

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }
}

@ApplicationScoped
class CustomConfiguration {

    @Inject
    CustomConfigurationUtil customConfigurationUtil;

    /**
     * This method configures and produces a customized Picocli {@code CommandLine} instance.
     *
     * @param factory The {@code PicocliCommandLineFactory} used to create the {@code CommandLine}.
     * @return The customized {@code CommandLine} instance.
     */
    @Produces
    CommandLine customCommandLine(final PicocliCommandLineFactory factory) {

        final CommandLine cmdLine = factory.create();
        customConfigurationUtil
                // Injecting custom push mixins to the global push command
                .injectPushMixins(cmdLine)
                // Injecting custom pull mixins to the global pull command
                .injectPullMixins(cmdLine)
                // Customizing the CommandLine object
                .customize(cmdLine)
        ;

        return cmdLine;
    }

}

