package com.dotmarketing.util;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;


class EmailUtilsTest {

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
        EmailUtils.sendMail(user, company, "Test Subject", "Test Body");

        verify(mockMailer).setFromEmail(COMPANY_MAIL_ADDRESS);
        verify(mockMailer).sendMessage();
    }

    @Test
    void testSendMail_UsesFallbackEmail_WhenNoConfigSet() {
        EmailUtils.sendMail(user, company, "Test Subject", "Test Body");

        verify(mockMailer).setFromEmail(COMPANY_MAIL_ADDRESS);
        verify(mockMailer).sendMessage();
    }
}

