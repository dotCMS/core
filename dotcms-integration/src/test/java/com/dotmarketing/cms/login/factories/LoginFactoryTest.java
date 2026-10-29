package com.dotmarketing.cms.login.factories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link LoginFactory#passwordMatch(String, User)}.
 *
 * <p>Each test writes {@code user_.password_} directly so it controls exactly which stored
 * format the comparison sees, then reloads the user through {@code UserAPI} the same way the
 * login flows do.</p>
 */
public class LoginFactoryTest extends IntegrationTestBase {

    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";

    private final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void cleanup() {
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
