package com.dotcms.cli.command;


import com.dotcms.api.AuthSecurityContext;
import com.dotcms.api.AuthenticationAPI;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(name = "login", description = "Login Command Expects a user and a password.")
public class LoginCommand implements Runnable {

    private static final Logger logger = Logger.getLogger(LoginCommand.class);

    @CommandLine.Option(names = {"-u", "--user"}, description = "User name", defaultValue = "admin@dotcms.com", required = true, interactive = true )
    String user;

    @CommandLine.Option(names = {"-p", "--password"}, arity = "0..1", description = "Passphrase", interactive = true, required = true )
    String password;

    @Inject
    @RestClient
    AuthenticationAPI client;

    @Inject
    AuthSecurityContext authSecurityContext;


    @Override
    public void run() {
        final APITokenRequest tokenRequest = APITokenRequest.builder().user(user).password(password).expirationDays(10).build();
        final ResponseEntityView<TokenEntity> response = client.getToken(tokenRequest);
        authSecurityContext.setToken(response.entity().token(), user);
        logger.info(String.format("Successfully logged-in as %s. ",user));
    }
}
