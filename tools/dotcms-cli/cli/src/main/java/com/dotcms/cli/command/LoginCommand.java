package com.dotcms.cli.command;


import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.common.OutputOptionMixin;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = LoginCommand.NAME,
        description = "Login Command Expects a user in --user and a password in --password. Both are mandatory params."
)
public class LoginCommand implements Runnable {

    static final String NAME = "login";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Option(names = {"-u", "--user"}, description = "User name", defaultValue = "admin@dotcms.com", required = true, interactive = true )
    String user;

    @CommandLine.Option(names = {"-p", "--password"}, arity = "0..1", description = "Passphrase", interactive = true, required = true )
    String password;

    @Inject
    AuthenticationContext authenticationContext;

    @Override
    public void run() {
        output.info(String.format("Logging in as [@|bold,cyan %s|@]. ",user));
        try {
            authenticationContext.login(user, password);
            output.info(String.format("@|bold,green Successfully logged-in as |@ [@|bold,blue %s|@]", user));
        }catch (Exception wae){
            output.error("Unable to login. ", wae);
        }
    }
}
