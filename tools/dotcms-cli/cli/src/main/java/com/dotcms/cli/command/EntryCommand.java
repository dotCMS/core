package com.dotcms.cli.command;

import com.dotcms.api.client.model.AuthenticationParam;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.command.contenttype.ContentTypeCommand;
import com.dotcms.cli.command.files.FilesCommand;
import com.dotcms.cli.command.language.LanguageCommand;
import com.dotcms.cli.command.site.SiteCommand;
import com.dotcms.cli.common.DotExceptionHandler;
import com.dotcms.cli.common.DotExecutionStrategy;
import com.dotcms.cli.common.DotExitCodeExceptionMapper;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.VersionProvider;
import com.dotcms.cli.exception.ExceptionHandlerImpl;
import com.dotcms.model.annotation.SecuredPassword;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

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
public class EntryCommand implements DotCommand{

    public static final String NAME = "dotCLI";

    // Declared here, so we have an instance available via Arc container on the Customized CommandLine
    @Inject
    ExceptionHandlerImpl exceptionHandler;

    @Inject
    AuthenticationParam authenticationParam;

    @SecuredPassword
    @Inject
    ServiceManager serviceManager;

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

    /**
     * This method configures and produces a customized Picocli {@code CommandLine} instance.
     *
     * @param factory The {@code PicocliCommandLineFactory} used to create the {@code CommandLine}.
     * @return The customized {@code CommandLine} instance.
     */
    @Produces
    CommandLine customCommandLine(final PicocliCommandLineFactory factory) {

        final CommandLine cmdLine = factory.create();

        var configurationUtil = CustomConfigurationUtil.getInstance();

        // Injecting custom push mixins to the global push command
        configurationUtil.injectPushMixins(cmdLine);
        // Injecting custom pull mixins to the global pull command
        configurationUtil.injectPullMixins(cmdLine);
        // Customizing the CommandLine object
        configurationUtil.customize(cmdLine);

        return cmdLine;
    }

}

/**
 * A utility class for customizing configurations.
 */
class CustomConfigurationUtil {

    private static final CustomConfigurationUtil INSTANCE = new CustomConfigurationUtil();

    private CustomConfigurationUtil() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Singleton already constructed");
        }
    }

    /**
     * Returns the singleton instance of CustomConfigurationUtil.
     *
     * @return the singleton instance of CustomConfigurationUtil
     */
    public static CustomConfigurationUtil getInstance() {
        return INSTANCE;
    }

    /**
     * Customizes a CommandLine object.
     *
     * @param cmdLine the CommandLine object to customize
     * @return the customized CommandLine object
     */
    public void customize(CommandLine cmdLine) {

        cmdLine.setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionStrategy(new DotExecutionStrategy(new CommandLine.RunLast()))
                .setExecutionExceptionHandler(new DotExceptionHandler())
                .setExitCodeExceptionMapper(new DotExitCodeExceptionMapper());
    }

    /**
     * Injects custom push mixins into the global push command.
     *
     * @param cmdLine the main entry command
     */
    public void injectPushMixins(CommandLine cmdLine) {

        // Looking for the global push command
        CommandSpec pushCommandSpec = cmdLine.getSubcommands().get(PushCommand.NAME)
                .getCommandSpec();

        // Get all instances that implement DotPush
        Instance<DotPush> dotPushCommands = CDI.current().select(DotPush.class);

        // Iterate over each DotPush instance and add their options to the PushCommand's spec
        for (var pushSubCommand : dotPushCommands) {
            CommandSpec commandSpec = CommandSpec.forAnnotatedObject(
                    pushSubCommand);

            var mixin = pushSubCommand.getCustomMixinName();
            mixin.ifPresent(mixinName -> {
                if (commandSpec.mixins().containsKey(mixinName)) {
                    Iterable<OptionSpec> options = commandSpec.mixins().get(mixinName).options();
                    for (OptionSpec option : options) {
                        pushCommandSpec.add(option);
                    }
                }
            });
        }
    }

    /**
     * Injects custom pull mixins into the global pull command.
     *
     * @param cmdLine the main entry command
     */
    public void injectPullMixins(CommandLine cmdLine) {

        // Looking for the global pull command
        CommandSpec pullCommandSpec = cmdLine.getSubcommands().get(PullCommand.NAME)
                .getCommandSpec();

        // Get all instances that implement DotPull
        Instance<DotPull> dotPullCommands = CDI.current().select(DotPull.class);

        // Iterate over each DotPull instance and add their options to the PullCommand's spec
        for (var pullSubCommand : dotPullCommands) {
            CommandSpec commandSpec = CommandSpec.forAnnotatedObject(
                    pullSubCommand);

            var mixin = pullSubCommand.getCustomMixinName();
            mixin.ifPresent(mixinName -> {
                if (commandSpec.mixins().containsKey(mixinName)) {
                    Iterable<OptionSpec> options = commandSpec.mixins().get(mixinName).options();
                    for (OptionSpec option : options) {
                        pullCommandSpec.add(option);
                    }
                }
            });
        }
    }

}
