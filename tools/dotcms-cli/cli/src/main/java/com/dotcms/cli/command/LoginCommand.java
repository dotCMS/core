package com.dotcms.cli.command;


import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import java.io.IOException;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = LoginCommand.NAME,
        header = "@|bold,blue Use this command to login to a dotCMS instance.|@",
        description = {
                " Once an instance is selected. Use this command to open a session",
                " Expects a user in @|yellow --user -u|@ and a password @|yellow --password -p|@",
                " @|bold Both parameters are mandatory.|@",
                " If the password is not provided, the command will prompt for it.",
                " if you're not sure which instance is active, use the @|yellow status|@ command.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class LoginCommand implements Callable<Integer>, DotCommand {

    static final String NAME = "login";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.Option(names = {"-u", "--user"}, arity = "1", description = "User name", required = true )
    String user;

    @CommandLine.Option(names = {"-p", "--password"}, arity = "0..1", description = {
        "Passphrase",
        "The following is the recommended way to use this param ",
        "as the password will be promoted securely",
        "@|yellow login -u=admin@dotCMS.com -p |@",
        "Both options, user and password are mandatory",
        "and they can also be provided inline as follows:",
        "@|yellow login -u=admin@dotCMS.com -p=admin |@",
        "However, this opens a possibility for @|cyan password theft|@",
        "as the password will be visible in the command history."
    }, required = true, interactive = true, echo = false )
    char[] password;

    @Inject
    AuthenticationContext authenticationContext;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws IOException {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        output.info(String.format("Logging in as [@|bold,cyan %s|@]. ",user));
        authenticationContext.login(user, password);
        output.info(String.format("@|bold,green Successfully logged-in as |@[@|bold,blue %s|@]", user));
        return ExitCode.OK;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }
}
