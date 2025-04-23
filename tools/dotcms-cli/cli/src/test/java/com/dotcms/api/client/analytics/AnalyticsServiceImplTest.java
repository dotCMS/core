package com.dotcms.api.client.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.AnalyticsAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.analytics.AnalyticsEvent;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for the {@link AnalyticsServiceImpl} class which is responsible for recording command
 * execution events to the analytics service.
 */
class AnalyticsServiceImplTest {

    @Mock
    private RestClientFactory clientFactory;

    @Mock
    private Logger logger;

    @Mock
    private AnalyticsAPI analyticsAPI;

    @Mock
    private ServiceBean serviceBean;

    @Mock
    private CredentialsBean credentials;

    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        analyticsService = new AnalyticsServiceImpl(clientFactory, logger);

        // Default mock behaviors
        when(clientFactory.getClient(AnalyticsAPI.class)).thenReturn(analyticsAPI);
        when(serviceBean.credentials()).thenReturn(credentials);
        when(serviceBean.name()).thenReturn("testProfile");
        when(serviceBean.url()).thenReturn(new URL("https://test.dotcms.com"));
        when(credentials.user()).thenReturn("testUser");
    }

    /**
     * <b>Method to test:</b> recordCommand <br>
     * <b>Given Scenario:</b> Valid service profile with credentials is available <br>
     * <b>Expected Result:</b> Event should be sent to analytics API with correct command,
     * arguments, user, site, and event type.
     */
    @Test
    void recordCommand_shouldSendEventWithCorrectData() throws IOException {
        // Arrange
        String command = "push";
        List<String> arguments = Arrays.asList("path/to/file", "--dry-run");

        when(clientFactory.getServiceProfile()).thenReturn(Optional.of(serviceBean));

        // Create a captor to verify the event
        ArgumentCaptor<AnalyticsEvent> eventCaptor = ArgumentCaptor.forClass(AnalyticsEvent.class);

        // Act
        analyticsService.recordCommand(command, arguments);

        // Assert
        verify(analyticsAPI).fireEvent(eventCaptor.capture());

        AnalyticsEvent capturedEvent = eventCaptor.getValue();
        assertEquals(command, capturedEvent.command());
        assertEquals(arguments, capturedEvent.arguments());
        assertEquals(AnalyticsServiceImpl.COMMAND_EVENT_TYPE, capturedEvent.eventType());

        // Verify logging
        verify(logger).debug(contains("Event recorded"));
    }

    /**
     * <b>Method to test:</b> recordCommand
     * <br>
     * <b>Given Scenario:</b> A command with the "--noValidateUnmatchedArguments" flag is executed
     * <br>
     * <b>Expected Result:</b> No event should be sent to the analytics API as the flag indicates
     * this is a subcommand of a global command execution.
     */
    @Test
    void recordCommand_whenNoValidateUnmatchedArgumentsFlagPresent_shouldNotFireEvent()
            throws IOException {

        when(clientFactory.getServiceProfile()).thenReturn(Optional.of(serviceBean));

        // Execute a command that includes the --noValidateUnmatchedArguments flag
        String command = "push";
        List<String> arguments = Arrays.asList(
                "path/to/file", "--dry-run", "--noValidateUnmatchedArguments"
        );

        // Record the command
        analyticsService.recordCommand(command, arguments);

        // Verify that fireEvent was never called
        verify(analyticsAPI, never()).fireEvent(any(AnalyticsEvent.class));

        // Additional verification that debug or error logging wasn't triggered
        verify(logger, never()).debug(any(String.class));
        verify(logger, never()).error(any(String.class));
    }

    /**
     * <b>Method to test:</b> recordCommand <br>
     * <b>Given Scenario:</b> No service profile is available <br>
     * <b>Expected Result:</b> No event should be sent and an error should be logged
     */
    @Test
    void recordCommand_whenNoServiceProfile_shouldLogError() throws IOException {

        // Arrange
        String command = "push";
        List<String> arguments = Arrays.asList("path/to/file", "--dry-run");

        when(clientFactory.getServiceProfile()).thenReturn(Optional.empty());

        // Act
        analyticsService.recordCommand(command, arguments);

        // Assert
        verify(analyticsAPI, never()).fireEvent(any());
        verify(logger).error("No service profile found.");
    }

    /**
     * <b>Method to test:</b> recordCommand <br>
     * <b>Given Scenario:</b> Analytics API throws an exception when trying to fire event <br>
     * <b>Expected Result:</b> Exception should be caught and an error should be logged.
     */
    @Test
    void recordCommand_whenAnalyticsAPIThrowsException_shouldLogError() throws IOException {

        // Arrange
        String command = "push";
        List<String> arguments = Arrays.asList("path/to/file", "--dry-run");

        when(clientFactory.getServiceProfile()).thenReturn(Optional.of(serviceBean));
        doThrow(new RuntimeException("Connection error")).when(analyticsAPI).fireEvent(any());

        // Act
        analyticsService.recordCommand(command, arguments);

        // Assert
        verify(logger).error(contains("Error recording event"), any(Exception.class));
    }
}