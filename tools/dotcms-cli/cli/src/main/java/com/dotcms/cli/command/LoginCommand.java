package com.dotcms.cli.command;


import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.common.OutputOptionMixin;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = LoginCommand.NAME,
        description = "@|bold,green Once a profile is selected. Use this command to open a session|@ Expects a user in @|bold,cyan --user -u|@ and a password @|bold,cyan --password -p|@ @|bold Both are mandatory params.|@"
)
public class LoginCommand implements Callable<Integer> {

    static final String NAME = "login";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

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
            return ExitCode.OK;
        }catch (Exception wae){
            output.error("Unable to login. ", wae);
        }
        return ExitCode.SOFTWARE;
    }
}
