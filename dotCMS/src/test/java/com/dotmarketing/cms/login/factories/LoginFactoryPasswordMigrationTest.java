package com.dotmarketing.cms.login.factories;

import static com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus.AUTHENTICATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Exercises real password hashing through both login selectors with mocked persistence. */
@RunWith(Parameterized.class)
public class LoginFactoryPasswordMigrationTest {

    private static final Properties FIXTURES = new Properties();

    @Parameterized.Parameter
    public String authType;

    private UserAPI userAPI;
    private User user;
    private User systemUser;
    private final AtomicReference<String> storedPassword = new AtomicReference<>();
    private boolean storedEncrypted;
    private MockedStatic<APILocator> api;
    private MockedStatic<PublicCompanyFactory> companies;
    private MockedStatic<PropsUtil> props;
    private MockedStatic<SecurityLogger> securityLogger;

    /** Runs each persistence scenario for email-address and user-ID authentication. */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> authenticationModes() {
        return Arrays.asList(new Object[][]{{Company.AUTH_TYPE_EA}, {Company.AUTH_TYPE_ID}});
    }

    /** Loads frozen reference hashes, leaving production hashing unmocked. */
    @BeforeClass
    public static void prepare() throws Exception {
        Config.initializeConfig();
        try (InputStream input = LoginFactoryPasswordMigrationTest.class
                .getResourceAsStream("/password-hash-migration.properties")) {
            assertNotNull("Reference password records must be available", input);
            FIXTURES.load(input);
        }
    }

    /** Isolates account lookup and records only successful persistence calls. */
    @Before
    public void setUp() throws Exception {
        user = new User();
        user.setUserId("hash-migration-user");
        user.setEmailAddress("hash-migration@example.com");
        user.setActive(true);
        user.setNew(false);
        user.setPasswordEncrypted(true);
        useFixture("legacy-current-cost");

        systemUser = new User();
        systemUser.setUserId("system");
        systemUser.setEmailAddress("system@example.com");
        userAPI = mock(UserAPI.class);
        final Company company = mock(Company.class);
        when(company.getAuthType()).thenReturn(authType);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadByUserByEmail(user.getEmailAddress(), systemUser, false))
                .thenReturn(user);
        when(userAPI.loadUserById(user.getUserId(), systemUser, false)).thenReturn(user);
        doAnswer(invocation -> {
            final User saved = invocation.getArgument(0);
            storedPassword.set(saved.getPassword());
            storedEncrypted = saved.getPasswordEncrypted();
            return null;
        }).when(userAPI).save(same(user), same(systemUser), eq(false));

        api = mockStatic(APILocator.class);
        api.when(APILocator::getUserAPI).thenReturn(userAPI);
        companies = mockStatic(PublicCompanyFactory.class);
        companies.when(PublicCompanyFactory::getDefaultCompany).thenReturn(company);
        props = mockStatic(PropsUtil.class, Mockito.CALLS_REAL_METHODS);
        props.when(() -> PropsUtil.get("auth.pipeline.pre")).thenReturn("");
        securityLogger = mockStatic(SecurityLogger.class);
    }

    /** Releases all thread-local static mocks after each parameterized scenario. */
    @After
    public void tearDown() {
        if (securityLogger != null) {
            securityLogger.close();
        }
        if (props != null) {
            props.close();
        }
        if (companies != null) {
            companies.close();
        }
        if (api != null) {
            api.close();
        }
    }

    /** Successful legacy login must save a current hash once, then stop rewriting it. */
    @Test
    public void successfulLegacyLoginPersists256BitHashOnce() throws Exception {
        final String legacy = storedPassword.get();

        assertTrue(LoginFactory.doLogin(loginIdentifier(), password()));
        assertNotEquals(legacy, storedPassword.get());
        assertCurrentStoredPassword();
        assertTrue(LoginFactory.doLogin(loginIdentifier(), password()));
        verify(userAPI, times(1)).save(same(user), same(systemUser), eq(false));
        verifyLoginSelector();
    }

    /** Old iteration costs must authenticate and migrate with the same password. */
    @Test
    public void successfulLowCostLegacyLoginPersistsCurrentHash() throws Exception {
        useFixture("legacy-low-cost");

        assertTrue(LoginFactory.doLogin(loginIdentifier(), password()));
        assertCurrentStoredPassword();
        verifyLoginSelector();
    }

    /** Failed logins may update attempt counters but must not replace the password. */
    @Test
    public void incorrectPasswordLeavesStoredHashUnchanged() throws Exception {
        for (final String key : Arrays.asList("legacy-current-cost", "current-current-cost")) {
            useFixture(key);
            user.setFailedLoginAttempts(0);
            final String original = storedPassword.get();

            assertFalse(LoginFactory.doLogin(loginIdentifier(), "incorrect"));
            assertEquals(original, storedPassword.get());
            assertEquals(original, user.getPassword());
            assertTrue(storedEncrypted);
            assertEquals(1, user.getFailedLoginAttempts());
        }
        verifyLoginSelector();
    }

    /** A current hash at the current cost must sign in without a persistence rewrite. */
    @Test
    public void currentHashLoginDoesNotRewritePassword() throws Exception {
        useFixture("current-current-cost");
        final String current = storedPassword.get();

        assertTrue(LoginFactory.doLogin(loginIdentifier(), password()));
        assertEquals(current, storedPassword.get());
        verify(userAPI, never()).save(same(user), same(systemUser), eq(false));
        verifyLoginSelector();
    }

    /** A save failure must not be reported as a successful persisted migration. */
    @Test
    public void rehashSaveFailureFailsAuthenticationWithoutPersistingNewHash() throws Exception {
        final String legacy = storedPassword.get();
        doThrow(new DotDataException("Test rehash persistence failure"))
                .when(userAPI).save(same(user), same(systemUser), eq(false));

        assertFalse(LoginFactory.doLogin(loginIdentifier(), password()));
        assertEquals(legacy, storedPassword.get());
    }

    /** Selects a frozen record as both the loaded and persisted account password. */
    private void useFixture(final String key) {
        user.setPassword(FIXTURES.getProperty(key));
        storedPassword.set(user.getPassword());
        storedEncrypted = user.getPasswordEncrypted();
    }

    /** Checks saved output length, hashed marker, password verification, and upgrade status. */
    private void assertCurrentStoredPassword() throws Exception {
        final String hash = storedPassword.get();

        assertEquals("2", hash.split(":")[1]);
        assertEquals(32, Base64.getDecoder().decode(hash.split(":")[4]).length);
        assertNotEquals(password(), hash);
        assertTrue(storedEncrypted);
        assertEquals(AUTHENTICATED, PasswordFactoryProxy.authPassword(password(), hash));
    }

    /** Ensures each run actually selected the configured identifier lookup path. */
    private void verifyLoginSelector() throws Exception {
        if (Company.AUTH_TYPE_EA.equals(authType)) {
            verify(userAPI, Mockito.atLeastOnce())
                    .loadByUserByEmail(user.getEmailAddress(), systemUser, false);
            verify(userAPI, never()).loadUserById(user.getUserId(), systemUser, false);
        } else {
            verify(userAPI, Mockito.atLeastOnce())
                    .loadUserById(user.getUserId(), systemUser, false);
            verify(userAPI, never())
                    .loadByUserByEmail(user.getEmailAddress(), systemUser, false);
        }
    }

    /** Returns the login identifier selected by this receiver's authentication mode. */
    private String loginIdentifier() {
        return Company.AUTH_TYPE_EA.equals(authType) ? user.getEmailAddress() : user.getUserId();
    }

    /** Returns the known test password associated with the frozen reference records. */
    private static String password() {
        return FIXTURES.getProperty("password");
    }
}
