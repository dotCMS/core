package com.dotmarketing.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.auth.LoginLockout;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import java.time.Instant;
import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for native WebDAV password/token authentication and login lockout through
 * {@link DotWebdavHelper#authorizePrincipal(String, String)}.
 */
public class DotWebdavHelperAuthorizePrincipalTest extends IntegrationTestBase {

    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";
    private static final String LEGACY_PRE_AUTHENTICATOR = "com.dotcms.enterprise.LDAPProxy";

    private static User user;
    private static String login;
    private static String previousBasicAuthMode;
    private static String previousPreAuthenticator;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        previousBasicAuthMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        previousPreAuthenticator = Config.getStringProperty(PropsUtil.AUTH_PIPELINE_PRE, null);
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        // Keep legacy configuration in place for every public-path check, including the first helper construction.
        Config.setProperty(PropsUtil.AUTH_PIPELINE_PRE, LEGACY_PRE_AUTHENTICATOR);

        user = new UserDataGen()
                .emailAddress("webdav-auth-" + UUIDGenerator.generateUuid() + "@dotcms.test")
                .password(PASSWORD)
                .nextPersisted();

        login = Company.AUTH_TYPE_ID.equals(PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? user.getUserId() : user.getEmailAddress();
    }

    @After
    public void clearLockout() {
        LoginLockout.getInstance().clear(user.getUserId());
    }

    @AfterClass
    public static void cleanup() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousBasicAuthMode);
        Config.setProperty(PropsUtil.AUTH_PIPELINE_PRE, previousPreAuthenticator);
        UserDataGen.remove(user);
    }

    /** The removed LDAP class must not be instantiated, even when its old property is still set. */
    @Test
    public void test_authorizePrincipal_removedLdapPreAuthenticator_acceptsNativePassword() throws Exception {
        assertEquals(LEGACY_PRE_AUTHENTICATOR, PropsUtil.get(PropsUtil.AUTH_PIPELINE_PRE));

        assertEquals(user.getUserId(),
                new DotWebdavHelper().authorizePrincipal(login, PASSWORD).getUserId());
    }

    @Test
    public void test_authorizePrincipal_nativePassword_checksPassword() throws Exception {
        final DotWebdavHelper helper = new DotWebdavHelper();

        assertEquals(user.getUserId(), helper.authorizePrincipal(login, PASSWORD).getUserId());
        assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(login, PASSWORD + "-wrong"));
    }

    @Test
    public void test_authorizePrincipal_allMode_rejectsInvalidToken() {
        final DotWebdavHelper helper = new DotWebdavHelper();

        assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(login, "invalid-api-token"));
    }

    @Test
    public void test_authorizePrincipal_tokenOnlyMode_rejectsPasswordAndInvalidToken() {
        final DotWebdavHelper helper = new DotWebdavHelper();
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        try {
            assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(login, PASSWORD));
            assertThrows(DotSecurityException.class,
                    () -> helper.authorizePrincipal(login, "invalid-api-token"));
        } finally {
            Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        }
    }

    @Test
    public void test_authorizePrincipal_disabledMode_rejectsPassword() {
        final DotWebdavHelper helper = new DotWebdavHelper();
        Config.setProperty("ALLOW_BASIC_AUTH", "NONE");
        try {
            assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(login, PASSWORD));
        } finally {
            Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        }
    }

    /** Password failures lock only WebDAV; valid API tokens still work without clearing the lock. */
    @Test
    public void test_authorizePrincipal_lockedPassword_stillAcceptsApiToken() throws Exception {
        final String previousLockoutEnabled = Config.getStringProperty(LoginLockout.LOCKOUT_ENABLED, null);
        final String previousLimit = Config.getStringProperty(PropsUtil.AUTH_MAX_FAILURES_LIMIT, null);
        final String previousAccountLimit = Config.getStringProperty(LoginLockout.LOCKOUT_ACCOUNT_MAX_FAILURES, null);
        final DotWebdavHelper helper = new DotWebdavHelper();
        ApiToken token = null;
        try {
            Config.setProperty(LoginLockout.LOCKOUT_ENABLED, true);
            Config.setProperty(PropsUtil.AUTH_MAX_FAILURES_LIMIT, 2);
            Config.setProperty(LoginLockout.LOCKOUT_ACCOUNT_MAX_FAILURES, 0);
            token = APILocator.getApiTokenAPI().persistApiToken(ApiToken.builder()
                    .withIssuer(ClusterFactory.getClusterId())
                    .withExpires(Date.from(Instant.now().plusSeconds(3600)))
                    .withUserId(user.getUserId())
                    .withRequestingUserId(APILocator.systemUser().getUserId())
                    .withRequestingIp("192.0.2.10")
                    .build(), APILocator.systemUser());
            final String jwt = APILocator.getApiTokenAPI().getJWT(token, APILocator.systemUser());

            assertEquals(user.getUserId(), helper.authorizePrincipal(login, jwt).getUserId());
            for (int i = 0; i < 2; i++) {
                assertThrows(DotSecurityException.class,
                        () -> helper.authorizePrincipal(login, PASSWORD + "-wrong"));
            }
            final String source = LoginLockout.webdavSource();
            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId(), source));
            assertFalse(LoginLockout.getInstance().isLocked(user.getUserId(), LoginLockout.clientAddress()));
            assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(login, PASSWORD));
            assertThrows(DotSecurityException.class,
                    () -> helper.authorizePrincipal(login, "invalid-api-token"));
            assertEquals(user.getUserId(), helper.authorizePrincipal(login, jwt).getUserId());
            assertTrue(LoginLockout.getInstance().isLocked(user.getUserId(), source));

            Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
            assertEquals(user.getUserId(), helper.authorizePrincipal(login, jwt).getUserId());
            assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(login, PASSWORD));

            Config.setProperty("ALLOW_BASIC_AUTH", "NONE");
            assertThrows(DotSecurityException.class, () -> helper.authorizePrincipal(login, jwt));

            Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
            LoginLockout.getInstance().clear(user.getUserId());
            assertEquals(user.getUserId(), helper.authorizePrincipal(login, PASSWORD).getUserId());
        } finally {
            Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
            Config.setProperty(LoginLockout.LOCKOUT_ENABLED, previousLockoutEnabled);
            Config.setProperty(PropsUtil.AUTH_MAX_FAILURES_LIMIT, previousLimit);
            Config.setProperty(LoginLockout.LOCKOUT_ACCOUNT_MAX_FAILURES, previousAccountLimit);
            if (token != null) {
                APILocator.getApiTokenAPI().deleteToken(token, APILocator.systemUser());
            }
        }
    }
}
