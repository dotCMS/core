package com.liferay.portal.ejb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.auth.Authenticator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link UserManagerImpl} logins whose account does not exist. Such a login still
 * answers {@link Authenticator#DNE}, and the submitted password goes through the same hash
 * verifier that checks a stored password, against a placeholder hash in the current format that
 * is prepared once.
 *
 * <p>The account lookup is mocked, so no database is needed; the password verifier runs for
 * real.</p>
 *
 * @author hassandotcms
 */
class UserManagerImplTest {

    private static final String COMPANY_ID = "test-company";
    private static final String PASSWORD = "Some-Password-123";

    private String previousDelayStrategy;

    /** Turns the failed-login delay off so a refused login returns at once. */
    @BeforeEach
    void disableDelay() {
        previousDelayStrategy = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, null);
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "TIME_MILLS:0");
    }

    /** Restores the delay setting so an assertion failure cannot leak configuration. */
    @AfterEach
    void restoreDelay() {
        Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, previousDelayStrategy);
    }

    /** A login for an account that does not exist checks the submitted password once. */
    @Test
    void loginForUnknownAccountChecksThePasswordOnce() throws Exception {
        try (MockedStatic<UserUtil> users = mockStatic(UserUtil.class);
                MockedStatic<PasswordFactoryProxy> passwords = mockStatic(PasswordFactoryProxy.class,
                        CALLS_REAL_METHODS)) {
            noAccountsByEmail(users);

            // When
            final int result = new UserManagerImpl().authenticateByEmailAddress(COMPANY_ID,
                    "no-account-1@example.com", PASSWORD);

            // Then
            assertEquals(Authenticator.DNE, result, "A login with no account still answers DNE");
            passwords.verify(() -> PasswordFactoryProxy.authPassword(eq(PASSWORD), anyString()), times(1));
        }
    }

    /** The placeholder is in the current hash format, so checking it costs what a stored password costs. */
    @Test
    void unknownAccountCheckUsesTheCurrentHashFormat() throws Exception {
        try (MockedStatic<UserUtil> users = mockStatic(UserUtil.class);
                MockedStatic<PasswordFactoryProxy> passwords = mockStatic(PasswordFactoryProxy.class,
                        CALLS_REAL_METHODS)) {
            noAccountsByEmail(users);

            // When
            new UserManagerImpl().authenticateByEmailAddress(COMPANY_ID, "no-account-2@example.com", PASSWORD);

            // Then
            final ArgumentCaptor<String> placeholder = ArgumentCaptor.forClass(String.class);
            passwords.verify(() -> PasswordFactoryProxy.authPassword(eq(PASSWORD), placeholder.capture()));
            assertFalse(PasswordFactoryProxy.isUnsecurePasswordHash(placeholder.getValue()),
                    "The placeholder is not in the legacy digest format");
            assertFalse(PasswordFactory.create().needsRehash(placeholder.getValue()),
                    "The placeholder uses the current algorithm and parameters");
        }
    }

    /** Every such login is checked against the same placeholder, prepared once rather than per login. */
    @Test
    void unknownAccountChecksReuseOnePlaceholder() throws Exception {
        try (MockedStatic<UserUtil> users = mockStatic(UserUtil.class);
                MockedStatic<PasswordFactoryProxy> passwords = mockStatic(PasswordFactoryProxy.class,
                        CALLS_REAL_METHODS)) {
            noAccountsByEmail(users);

            // When
            final UserManagerImpl userManager = new UserManagerImpl();
            userManager.authenticateByEmailAddress(COMPANY_ID, "no-account-3@example.com", PASSWORD);
            userManager.authenticateByEmailAddress(COMPANY_ID, "no-account-4@example.com", PASSWORD);

            // Then
            final ArgumentCaptor<String> placeholder = ArgumentCaptor.forClass(String.class);
            passwords.verify(() -> PasswordFactoryProxy.authPassword(eq(PASSWORD), placeholder.capture()),
                    times(2));
            final List<String> used = placeholder.getAllValues();
            assertEquals(used.get(0), used.get(1), "Both logins use the same placeholder");
        }
    }

    /** A login by user ID with no account checks the submitted password once, as a login by email does. */
    @Test
    void loginByUserIdForUnknownAccountChecksThePasswordOnce() throws Exception {
        try (MockedStatic<UserUtil> users = mockStatic(UserUtil.class);
                MockedStatic<PasswordFactoryProxy> passwords = mockStatic(PasswordFactoryProxy.class,
                        CALLS_REAL_METHODS)) {
            users.when(() -> UserUtil.findByC_U(anyString(), anyString())).thenThrow(new NoSuchUserException());

            // When
            final int result = new UserManagerImpl().authenticateByUserId(COMPANY_ID, "no-account-id", PASSWORD);

            // Then
            assertEquals(Authenticator.DNE, result, "A login with no account still answers DNE");
            passwords.verify(() -> PasswordFactoryProxy.authPassword(eq(PASSWORD), anyString()), times(1));
        }
    }

    /** Makes every account lookup by email address miss. */
    private static void noAccountsByEmail(final MockedStatic<UserUtil> users) {
        users.when(() -> UserUtil.findByC_EA(anyString(), anyString())).thenThrow(new NoSuchUserException());
    }
}
