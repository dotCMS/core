package com.dotmarketing.cms.login.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for native login and {@link LoginFactory#passwordMatch(String, User)}.
 *
 * <p>Each test writes {@code user_.password_} directly so it controls exactly which stored
 * format the comparison sees, then reloads the user through {@code UserAPI} the same way the
 * login flows do.</p>
 *
 * <p>The last tests drive {@link LoginFactory#doLogin(String, String)} and
 * {@link LoginFactory#doLogin(String, String, boolean, HttpServletRequest, HttpServletResponse)}
 * with stale legacy hooks configured to an authenticator that answers
 * {@link Authenticator#SUCCESS} without checking anything. Native login must ignore those hooks
 * and still require the local password and an active account.</p>
 */
public class LoginFactoryTest extends IntegrationTestBase {

    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";
    private static final String PRE_AUTHENTICATOR_KEY = "auth.pipeline.pre";
    private static final String DIRECTORY_IMPLEMENTATION = "LDAP_FRONTEND_AUTH_IMPLEMENTATION";

    private final List<User> createdUsers = new ArrayList<>();

    private String originalPreAuthenticator;
    private String originalDirectoryImplementation;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void saveAuthenticationSettings() {
        originalPreAuthenticator = Config.getStringProperty(PRE_AUTHENTICATOR_KEY, null);
        originalDirectoryImplementation = Config.getStringProperty(DIRECTORY_IMPLEMENTATION, null);
    }

    @After
    public void cleanup() {
        Config.setProperty(PRE_AUTHENTICATOR_KEY, originalPreAuthenticator);
        Config.setProperty(DIRECTORY_IMPLEMENTATION, originalDirectoryImplementation);
        createdUsers.forEach(UserDataGen::remove);
        createdUsers.clear();
    }

    /**
     * Given: a stored value in the older digest format.
     * When:  the user types the password the digest was made from.
     * Then:  the password matches, and the stored value is upgraded to a secure hash.
     */
    @Test
    public void test_passwordMatch_digestFormat_acceptsOriginalPassword() throws Exception {
        final User user = userWithStoredPassword(PublicEncryptionFactory.digestString(PASSWORD));

        assertTrue(LoginFactory.passwordMatch(PASSWORD, user));
        assertEquals(AuthenticationStatus.AUTHENTICATED,
                PasswordFactoryProxy.authPassword(PASSWORD, loadStoredPassword(user)));
    }

    /**
     * Given: a stored value in the older digest format.
     * When:  the typed text is the stored value itself.
     * Then:  the password does not match, because only the digest of the typed text is
     *        compared, and the stored value is left untouched.
     */
    @Test
    public void test_passwordMatch_digestFormat_comparedOnlyThroughDigest() throws Exception {
        final String stored = PublicEncryptionFactory.digestString(PASSWORD);
        final User user = userWithStoredPassword(stored);

        assertFalse(LoginFactory.passwordMatch(stored, user));
        assertEquals(stored, loadStoredPassword(user));
    }

    /**
     * Given: a stored value that is plain text rather than a hash or digest.
     * When:  the typed text is that same value.
     * Then:  the password does not match, because the typed text is always hashed before it is
     *        compared, and the stored value is left untouched.
     */
    @Test
    public void test_passwordMatch_plainTextValue_neverComparedDirectly() throws Exception {
        final User user = userWithStoredPassword(PASSWORD);

        assertFalse(LoginFactory.passwordMatch(PASSWORD, user));
        assertEquals(PASSWORD, loadStoredPassword(user));
    }

    /**
     * Given: an account whose stored password is the reserved directory-account value, either
     *        as written by older versions or after it was hashed.
     * When:  that reserved value is typed as the password.
     * Then:  the password never matches.
     */
    @Test
    public void test_passwordMatch_reservedDirectoryValue_neverMatches() throws Exception {
        final String reserved = DotCustomLoginPostAction.FAKE_PASSWORD;

        final User plainUser = userWithStoredPassword(reserved);
        assertFalse(LoginFactory.passwordMatch(reserved, plainUser));

        final User hashedUser = userWithStoredPassword(PasswordFactoryProxy.generateHash(reserved));
        assertFalse(LoginFactory.passwordMatch(reserved, hashedUser));
    }

    /**
     * Given: a password stored as a secure hash.
     * When:  the user types the right password, and then a wrong one.
     * Then:  only the right password matches.
     */
    @Test
    public void test_passwordMatch_secureHash_unchanged() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));

        assertTrue(LoginFactory.passwordMatch(PASSWORD, user));
        assertFalse(LoginFactory.passwordMatch(PASSWORD + "-wrong", user));
    }

    /**
     * Given: a user with no stored password, either null or empty.
     * When:  a password, or an empty one, is typed.
     * Then:  the password does not match, and the check fails cleanly instead of throwing.
     */
    @Test
    public void test_passwordMatch_missingStoredPassword_neverMatches() throws Exception {
        final User nullUser = userWithStoredPassword(null);
        assertFalse(LoginFactory.passwordMatch(PASSWORD, nullUser));
        assertFalse(LoginFactory.passwordMatch("", nullUser));

        final User emptyUser = userWithStoredPassword("");
        assertFalse(LoginFactory.passwordMatch(PASSWORD, emptyUser));
        assertFalse(LoginFactory.passwordMatch("", emptyUser));
    }

    /**
     * Given: a password stored as a secure hash.
     * When:  the stored hash itself is typed as the password.
     * Then:  it does not match, while the real password still does.
     */
    @Test
    public void test_passwordMatch_secureHash_storedValueIsNotAPassword() throws Exception {
        final String stored = PasswordFactoryProxy.generateHash(PASSWORD);
        final User user = userWithStoredPassword(stored);

        assertFalse(LoginFactory.passwordMatch(stored, user));
        assertTrue(LoginFactory.passwordMatch(PASSWORD, user));
    }

    @Test
    public void test_doLogin_nativePassword_acceptsCorrectPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));

        assertTrue(LoginFactory.doLogin(loginName(user), PASSWORD));
    }

    @Test
    public void test_doLogin_nativePassword_rejectsWrongPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));

        assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD + "-wrong"));
    }

    /** Both native login paths must reject an inactive account before checking its password. */
    @Test
    public void test_doLogin_deactivatedAccount_isRefused() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        deactivate(user);

        assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD));
        assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD, false, requestWithSession(),
                mock(HttpServletResponse.class)));
    }

    /** Returns the identifier accepted by the configured native login mode. */
    private String loginName(final User user) {
        return com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getAuthType()
                .equals(com.liferay.portal.model.Company.AUTH_TYPE_EA)
                ? user.getEmailAddress() : user.getUserId();
    }

    /**
     * Given: stale legacy hooks configured to an authenticator that accepts any password.
     * When:  the credentials-only login is tried with a password that is not the stored one.
     * Then:  native login ignores the hooks and refuses the login.
     */
    @Test
    public void test_doLogin_staleLegacyHooks_refusesAnotherPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);

        assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD + "-wrong"));
    }

    /**
     * Given: the same stale legacy hooks.
     * When:  the credentials-only login is tried with the stored password.
     * Then:  native login accepts the password despite the obsolete configuration.
     */
    @Test
    public void test_doLogin_staleLegacyHooks_acceptsTheStoredPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);

        assertTrue(LoginFactory.doLogin(loginName(user), PASSWORD));
    }

    /**
     * Given: the same stale legacy hooks, and an account that was deactivated.
     * When:  the login is tried with the stored password.
     * Then:  native login refuses the inactive account.
     */
    @Test
    public void test_doLogin_staleLegacyHooks_refusesDeactivatedAccount() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        deactivate(user);
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);

        assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD));
    }

    /**
     * Given: the same stale legacy hooks.
     * When:  the login that establishes a session is tried with a password that is not the
     *        stored one, and then with the stored one.
     * Then:  only the stored password signs the user in.
     *
     * <p>The account also holds the front-end role, because a login that establishes a session
     * refuses an account that is neither a front-end nor a back-end user, whatever its password
     * is.</p>
     */
    @Test
    public void test_doLoginWithSession_staleLegacyHooks_requiresTheStoredPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadFrontEndUserRole(), user);
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);
        final HttpServletRequest request = requestWithSession();
        final HttpServletResponse response = mock(HttpServletResponse.class);

        assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD + "-wrong", false, request,
                response));
        assertTrue(LoginFactory.doLogin(loginName(user), PASSWORD, false, request, response));
    }

    /**
     * Given: stale legacy hooks and an inactive account with the front-end role.
     * When:  the session login is tried with the correct local password.
     * Then:  native login refuses the inactive account despite the configured hooks.
     */
    @Test
    public void test_doLoginWithSession_staleLegacyHooks_refusesDeactivatedAccount() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadFrontEndUserRole(), user);
        deactivate(user);
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);

        assertFalse(LoginFactory.doLogin(loginName(user), PASSWORD, false, requestWithSession(),
                mock(HttpServletResponse.class)));
    }

    /**
     * Configures obsolete authentication hooks that native login must ignore.
     *
     * @param authenticator the {@link Authenticator} named by the stale configuration
     */
    private static void usePreAuthenticator(final Class<? extends Authenticator> authenticator) {
        Config.setProperty(PRE_AUTHENTICATOR_KEY, authenticator.getName());
        Config.setProperty(DIRECTORY_IMPLEMENTATION, authenticator.getName());
    }

    /**
     * Deactivates an account the way an administrator would, writing the column directly so the
     * stored password is left exactly as the test wrote it.
     *
     * @param user the {@link User} to deactivate
     */
    private void deactivate(final User user) throws Exception {
        new DotConnect()
                .setSQL("update user_ set active_ = ? where userId = ?")
                .addParam(false)
                .addParam(user.getUserId())
                .loadResult();
        CacheLocator.getUserCache().remove(user.getUserId());
    }

    /**
     * @return a request carrying a session that behaves like a real one, because a successful
     * login replaces it to prevent session fixation, copying its attributes across
     */
    private static HttpServletRequest requestWithSession() {
        return new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
    }

    /**
     * A synthetic legacy hook that accepts anyone. Configuring it must not bypass native
     * password validation or inactive-account checks.
     */
    public static class AcceptsAnyoneAuthenticator implements Authenticator {

        @Override
        public int authenticateByEmailAddress(final String companyId, final String emailAddress,
                final String password) throws AuthException {
            return SUCCESS;
        }

        @Override
        public int authenticateByUserId(final String companyId, final String userId,
                final String password) throws AuthException {
            return SUCCESS;
        }
    }


    /**
     * Creates a user, overwrites its stored password directly in the database, and reloads it
     * so the test sees exactly what a login would see.
     *
     * @param storedPassword the value to place in {@code user_.password_}
     * @return the reloaded {@link User}
     */
    private User userWithStoredPassword(final String storedPassword) throws Exception {
        final User user = new UserDataGen()
                .emailAddress("login-factory-" + UUIDGenerator.generateUuid() + "@dotcms.test")
                .nextPersisted();
        createdUsers.add(user);

        new DotConnect()
                .setSQL("update user_ set password_ = ? where userId = ?")
                .addParam(storedPassword)
                .addParam(user.getUserId())
                .loadResult();
        CacheLocator.getUserCache().remove(user.getUserId());

        return APILocator.getUserAPI().loadUserById(user.getUserId());
    }

    /**
     * Reads a user's stored password straight from the database.
     *
     * @param user the {@link User} to read
     * @return the {@code password_} column
     */
    private String loadStoredPassword(final User user) throws Exception {
        return new DotConnect()
                .setSQL("select password_ from user_ where userId = ?")
                .addParam(user.getUserId())
                .getString("password_");
    }
}
