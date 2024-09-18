package com.dotcms.cli.command;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.common.DirectoryWatcherService;
import com.dotcms.cli.common.DotExceptionHandler;
import com.dotcms.cli.common.DotExecutionStrategy;
import com.dotcms.cli.common.DotExitCodeExceptionMapper;
import com.dotcms.cli.common.SubcommandProcessor;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

/**
 * A utility class for customizing configurations.
 */
@ApplicationScoped
class CustomConfigurationUtil {

    /**
     * Customizes a CommandLine object.
     *
     * @param cmdLine the CommandLine object to customize
     * @return the customized CommandLine object
     */
    @CanIgnoreReturnValue
    public CustomConfigurationUtil customize(CommandLine cmdLine) {

        cmdLine.setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionStrategy(new DotExecutionStrategy(
                    new CommandLine.RunLast(), new SubcommandProcessor(),
                    Arc.container().instance(DirectoryWatcherService.class).get(),
                    Arc.container().instance(ServiceManager.class).get())
                )
                .setExecutionExceptionHandler(new DotExceptionHandler())
                .setExitCodeExceptionMapper(new DotExitCodeExceptionMapper());
        return this;
    }

    /**
     * Injects custom push mixins into the global push command.
     *
     * @param cmdLine the main entry command
     */
    @CanIgnoreReturnValue
    public CustomConfigurationUtil injectPushMixins(CommandLine cmdLine) {

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
        return this;
    }

    /**
     * Injects custom pull mixins into the global pull command.
     *
     * @param cmdLine the main entry command
     */
    @CanIgnoreReturnValue
    public CustomConfigurationUtil injectPullMixins(CommandLine cmdLine) {

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
        return this;
    }

}
