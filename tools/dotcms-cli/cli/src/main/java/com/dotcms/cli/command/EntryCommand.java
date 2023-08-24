package com.dotcms.cli.command;

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
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParameterException;

@TopCommand
@CommandLine.Command(
        name = "dotCMS",
        mixinStandardHelpOptions = true,
        version = {"dotCMS-cli 1.0", "picocli " + CommandLine.VERSION},
        description = {
                "@|bold,underline,blue dotCMS|@ cli is a command line interface to interact with your @|bold,underline,blue dotCMS|@ instance.",
        },
        header = "dotCMS cli",
        subcommands = {
          //-- Miscellaneous stuff
           StatusCommand.class,
           InstanceCommand.class,
           LoginCommand.class,

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
        CommandLine customCommandLine( final PicocliCommandLineFactory factory) {
                return factory.create()
                        .setCaseInsensitiveEnumValuesAllowed(true)
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
}
