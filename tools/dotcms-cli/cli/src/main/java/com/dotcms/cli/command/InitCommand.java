package com.dotcms.cli.command;

import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = InitCommand.NAME,
        header = "@|bold,blue Initialize the cli and Configures dotCMS instances.|@",
        description = {
                " Creates the initial configuration file for the dotCMS CLI.",
                " The file is created in the user's home directory.",
                " The file is named [dot-service.yml]",
                " This file is used to store the configuration of the dotCMS instances.",
                " An instance can be activated by using the command @|yellow instance -act <instanceName>|@",
                " Typically an instance holds the API URL, the user and the profile-name.",
                " Running this command is mandatory before using the CLI.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class InitCommand implements Callable<Integer>, DotCommand {

    public static final String NAME = "init";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    @Override
    public Integer call() throws Exception {
        return null;
    }
}
