package com.dotcms.api.client.analytics;

import static com.dotcms.cli.common.GlobalMixin.OPTION_NO_VALIDATE_UNMATCHED_ARGUMENTS;

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

    private final RestClientFactory clientFactory;
    private final Logger logger;

    @Inject
    public AnalyticsServiceImpl(final RestClientFactory clientFactory, final Logger logger) {
        this.clientFactory = clientFactory;
        this.logger = logger;
    }

    @Override
    @ActivateRequestContext
    public void recordCommand(final String command, final List<String> arguments)
            throws IOException {

        if (skipEventRecord(arguments)) {
            return;
        }

        try {

            final var analyticsAPI = clientFactory.getClient(AnalyticsAPI.class);
            analyticsAPI.fireEvent(
                    AnalyticsEvent.builder()
                            .command(command)
                            .arguments(arguments)
                            .eventType(COMMAND_EVENT_TYPE)
                            .build());
            logger.debug(
                    String.format(
                            "Event recorded: command [%s][%s]",
                            command, String.join(" ", arguments)
                    )
            );
        } catch (Exception e) {
            logger.error(
                    String.format(
                            "Error recording event: command [%s][%s]",
                            command, String.join(" ", arguments)
                    ), e
            );
        }
    }

    /**
     * Determines whether event recording should be skipped based on: 1. Command arguments
     * containing a flag indicating it's a subcommand 2. Absence of a service profile
     *
     * @param arguments The command arguments to check
     * @return true if event recording should be skipped, false otherwise
     */
    private boolean skipEventRecord(final List<String> arguments) throws IOException {

        // Skip if this is a subcommand (indicated by the presence of --noValidateUnmatchedArguments)
        if (arguments.contains(OPTION_NO_VALIDATE_UNMATCHED_ARGUMENTS)) {
            return true;
        }

        // Skip if no service profile is available
        final var serviceProfileOpt = clientFactory.getServiceProfile();
        if (serviceProfileOpt.isEmpty()) {
            logger.error("No service profile found.");
            return true;
        }

        return false;
    }

}
