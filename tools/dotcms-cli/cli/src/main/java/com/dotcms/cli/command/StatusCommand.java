package com.dotcms.cli.command;


import com.dotcms.api.AuthSecurityContext;
import com.dotcms.model.user.User;
import com.dotcms.api.LegacyUsersClient;
import java.util.Optional;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "status", description = "Login Status.")
public class StatusCommand implements Runnable {

    private static final Logger logger = Logger.getLogger(StatusCommand.class);

    @Inject
    @RestClient
    LegacyUsersClient usersClient;

    @Inject
    AuthSecurityContext authSecurityContext;

    @Override
    public void run() {
        logger.info("We're set to the API at "+ authSecurityContext.getDotCMSAPIHost());
        final Optional<String> userId = authSecurityContext.getUser();
        if (userId.isEmpty()) {
            logger.info("You're NOT logged in.");
        } else {
            final Optional<String> token = authSecurityContext.getToken();
            if (token.isPresent()) {
                final User user = usersClient.getCurrent();
                logger.info("You're currently logged in as "+user.email());
            } else {
                logger.info("I did not find a valid token saved for saved user "+userId.get());
            }
        }
    }
}
