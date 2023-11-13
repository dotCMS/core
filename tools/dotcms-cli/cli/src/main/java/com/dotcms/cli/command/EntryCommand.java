package com.dotcms.cli.command;

import com.dotcms.api.client.AuthenticationParam;
import com.dotcms.cli.command.contenttype.ContentTypeCommand;
import com.dotcms.cli.command.files.FilesCommand;
import com.dotcms.cli.command.language.LanguageCommand;
import com.dotcms.cli.command.site.SiteCommand;
import com.dotcms.cli.common.ExceptionHandlerImpl;
import com.dotcms.cli.common.LoggingExecutionStrategy;
import com.dotcms.cli.common.OutputOptionMixin;
import io.quarkus.arc.Unremovable;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

@TopCommand
@CommandLine.Command(
        name = "dotCMS",
        mixinStandardHelpOptions = true,
        version = {"dotcms-cli 1.0", "picocli " + CommandLine.VERSION},
        description = {
                "@|bold,underline,blue dotCMS|@ cli is a command line interface to interact with your @|bold,underline,blue dotCMS|@ instance.",
        },
        header = "dotCMS cli",
        subcommands = {
                //-- Miscellaneous stuff
                StatusCommand.class,
                InstanceCommand.class,
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
public class EntryCommand  {

    // Declared here, so we have an instance available via Arc container
    @Unremovable
    @Inject
    ExceptionHandlerImpl exceptionHandler;

    @Unremovable
    @Inject
    AuthenticationParam authenticationParam;

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

        CommandLine cmdLine = factory.create();

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
                .setExecutionStrategy(
                        new LoggingExecutionStrategy(new CommandLine.RunLast()))
                .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                    final Object object = commandLine.getCommand();
                    if (object instanceof DotCommand) {
                        final DotCommand command = (DotCommand) object;
                        final OutputOptionMixin output = command.getOutput();
                        return output.handleCommandException(ex,
                                String.format("Error in command [%s] with message: %n ",
                                        command.getName()));
                    } else {
                        commandLine.getErr().println(ex.getMessage());
                    }
                    return ExitCode.SOFTWARE;
                }).setExitCodeExceptionMapper(t -> {
                    // customize exit code
                    // We usually throw an IllegalArgumentException to denote that an invalid param has been passed
                    if (t instanceof ParameterException || t instanceof IllegalArgumentException) {
                        return ExitCode.USAGE;
                    }
                    return ExitCode.SOFTWARE;
                });
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
