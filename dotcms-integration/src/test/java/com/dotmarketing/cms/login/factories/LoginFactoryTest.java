package com.dotmarketing.cms.login.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.BaseAuthenticator;
import com.dotcms.enterprise.LDAPImpl;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
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
import com.liferay.portal.util.PropsUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link LoginFactory#passwordMatch(String, User)} and for how a directory
 * login updates the local password ({@link LoginFactory#resetDirectoryPassword(User, int)}).
 *
 * <p>Each test writes {@code user_.password_} directly so it controls exactly which stored
 * format the comparison sees, then reloads the user through {@code UserAPI} the same way the
 * login flows do.</p>
 *
 * <p>The last tests drive {@link LoginFactory#doLogin(String, String)} and
 * {@link LoginFactory#doLogin(String, String, boolean, HttpServletRequest, HttpServletResponse)}
 * with a pre-authenticator that answers {@link Authenticator#SUCCESS} without checking anything,
 * which is what the bundled directory authenticator does for an account it did not find in the
 * directory.</p>
 */
public class LoginFactoryTest extends IntegrationTestBase {

    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";
    private static final String DIRECTORY_IMPLEMENTATION = "LDAP_FRONTEND_AUTH_IMPLEMENTATION";

    private static boolean originalSyncPassword;
    private static Role createdDirectoryRole;

    private final List<User> createdUsers = new ArrayList<>();

    private String originalPreAuthenticator;
    private String originalDirectoryImplementation;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        originalSyncPassword = BaseAuthenticator.SYNC_PASSWORD;
    }

    @Before
    public void saveAuthenticationSettings() {
        originalPreAuthenticator = Config.getStringProperty(PropsUtil.AUTH_PIPELINE_PRE, null);
        originalDirectoryImplementation = Config.getStringProperty(DIRECTORY_IMPLEMENTATION, null);
    }

    @After
    public void cleanup() {
        Config.setProperty(PropsUtil.AUTH_PIPELINE_PRE, originalPreAuthenticator);
        Config.setProperty(DIRECTORY_IMPLEMENTATION, originalDirectoryImplementation);
        BaseAuthenticator.SYNC_PASSWORD = originalSyncPassword;
        createdUsers.forEach(UserDataGen::remove);
        createdUsers.clear();
    }

    @AfterClass
    public static void removeDirectoryRole() {
        if (createdDirectoryRole != null) {
            RoleDataGen.remove(createdDirectoryRole);
        }
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

    /**
     * Given: a directory user, with directory passwords not copied into dotCMS.
     * When:  the directory rejects the login.
     * Then:  the stored password is left untouched.
     */
    @Test
    public void test_resetDirectoryPassword_directoryRejected_keepsStoredPassword() throws Exception {
        BaseAuthenticator.SYNC_PASSWORD = false;
        final String stored = PasswordFactoryProxy.generateHash(PASSWORD);
        final User user = directoryUserWithStoredPassword(stored);

        LoginFactory.resetDirectoryPassword(user, Authenticator.FAILURE);

        assertEquals(stored, loadStoredPassword(user));
    }

    /**
     * Given: a directory user, with directory passwords not copied into dotCMS.
     * When:  the directory accepts the login.
     * Then:  the stored password is replaced with a new secure hash, and the old password no
     *        longer matches it.
     */
    @Test
    public void test_resetDirectoryPassword_directoryAccepted_storesNewSecureHash() throws Exception {
        BaseAuthenticator.SYNC_PASSWORD = false;
        final String stored = PasswordFactoryProxy.generateHash(PASSWORD);
        final User user = directoryUserWithStoredPassword(stored);

        LoginFactory.resetDirectoryPassword(user, Authenticator.SUCCESS);

        final String updated = loadStoredPassword(user);
        assertNotEquals(stored, updated);
        assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash(updated));
        assertEquals(AuthenticationStatus.NOT_AUTHENTICATED,
                PasswordFactoryProxy.authPassword(PASSWORD, updated));
    }

    /**
     * Given: a user who does not belong to the directory.
     * When:  the login is accepted.
     * Then:  the stored password is left untouched.
     */
    @Test
    public void test_resetDirectoryPassword_localUser_keepsStoredPassword() throws Exception {
        BaseAuthenticator.SYNC_PASSWORD = false;
        final String stored = PasswordFactoryProxy.generateHash(PASSWORD);
        final User user = userWithStoredPassword(stored);

        LoginFactory.resetDirectoryPassword(user, Authenticator.SUCCESS);

        assertEquals(stored, loadStoredPassword(user));
    }

    /**
     * Given: a pre-authenticator that answers SUCCESS for anyone, which is what the bundled
     *        directory authenticator does for an account the directory does not hold.
     * When:  the credentials-only login is tried with a password that is not the stored one.
     * Then:  the login is refused: the pre-authenticator's answer is not a password check.
     */
    @Test
    public void test_doLogin_preAuthenticatorSuccess_refusesAnotherPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);

        assertFalse(LoginFactory.doLogin(user.getEmailAddress(), PASSWORD + "-wrong"));
    }

    /**
     * Given: the same pre-authenticator that answers SUCCESS for anyone.
     * When:  the credentials-only login is tried with the stored password.
     * Then:  the login is accepted, so requiring the stored password does not shut out a user
     *        the directory did authenticate, whose password it stores in dotCMS first.
     */
    @Test
    public void test_doLogin_preAuthenticatorSuccess_acceptsTheStoredPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);

        assertTrue(LoginFactory.doLogin(user.getEmailAddress(), PASSWORD));
    }

    /**
     * Given: the same pre-authenticator, and an account that was deactivated.
     * When:  the login is tried with the stored password.
     * Then:  the login is refused, the way the login against dotCMS and the REST login already
     *        refuse a deactivated account.
     */
    @Test
    public void test_doLogin_preAuthenticatorSuccess_refusesDeactivatedAccount() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        deactivate(user);
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);

        assertFalse(LoginFactory.doLogin(user.getEmailAddress(), PASSWORD));
    }

    /**
     * Given: the same pre-authenticator.
     * When:  the login that establishes a session is tried with a password that is not the
     *        stored one, and then with the stored one.
     * Then:  only the stored password signs the user in.
     *
     * <p>The account also holds the front-end role, because a login that establishes a session
     * refuses an account that is neither a front-end nor a back-end user, whatever its password
     * is.</p>
     */
    @Test
    public void test_doLoginWithSession_preAuthenticatorSuccess_requiresTheStoredPassword() throws Exception {
        final User user = userWithStoredPassword(PasswordFactoryProxy.generateHash(PASSWORD));
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadFrontEndUserRole(), user);
        usePreAuthenticator(AcceptsAnyoneAuthenticator.class);
        final HttpServletRequest request = requestWithSession();
        final HttpServletResponse response = mock(HttpServletResponse.class);

        assertFalse(LoginFactory.doLogin(user.getEmailAddress(), PASSWORD + "-wrong", false, request,
                response));
        assertTrue(LoginFactory.doLogin(user.getEmailAddress(), PASSWORD, false, request, response));
    }

    /**
     * Points {@code auth.pipeline.pre} and the directory implementation at the given
     * authenticator, which is how a login reaches the directory branch.
     *
     * @param authenticator the {@link Authenticator} the login should consult
     */
    private static void usePreAuthenticator(final Class<? extends Authenticator> authenticator) {
        Config.setProperty(PropsUtil.AUTH_PIPELINE_PRE, authenticator.getName());
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
     * A pre-authenticator that answers {@link Authenticator#SUCCESS} without checking anything,
     * standing for the branch of the bundled directory authenticator that answers for an account
     * the directory does not hold.
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
     * Creates a user with the given stored password and gives it the directory role.
     *
     * @param storedPassword the value to place in {@code user_.password_}
     * @return the reloaded {@link User}
     */
    private User directoryUserWithStoredPassword(final String storedPassword) throws Exception {
        final User user = userWithStoredPassword(storedPassword);
        APILocator.getRoleAPI().addRoleToUser(directoryRole(), user);
        return user;
    }

    /**
     * Returns the role that marks directory users, creating it when the test database does not
     * have it yet. A role created here is removed after the class runs.
     *
     * @return the directory {@link Role}
     */
    private static Role directoryRole() throws Exception {
        final Role existing = APILocator.getRoleAPI().loadRoleByKey(LDAPImpl.LDAP_USER_ROLE);
        if (existing != null) {
            return existing;
        }
        createdDirectoryRole = new RoleDataGen()
                .key(LDAPImpl.LDAP_USER_ROLE)
                .name(LDAPImpl.LDAP_USER_ROLE)
                .nextPersisted();
        return createdDirectoryRole;
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
