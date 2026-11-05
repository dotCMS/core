package com.dotmarketing.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for how {@link DotWebdavHelper#authorizePrincipal(String, String)} treats the
 * answer of the configured pre-authenticator ({@code auth.pipeline.pre}).
 *
 * <p>Passwords are only accepted when {@code ALLOW_BASIC_AUTH} is {@code ALL}, so the class runs
 * in that mode and restores the previous value afterwards.</p>
 */
public class DotWebdavHelperAuthorizePrincipalTest extends IntegrationTestBase {

    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";

    private static User user;
    private static String login;
    private static String previousBasicAuthMode;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        previousBasicAuthMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");

        user = new UserDataGen()
                .emailAddress("webdav-auth-" + UUIDGenerator.generateUuid() + "@dotcms.test")
                .password(PASSWORD)
                .nextPersisted();

        login = Company.AUTH_TYPE_ID.equals(PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? user.getUserId() : user.getEmailAddress();
    }

    @AfterClass
    public static void cleanup() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousBasicAuthMode);
        UserDataGen.remove(user);
    }

    /**
     * Given: a pre-authenticator that rejects the login by returning a failure result.
     * When:  WebDAV authorizes a user who submits that user's correct dotCMS password.
     * Then:  access is denied, just as when the pre-authenticator throws.
     */
    @Test
    public void test_authorizePrincipal_preAuthenticatorFailure_deniesAccess() {
        final DotWebdavHelper helper = new DotWebdavHelper();

        assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(
                login, PASSWORD, RejectingPreAuthenticator.class.getName()));
    }

    /**
     * Given: a pre-authenticator that lets the login continue.
     * When:  WebDAV authorizes a user with the correct password, and then with a wrong one.
     * Then:  the correct password returns the user and the wrong one is denied.
     */
    @Test
    public void test_authorizePrincipal_preAuthenticatorSuccess_checksPassword() throws Exception {
        final DotWebdavHelper helper = new DotWebdavHelper();

        final User authorized = helper.authorizePrincipal(
                login, PASSWORD, AcceptingPreAuthenticator.class.getName());
        assertEquals(user.getUserId(), authorized.getUserId());

        assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(
                login, PASSWORD + "-wrong", AcceptingPreAuthenticator.class.getName()));
    }

    /**
     * Given: no pre-authenticator configured.
     * When:  WebDAV authorizes a user with the correct password.
     * Then:  the user is returned, as before.
     */
    @Test
    public void test_authorizePrincipal_noPreAuthenticator_checksPassword() throws Exception {
        final DotWebdavHelper helper = new DotWebdavHelper();

        assertEquals(user.getUserId(),
                helper.authorizePrincipal(login, PASSWORD, null).getUserId());
    }

    /**
     * Given: Basic authentication limited to API tokens ({@code ALLOW_BASIC_AUTH=JWT_TOKEN}) and a
     *        pre-authenticator that would accept the login.
     * When:  WebDAV authorizes a user who submits that user's correct dotCMS password.
     * Then:  access is denied, because only an API token is accepted in that mode.
     */
    @Test
    public void test_authorizePrincipal_tokenOnlyMode_rejectsPassword() {
        final DotWebdavHelper helper = new DotWebdavHelper();
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        try {
            assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(
                    login, PASSWORD, AcceptingPreAuthenticator.class.getName()));
        } finally {
            Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        }
    }
}
