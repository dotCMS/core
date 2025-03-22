package com.dotmarketing.util;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;


class EmailUtilsTest {

    private static final String DOT_MAIL_FROM_ADDRESS = "configured@example.com";
    private static final String COMPANY_MAIL_ADDRESS = "from@example.com";

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
        company.setEmailAddress(COMPANY_MAIL_ADDRESS);
        company.setName("Acme Corp");
    }

    @Test
    void testSendMail_UsesConfiguredFromAddress_WhenSet() {
        try (MockedStatic<Config> mockedConfig = mockStatic(Config.class)) {
            // Simulate DOT_MAIL_FROM_ADDRESS being set
            mockedConfig.when(() -> Config.getStringProperty("DOT_MAIL_FROM_ADDRESS", "from@example.com"))
                    .thenReturn(DOT_MAIL_FROM_ADDRESS);

            // Act
            EmailUtils.sendMail(user, company, "Test Subject", "Test Body");

            // Assert
            verify(mockMailer).setFromEmail(DOT_MAIL_FROM_ADDRESS);
            verify(mockMailer).sendMessage();
        }
    }

    @Test
    void testSendMail_UsesFallbackEmail_WhenNoConfigSet() {
        // Call the sendMail method.
        EmailUtils.sendMail(user, company, "Test Subject", "Test Body");

        // Verify that setFromEmail was called with the expected value.
        verify(mockMailer).setFromEmail(COMPANY_MAIL_ADDRESS);

        // Optionally, verify that sendMessage was called.
        verify(mockMailer).sendMessage();
    }
}

