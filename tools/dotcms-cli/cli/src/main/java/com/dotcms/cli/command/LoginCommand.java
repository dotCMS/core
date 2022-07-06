package com.dotcms.cli.command;


import com.dotcms.cli.ApplicationContext;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import com.dotcms.restclient.LegacyAuthenticationClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import javax.inject.Inject;

@CommandLine.Command(name = "login", description = "Login Command Expects a user and a password.")
public class LoginCommand implements Runnable {

    private static final Logger logger = Logger.getLogger(LoginCommand.class);

    @CommandLine.Option(names = {"-u", "--user"}, description = "User name", defaultValue = "admin@dotcms.com", required = true, interactive = true )
    String user;

    @CommandLine.Option(names = {"-p", "--password"}, arity = "0..1", description = "Passphrase", interactive = true, required = true )
    String password;

    @Inject
    @RestClient
    LegacyAuthenticationClient client;

    @Inject
    ApplicationContext applicationContext;


    @Override
    public void run() {
        final APITokenRequest tokenRequest = APITokenRequest.builder().user(user).password(password).expirationDays(10).build();
        final APITokenResponse resp = client.getToken(tokenRequest);
        applicationContext.setToken(resp.entity().token(), user);
        logger.info(String.format("Successfully logged-in as %s. ",user));
    }
}
