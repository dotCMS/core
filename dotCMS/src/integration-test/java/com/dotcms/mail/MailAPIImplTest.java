package com.dotcms.mail;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.mail.Authenticator;
import javax.mail.Session;
import java.util.Properties;

/**
 * Unit test for {@link MailAPI}
 */
public class MailAPIImplTest {
    
    MailAPIImpl mailAPI = new MailAPIImpl();

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment

        IntegrationTestInitService.getInstance().init();
        Config.setProperty("DOT_MAIL_SMTP_user","testUser");
        Config.setProperty("DOT_MAIL_SMTP_host", "testHost");
        Config.setProperty("DOT_MAIL_SMTP_port", "12345");
        Config.setProperty("DOT_MAIL_SMTP_connectiontimeout", "54321");
        Config.setProperty("DOT_MAIL_SMTP_auth", "true");
        Config.setProperty("DOT_MAIL_SMTP_ehlo", "false");
        Config.setProperty("mail.testing.XXX", "true");
    }

    /**
     * Method to test: loadMailSessionFromContext and  getMailSession
     * Given Scenario: Checks that the Mail Session be always the same
     * ExpectedResult: If gets the session twice should be the same
     */
    @Test
    public void test_loadMailSessionFromContext() {

        assert(!mailAPI.loadMailSessionFromContext().isPresent());
        final Session session = mailAPI.getMailSession();
        
        // JNDI context not working
        //assert(api.loadMailSessionFromContext().isPresent());
        final Session session2 = mailAPI.getMailSession();
        
        assert(session == session2);
    }

    /**
     * The MailConfig class, based on the property, converts the String to the expected key so the props
     * can be overwritten by environmental variables
     * Method to test: MailConfig
     * Given Scenario: Gets the configuration from config
     * ExpectedResult: Configuration returned should be the expected
     */
    @Test
    public void test_mail_config_properties() {

        Properties props = new MailConfig().properties.get();
        
        assert(props.get("mail.smtp.user").equals("testUser"));
        assert(props.get("mail.smtp.host").equals("testHost"));
        assert(props.get("mail.smtp.port").equals("12345"));
        assert(props.get("mail.smtp.connectiontimeout").equals("54321"));
        assert(props.get("mail.smtp.auth").equals("true"));
        assert(props.get("mail.smtp.ehlo").equals("false"));
        assert(props.get("mail.testing.xxx").equals("true"));
        assert(!props.contains("DOT_MAIL_SMTP_auth"));
    }

    /**
     * Method to test: createAuthenticator
     * Given Scenario:
     *      - Check different scenarios were the authentication is off ("mail.smtp.auth", "false")
     *      - Check different scenarios were the authentication is on ("mail.smtp.auth", "true"), but with user, without password
     *      - Check different scenarios were the authentication is on ("mail.smtp.auth", "true"), but with user and password, -> creates an authenticator
     * ExpectedResult: Configuration returned should be the expected
     */
    @Test
    public void test_building_authenticator() {
        
        // test default
        Properties properties = new Properties();
        Authenticator authenticator = new MailAPIImpl(properties).createAuthenticator();
        assert(authenticator ==null);

        // test smtp.auth=false
        properties.setProperty("mail.smtp.auth", "false");
        authenticator = new MailAPIImpl(properties).createAuthenticator();
        assert(authenticator ==null);
        
        
        // Test no password
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.user", "test");
        authenticator = new MailAPIImpl(properties).createAuthenticator();
        assert(authenticator ==null);
        
        
        // Test password
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.user", "test");
        properties.setProperty("mail.smtp.password", "test");
        authenticator = new MailAPIImpl(properties).createAuthenticator();
        assert(authenticator !=null);
    }

}
