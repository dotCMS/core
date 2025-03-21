package com.dotmarketing.util;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;


public class EmailUtilsTest {

    @Test
    public void testSendMail_setsFromEmailUsingConfigUtils() {
        // Create a mock MailerWrapper.
        MailerWrapper mockMailer = mock(MailerWrapper.class);

        // Create a factory that always returns the mock MailerWrapper.
        MailerWrapperFactory mockFactory = new MailerWrapperFactory() {
            @Override
            public MailerWrapper createMailer() {
                return mockMailer;
            }
        };

        // Override the factory in EmailUtils.
        EmailUtils.setMailerWrapperFactory(mockFactory);

        // Create dummy User and Company objects.
        User user = new User();
        user.setEmailAddress("john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        Company company = new Company();
        company.setEmailAddress("from@example.com");
        company.setName("Acme Corp");

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

