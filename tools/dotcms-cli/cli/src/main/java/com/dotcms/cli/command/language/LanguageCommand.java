package com.dotcms.cli.command.language;

import com.dotcms.cli.common.HelpOption;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
        name = com.dotcms.cli.command.language.LanguageCommand.NAME,
        aliases = { com.dotcms.cli.command.language.LanguageCommand.ALIAS },
        header = "Language CRUD operations.",
        subcommands = {
                LanguagePull.class,
                LanguageFind.class
        }
)
/**
 * Super command for language operations
 * @author nollymar
 */
public class LanguageCommand implements Callable<Integer> {

    static final String NAME = "language";
    static final String ALIAS = "lang";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(System.out);
        output.info("Listing languages (default action)");
        return spec.commandLine().execute(NAME, LanguageFind.NAME);
    }

}
