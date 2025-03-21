package com.dotmarketing.util;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;


class EmailUtilsTest {
    private MailerWrapper mockMailer;
    private MailerWrapperFactory mockFactory;
    private User user;
    private Company company;

    @BeforeEach
    void setUp() {
        // Create a mock MailerWrapper.
        mockMailer = mock(MailerWrapper.class);

        // Factory always returns the mock MailerWrapper.
        mockFactory = () -> mockMailer;

        // Override the factory in EmailUtils.
        EmailUtils.setMailerWrapperFactory(mockFactory);

        // Create common User and Company objects.
        user = new User();
        user.setEmailAddress("john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        company = new Company();
        company.setEmailAddress("from@example.com");
        company.setName("Acme Corp");
    }

    @Test
    void testSendMail_UsesConfiguredFromAddress_WhenSet() {
        String configuredFromEmail = "configured@example.com";

        try (MockedStatic<Config> mockedConfig = mockStatic(Config.class)) {
            // Simulate DOT_MAIL_FROM_ADDRESS being set
            mockedConfig.when(() -> Config.getStringProperty("DOT_MAIL_FROM_ADDRESS", "from@example.com"))
                    .thenReturn(configuredFromEmail);

            // Act
            EmailUtils.sendMail(user, company, "Test Subject", "Test Body");

            // Assert
            verify(mockMailer).setFromEmail(configuredFromEmail);
            verify(mockMailer).sendMessage();
        }
    }

    @Test
    void testSendMail_UsesFallbackEmail_WhenNoConfigSet() {
        String fallbackEmail = "from@example.com"; // Should default to company email

        // Call the static sendMail method.
        EmailUtils.sendMail(user, company, "Test Subject", "Test Body");

        // Calculate what the expected from-email should be.
        String expectedFromEmail = ConfigUtils.getGlobalFromAddressOrFallback("from@example.com");

        // Verify that setFromEmail was called with the expected value.
        verify(mockMailer).setFromEmail(expectedFromEmail);

        // Optionally, verify that sendMessage was called.
        verify(mockMailer).sendMessage();
    }
}

