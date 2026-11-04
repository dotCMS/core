package com.dotmarketing.cms.login.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
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
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
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
 */
public class LoginFactoryTest extends IntegrationTestBase {

    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";

    private static boolean originalSyncPassword;
    private static Role createdDirectoryRole;

    private final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        originalSyncPassword = BaseAuthenticator.SYNC_PASSWORD;
    }

    @After
    public void cleanup() {
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
