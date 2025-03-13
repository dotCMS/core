package com.dotcms.api.client.analytics;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the {@link AnalyticsService} to verify analytics events are properly
 * recorded through API calls.
 */
@QuarkusTest
@TestProfile(DotCMSITProfile.class)
public class AnalyticsServiceIT {

    @Inject
    AnalyticsService analyticsService;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @Inject
    Logger logger;

    @BeforeEach
    public void setupTest() throws IOException {

        // Create a test service profile
        serviceManager.removeAll().persist(
                ServiceBean.builder().
                        name("default").
                        url(new URL("http://localhost:8080")).
                        active(true).
                        build()
        );

        // Login
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    /**
     * <b>Method to test:</b> recordCommand <br>
     * <b>Given Scenario:</b> Analytics service is called with a simple command and arguments <br>
     * <b>Expected Result:</b> The command should be recorded without throwing any exceptions
     */
    @Test
    void testRecordSimpleCommand() throws IOException {
        // A simple command with no arguments
        final String command = "status";
        final List<String> arguments = Collections.emptyList();

        // This should not throw any exceptions
        analyticsService.recordCommand(command, arguments);

        // If we reached here without exceptions, the test is successful
        logger.info("Successfully recorded analytics for command: " + command);
    }

    /**
     * <b>Method to test:</b> recordCommand <br>
     * <b>Given Scenario:</b> Analytics service is called with a complex command and multiple
     * arguments <br>
     * <b>Expected Result:</b> The command with all its arguments should be recorded without
     * throwing any exceptions
     */
    @Test
    void testRecordComplexCommand() throws IOException {
        // A more complex command with multiple arguments
        final String command = "push";
        final List<String> arguments = Arrays.asList(
                "/path/to/site",
                "--workspace", "/path/to/workspace",
                "--languages", "en-us,es-es",
                "--dry-run"
        );

        // This should not throw any exceptions
        analyticsService.recordCommand(command, arguments);

        // If we reached here without exceptions, the test is successful
        logger.info("Successfully recorded analytics for command: " + command + " with arguments: "
                + String.join(" ", arguments));
    }

    /**
     * <b>Method to test:</b> recordCommand <br>
     * <b>Given Scenario:</b> Analytics service is called with a command containing special
     * characters <br>
     * <b>Expected Result:</b> The command should be properly encoded and recorded without throwing
     * any exceptions
     */
    @Test
    void testRecordCommandWithSpecialCharacters() throws IOException {
        // Command with special characters
        final String command = "pull";
        final List<String> arguments = Arrays.asList(
                "/path/with spaces/and+special&chars",
                "--site", "demo.dotcms.com",
                "--filter", "*.html,*.css,*.js",
                "--exclude", "tmp/*"
        );

        // This should not throw any exceptions
        analyticsService.recordCommand(command, arguments);

        // If we reached here without exceptions, the test is successful
        logger.info("Successfully recorded analytics for command with special characters");
    }

}