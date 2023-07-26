package com.dotcms.cli.command;


import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
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
public class LoginCommand implements Callable<Integer> {

    static final String NAME = "login";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.Option(names = {"-u", "--user"}, arity = "1", description = "User name", required = true )
    String user;

    @CommandLine.Option(names = {"-p", "--password"}, arity = "0..1", description = "Passphrase", required = true, interactive = true, echo = false )
    char[] password;

    @Inject
    AuthenticationContext authenticationContext;

    @Override
    public Integer call() {
        output.info(String.format("Logging in as [@|bold,cyan %s|@]. ",user));
        try {
            authenticationContext.login(user, password);
            output.info(String.format("@|bold,green Successfully logged-in as |@[@|bold,blue %s|@]", user));
        }catch (Exception wae){
            return output.handleCommandException(wae,"Unable to login. ");
        }
        return ExitCode.OK;
    }
}
