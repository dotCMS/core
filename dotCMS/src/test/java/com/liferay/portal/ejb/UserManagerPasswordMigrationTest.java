package com.liferay.portal.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.dotmarketing.util.Config;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.Encryptor;
import com.liferay.util.InstancePool;
import com.liferay.util.KeyValuePair;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

/** Ensures verified passwords remain usable by the legacy encrypted-user-ID contract. */
public class UserManagerPasswordMigrationTest {

    private static final Properties FIXTURES = new Properties();

    /** Loads independent records without changing production profile configuration. */
    @BeforeClass
    public static void prepare() throws Exception {
        Config.initializeConfig();
        try (InputStream input = UserManagerPasswordMigrationTest.class
                .getResourceAsStream("/password-hash-migration.properties")) {
            assertNotNull("Reference password records must be available", input);
            FIXTURES.load(input);
        }
    }

    /** Correct passwords remain authenticated for both profiles and outdated iteration costs. */
    @Test
    public void correctLegacyAndCurrentPasswordsReturnCredentials() throws Exception {
        for (final String key : Arrays.asList("legacy-current-cost", "current-current-cost",
                "legacy-low-cost", "current-low-cost")) {
            checkCredentials(key, true);
        }
    }

    /** A request for rehash must never turn an incorrect password into successful login. */
    @Test
    public void incorrectLegacyAndCurrentPasswordsAreRejected() throws Exception {
        for (final String key : Arrays.asList("legacy-current-cost", "current-current-cost",
                "legacy-low-cost", "current-low-cost")) {
            checkCredentials(key, false);
        }
    }

    /** Stubs only ID/account lookup while using real password verification. */
    private static void checkCredentials(final String fixture, final boolean correct)
            throws Exception {
        final String userId = "hash-migration-user";
        final String companyId = "hash-migration-company";
        final String encodedUserId = "encoded-user-id";
        final String password = correct ? FIXTURES.getProperty("password") : "incorrect";
        final Company company = mock(Company.class);
        final User user = mock(User.class);
        final PrincipalFinder principal = mock(PrincipalFinder.class);
        when(user.getPassword()).thenReturn(FIXTURES.getProperty(fixture));
        when(principal.toLiferay(userId)).thenReturn(userId);

        try (MockedStatic<CompanyUtil> companies = mockStatic(CompanyUtil.class);
                MockedStatic<UserUtil> users = mockStatic(UserUtil.class);
                MockedStatic<Encryptor> encryptor = mockStatic(Encryptor.class);
                MockedStatic<InstancePool> instances = mockStatic(InstancePool.class)) {
            companies.when(() -> CompanyUtil.findByPrimaryKey(companyId)).thenReturn(company);
            users.when(() -> UserUtil.findByPrimaryKey(userId)).thenReturn(user);
            encryptor.when(() -> Encryptor.decrypt(any(), eq(encodedUserId)))
                    .thenReturn(userId);
            instances.when(() -> InstancePool.get(any())).thenReturn(principal);

            final UserManagerImpl manager = new UserManagerImpl();
            if (correct) {
                final KeyValuePair result = manager.decryptUserId(companyId, encodedUserId,
                        password);
                assertEquals(userId, result.getKey());
                assertEquals(password, result.getValue());
            } else {
                assertThrows(PrincipalException.class,
                        () -> manager.decryptUserId(companyId, encodedUserId, password));
            }
            users.verify(() -> UserUtil.update(user), never());
        }
    }
}
