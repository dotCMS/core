package com.dotcms.api.client.analytics;

import com.dotcms.api.AnalyticsAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.analytics.AnalyticsEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AnalyticsServiceImpl implements AnalyticsService {

    static final String COMMAND_EVENT_TYPE = "CMD_EXECUTED";

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    Logger logger;

    @Override
    @ActivateRequestContext
    public void recordCommand(final String command, final List<String> arguments)
            throws IOException {

        final var serviceProfileOpt = clientFactory.getServiceProfile();
        if (serviceProfileOpt.isPresent()) {

            final var serviceProfile = serviceProfileOpt.get();

            final var url = serviceProfile.url().toString();
            final String user;
            if (serviceProfile.credentials() != null) {
                user = serviceProfile.credentials().user();
            } else {
                user = "UNKNOWN";
                logger.error(
                        "No credentials found for the service profile: " + serviceProfile.name()
                );
            }

            try {

                final var analyticsAPI = clientFactory.getClient(AnalyticsAPI.class);
                analyticsAPI.fireEvent(
                        AnalyticsEvent.builder()
                                .command(command)
                                .arguments(arguments)
                                .eventType(COMMAND_EVENT_TYPE)
                                .user(user)
                                .site(url)
                                .build());
                logger.debug(
                        String.format(
                                "Event recorded: URL [%s] - user [%s] - command [%s]",
                                url, user, String.join(" ", arguments)
                        )
                );
            } catch (Exception e) {
                logger.error(
                        String.format(
                                "Error recording event: URL [%s] - user [%s] - command [%s]",
                                url, user, String.join(" ", arguments)
                        ), e
                );
            }
        } else {
            logger.error("No service profile found.");
        }

    }
}
