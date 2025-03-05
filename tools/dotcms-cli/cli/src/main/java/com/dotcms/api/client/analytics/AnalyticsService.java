package com.dotcms.api.client.analytics;

import java.io.IOException;
import java.util.List;

/**
 * Service interface for handling analytics-related operations.
 */
public interface AnalyticsService {

    /**
     * Records a command execution event by sending the command name and its corresponding arguments
     * to the analytics service.
     *
     * @param command   the name of the executed command
     * @param arguments a list of arguments associated with the executed command
     * @throws IOException if an I/O error occurs during event recording
     */
    void recordCommand(String command, List<String> arguments) throws IOException;

}
